package me.lightless.burp

import burp.IBurpExtenderCallbacks
import burp.IHttpListener
import burp.IHttpRequestResponse
import me.lightless.burp.executor.JsRuleExecutor
import me.lightless.burp.executor.SimpleRuleExecutor
import me.lightless.burp.items.EditParameter
import me.lightless.burp.items.SignRuleItem
import me.lightless.burp.models.SignRuleDAO
import me.lightless.burp.models.SignRuleModel
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import javax.script.SimpleBindings

class HTTPListener(private val callbacks: IBurpExtenderCallbacks) : IHttpListener {

    // 提前初始化规则引擎
    private val jsExecutor = JsRuleExecutor()
    private val simpleExecutor = SimpleRuleExecutor()

    override fun processHttpMessage(toolFlag: Int, messageIsRequest: Boolean, messageInfo: IHttpRequestResponse?) {

        // 跳过 response 和空消息
        if (!messageIsRequest || messageInfo == null) {
            return
        }

        // 执行签名
        doSign(messageInfo, toolFlag)
    }

    /**
     * 针对消息进行签名
     */
    private fun doSign(messageInfo: IHttpRequestResponse, toolFlag: Int) {
        // 获取当前的 URL
        val request = callbacks.helpers.analyzeRequest(messageInfo)
        val url = request.url.toString()
        Logger.debug("URL=$url, ContentType=${request.contentType.toUInt()} request.parameters = ${
            request.parameters.map {
                "[${
                    ParameterType.fromValue(
                        it.type.toInt()
                    )
                }]${it.name}=${it.value}"
            }
        }")

        // 检查当前的 URL 和 TOOLS 是否有匹配签名规则
        // TODO 暂时先每次请求查询数据库，后续再改成缓存模式
        // 如果有多个符合的签名规则，则直接返回，目前只允许同时处理一个规则
        val signRuleItem = transaction {
            val rules = SignRuleDAO.find { SignRuleModel.isDeleted eq false and (SignRuleModel.status eq true) }
                .filter { Regex(it.filter).containsMatchIn(url) && it.toolFlag and toolFlag != 0 }
            if (rules.isEmpty()) {
                Logger.debug("$url matches nothing, skip.")
                return@transaction null
            } else if (rules.size != 1) {
                Logger.warn("Must only one sign rule be matched. Matched sign rule count: ${rules.size}")
                return@transaction null
            } else {
                val t = rules.first()
                return@transaction SignRuleItem(
                    t.ruleName, t.filter, t.content, t.status, t.ruleType, t.toolFlag, t.createdTime, t.updatedTime
                )
            }
        } ?: return

        // 从请求中构建 binding 数据，无论是专家模式还是简单模式，都需要这个数据进行计算
        val bindings = this.buildBinding(messageInfo)

        // 调用对应的执行器
        val editParameterList = when (signRuleItem.ruleType) {
            1 -> {
                // JS 规则
                Logger.debug("Rule ${signRuleItem.ruleName} use JSExecutor")
                this.jsExecutor.execute(bindings, signRuleItem)
            }

            2 -> {
                // simple 规则
                Logger.debug("Rule ${signRuleItem.ruleName} use SimpleExecutor")
                this.simpleExecutor.execute(bindings, signRuleItem)
            }

            else -> {
                Logger.debug("Rule ${signRuleItem.ruleName} ruleType unknown. skip it.")
                return
            }
        }

        // 对 HTTP 的消息进行修改
        editHTTPMessage(editParameterList, messageInfo)
    }

    private fun buildBinding(messageInfo: IHttpRequestResponse): SimpleBindings {
        val bindings = SimpleBindings()
        val rawRequest = messageInfo.request
        val analyzedRequest = callbacks.helpers.analyzeRequest(rawRequest)

        val bindingRequest = mutableMapOf<String, Any>()
        // query 除了需要存 参数名 和 参数值 外，还需要存储参数位置等信息，所以这里需要用 List<Map<K, V>>
        val bindingQuery = mutableListOf<Map<String, Any>>()
        val bindingHeaders = mutableMapOf<String, String>()
        val bindingCookies = mutableMapOf<String, String>()

        bindings["request"] = bindingRequest
        bindingRequest["query"] = bindingQuery
        bindingRequest["headers"] = bindingHeaders
        bindingRequest["cookies"] = bindingCookies

        // 绑定请求参数
        analyzedRequest.parameters.forEach {
            // TODO 把参数从 map 换成一个 data class
            bindingQuery.add(mapOf("name" to it.name, "value" to it.value, "location" to it.type.toInt()))

            // 如果参数类型是 cookie 类型，同时存一份到 bindCookies 中
            if (it.type == ParameterType.PARAM_COOKIE.value.toByte()) {
                bindingCookies[it.name] = it.value
            }
        }

        // 绑定请求头
        analyzedRequest.headers.forEach {
            // 这里需要在第一个冒号的位置进行切分
            val idx = it.indexOf(":")
            if (idx != -1) {
                // 正常的 key: value 格式
                val name = it.substring(0, idx).trim()
                val value = it.substring(idx + 1, it.length).trim()
                bindingHeaders[name] = value

                // 同时添加一份到 query 中
                // TODO 把参数从 map 换成一个 data class
                bindingQuery.add(mapOf("name" to name, "value" to value, "location" to ParameterType.HEADER.value))
            } else {
                // HTTP 的起始行，需要绑定一些基础信息，该行格式： METHOD URI HTTP_VERSION
                it.split(" ").let { info ->
                    bindingRequest["HTTPMethod"] = info[0]
                    bindingRequest["URI"] = info[1]
                    bindingRequest["HTTPVersion"] = info[2]
                }

            }
        }

        // 绑定一些其他的数据
        bindingRequest["method"] = analyzedRequest.method.uppercase()
        // 这个 ContentType 获取到的是个 Byte 类型，是 burp 自己解析出来的，并非 HTTP 包中原始的 ContentType
        bindingRequest["burpContentType"] = analyzedRequest.contentType
        bindingRequest["host"] = messageInfo.httpService.host
        bindingRequest["port"] = messageInfo.httpService.port
        bindingRequest["protocol"] = messageInfo.httpService.protocol
        bindingRequest["url"] =
            "${messageInfo.httpService.protocol}://${messageInfo.httpService.host}${bindingRequest["uri"]}"

        return bindings
    }

    /**
     * 修改 HTTP 消息
     */
    private fun editHTTPMessage(
        editParameters: List<EditParameter>, messageInfo: IHttpRequestResponse
    ) {
        var analyzedRequest = callbacks.helpers.analyzeRequest(messageInfo.request)

        // 1. 先修改参数部分，跳过 Location 是 header 的
        for (editParameter in editParameters.filter { it.location != ParameterType.HEADER.value }) {
            Logger.debug("edit parameter: $editParameter, Location: ${ParameterType.fromValue(editParameter.location)}")
            when (editParameter.location) {
                ParameterType.PARAM_URL.value, ParameterType.PARAM_BODY.value, ParameterType.PARAM_COOKIE.value -> {
                    // 修改参数
                    val newParameter = callbacks.helpers.buildParameter(
                        editParameter.name, editParameter.value, editParameter.location.toByte()
                    )
                    val existedParam =
                        analyzedRequest.parameters.find { it.name == editParameter.name && it.type == editParameter.location.toByte() }
                    when (editParameter.action) {
                        // 如果参数存在则删除参数
                        EditAction.DELETE.value -> {
                            if (existedParam != null) {
                                messageInfo.request = callbacks.helpers.removeParameter(messageInfo.request, newParameter)
                            }
                        }
                        // 如果参数不存在，则新增参数，如果参数存在则什么都不做
                        EditAction.ADD.value -> {
                            if (existedParam == null) {
                                messageInfo.request = callbacks.helpers.addParameter(messageInfo.request, newParameter)
                            }
                        }
                        // 如果参数存在，则更新参数值，如果不存在，则什么都不干
                        EditAction.UPDATE.value -> {
                            if (existedParam != null) {
                                messageInfo.request = callbacks.helpers.updateParameter(messageInfo.request, newParameter)
                            }
                        }
                        // 如果参数存在，则更新参数值，如果参数不存在，则新增该参数
                        EditAction.OVERRIDE.value -> {
                            if (existedParam == null) {
                                messageInfo.request = callbacks.helpers.addParameter(messageInfo.request, newParameter)
                            } else {
                                messageInfo.request = callbacks.helpers.updateParameter(messageInfo.request, newParameter)
                            }
                        }
                    }
                    Logger.debug("Parameter $editParameter process finished.")
                }

                else -> {
                    Logger.warn("Edit parameter on ${ParameterType.fromValue(editParameter.location)} not support yet.")
                }
            }

            // TODO 重新分析一下，
            analyzedRequest = callbacks.helpers.analyzeRequest(messageInfo.request)
        }

        // 2. 修改 header 部分
        val headers = analyzedRequest.headers
        for (editParameter in editParameters.filter { it.location == ParameterType.HEADER.value }) {
            Logger.debug("edit parameter: $editParameter, Location: ${ParameterType.fromValue(editParameter.location)}")
            val existHeader = headers.find { it.lowercase().startsWith("${editParameter.name}: ".lowercase()) }
            when (editParameter.action) {
                EditAction.ADD.value -> {
                    if (existHeader == null) {
                        headers.add("${editParameter.name}: ${editParameter.value}")
                    }
                }

                EditAction.UPDATE.value -> {
                    if (existHeader != null) {
                        headers.remove(existHeader)
                        headers.add("${editParameter.name}: ${editParameter.value}")
                    }
                }

                EditAction.OVERRIDE.value -> {
                    if (existHeader == null) {
                        headers.add("${editParameter.name}: ${editParameter.value}")
                    } else {
                        headers.remove(existHeader)
                        headers.add("${editParameter.name}: ${editParameter.value}")
                    }
                }

                EditAction.DELETE.value -> {
                    if (existHeader != null) {
                        headers.remove(existHeader)
                    }
                }
            }

            analyzedRequest = callbacks.helpers.analyzeRequest(messageInfo.request)
            Logger.debug("Edit header ${editParameter.name} finished.")
        }

        // 全都改完了，重新构建 HTTP 消息
        val rawRequestBody = messageInfo.request.copyOfRange(analyzedRequest.bodyOffset, messageInfo.request.size)
        messageInfo.request = callbacks.helpers.buildHttpMessage(headers, rawRequestBody)
        Logger.debug("Final request: \n${callbacks.helpers.bytesToString(messageInfo.request)}\nBuild HTTP finished.")
    }
}