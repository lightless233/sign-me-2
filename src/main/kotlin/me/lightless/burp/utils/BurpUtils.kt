package me.lightless.burp.utils

import burp.IBurpExtenderCallbacks
import burp.IHttpRequestResponse
import burp.IHttpService
import me.lightless.burp.EditAction
import me.lightless.burp.Logger
import me.lightless.burp.ParameterType
import me.lightless.burp.items.EditParameter
import javax.script.SimpleBindings

object BurpUtils {
    lateinit var burpCallbacks: IBurpExtenderCallbacks

    @JvmStatic
    fun buildBinding(rawRequest: ByteArray, inputHttpService: IHttpService?): SimpleBindings {
        val httpService = inputHttpService ?: burpCallbacks.helpers.buildHttpService("DUMMY_HOST", 443, "https")

        val analyzedRequest = burpCallbacks.helpers.analyzeRequest(rawRequest)
        val bindings = SimpleBindings()

        val bindingRequest = mutableMapOf<String, Any>()
        // query 除了需要存 参数名 和 参数值 外，还需要存储参数位置等信息，所以这里需要用 List<Map<K, V>>
        val bindingQuery = mutableListOf<Map<String, Any>>()
        val bindingHeaders = mutableMapOf<String, String>()
        val bindingCookies = mutableMapOf<String, String>()

        bindings["request"] = bindingRequest
        bindingRequest["rawRequest"] = rawRequest.toString(Charsets.UTF_8)
        bindingRequest["body"] =
            rawRequest.copyOfRange(analyzedRequest.bodyOffset, rawRequest.size).toString(Charsets.UTF_8)
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
        var tempHost = "DUMMY_HOST"
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

                // 如果是 Host 头，临时存一份
                if (name.equals("host", true) && inputHttpService == null) {
                    tempHost = value
                }
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
        bindingRequest["host"] = inputHttpService?.host ?: tempHost
        bindingRequest["port"] = httpService.port
        bindingRequest["protocol"] = httpService.protocol
        bindingRequest["url"] = "${httpService.protocol}://${httpService.host}${bindingRequest["URI"]}"

        return bindings
    }

    @JvmStatic
    fun editHTTPRequestMessage(
        editParameters: List<EditParameter>,
        rawRequest: ByteArray,
    ): ByteArray {
        // 1. 修改参数
        val newRequest = editHTTPParameters(editParameters, rawRequest)

        // 2. 修改 HTTP Header
        val newHeaders = editHTTPHeader(editParameters, newRequest)

        // 3. 构建新的 HTTP 消息
        val analyzedRequest = burpCallbacks.helpers.analyzeRequest(newRequest)
        val newRequestBody = newRequest.copyOfRange(analyzedRequest.bodyOffset, newRequest.size)
        val newMessage = burpCallbacks.helpers.buildHttpMessage(newHeaders, newRequestBody)
        Logger.debug("Final request: \n${burpCallbacks.helpers.bytesToString(newMessage)}\nBuild HTTP finished.")

        return newMessage
    }

    /**
     * 修改 HTTP 参数
     */
    @JvmStatic
    fun editHTTPParameters(editParameters: List<EditParameter>, rawRequest: ByteArray): ByteArray {
        var newRequest = rawRequest

        for (ep in editParameters.filter { it.location != ParameterType.HEADER.value }) {
            Logger.debug("edit parameter: $ep, Location: ${ParameterType.fromValue(ep.location)}")
            newRequest = editSingleParameter(ep, newRequest)
        }

        return newRequest
    }

    /**
     * 修改单个 HTTP 参数
     */
    @JvmStatic
    fun editSingleParameter(editParameter: EditParameter, rawRequest: ByteArray): ByteArray {

        var newRequest: ByteArray = rawRequest
        val analyzedRequest = burpCallbacks.helpers.analyzeRequest(newRequest)

        // 根据参数位置，分成两个大的处理逻辑
        // STANDER_PARAM: PARAM_URL, PARAM_BODY, PARAM_COOKIE
        // CONTENT_PARAM: PARAM_JSON, PARAM_XML, PARAM_XML_ATTR, PARAM_MULTIPART_ATTR
        when (editParameter.location) {
            // 如果参数类型是 PARAM_URL, PARAM_BODY, PARAM_COOKIE, 则修改单个参数
            ParameterType.PARAM_URL.value, ParameterType.PARAM_BODY.value, ParameterType.PARAM_COOKIE.value -> {
                val newParameter = burpCallbacks.helpers.buildParameter(
                    editParameter.name,
                    editParameter.value,
                    editParameter.location.toByte()
                )
                val oldParameter =
                    analyzedRequest.parameters.find { it.name == editParameter.name && it.type == editParameter.location.toByte() }

                when (editParameter.location) {
                    // 如果参数存在，则删除参数
                    EditAction.DELETE.value -> {
                        if (oldParameter != null) {
                            newRequest = burpCallbacks.helpers.removeParameter(newRequest, oldParameter)
                        }
                    }
                    // 如果参数不存在，则新增参数，如果参数已经存在，则什么都不做
                    EditAction.ADD.value -> {
                        if (oldParameter == null) {
                            newRequest = burpCallbacks.helpers.addParameter(newRequest, newParameter)
                        }
                    }
                    // 如果参数存在，则更新参数，如果参数不存在，则什么都不做
                    EditAction.UPDATE.value -> {
                        if (oldParameter != null) {
                            newRequest = burpCallbacks.helpers.updateParameter(newRequest, newParameter)
                        }
                    }
                    // 如果参数不存在，则添加参数，如果参数存在，则更新参数
                    EditAction.OVERRIDE.value -> {
                        newRequest = if (oldParameter != null) {
                            burpCallbacks.helpers.updateParameter(newRequest, newParameter)
                        } else {
                            burpCallbacks.helpers.addParameter(newRequest, newParameter)
                        }
                    }
                    // 直接替换整个 BODY
                    EditAction.RAW.value -> {
                        if (editParameter.location == ParameterType.PARAM_BODY.value) {
                            newRequest = burpCallbacks.helpers.buildHttpMessage(
                                analyzedRequest.headers,
                                editParameter.value.encodeToByteArray()
                            )
                        } else {
                            Logger.warn("Action RAW on ${ParameterType.fromValue(editParameter.location)} not support yet.")
                        }
                    }
                }
            }

            // 如果参数类型是 PARAM_JSON, PARAM_XML, PARAM_XML_ATTR, PARAM_MULTIPART_ATTR, 则替换整个 body
            ParameterType.PARAM_JSON.value, ParameterType.PARAM_XML.value, ParameterType.PARAM_XML_ATTR.value, ParameterType.PARAM_MULTIPART_ATTR.value -> {
                when (editParameter.action) {
                    EditAction.RAW.value -> {
                        //
                        newRequest = burpCallbacks.helpers.buildHttpMessage(
                            analyzedRequest.headers,
                            editParameter.value.encodeToByteArray()
                        )
                    }

                    else ->
                        Logger.warn(
                            "Edit ${EditAction.fromValue(editParameter.action)} on ${
                                ParameterType.fromValue(
                                    editParameter.location
                                )
                            } not support yet."
                        )
                }
            }
        }

        return newRequest
    }

    /**
     * 修改 HTTP Header
     */
    @JvmStatic
    fun editHTTPHeader(editParameters: List<EditParameter>, rawRequest: ByteArray): MutableList<String> {
        val analyzedRequest = burpCallbacks.helpers.analyzeRequest(rawRequest)
        val headers = analyzedRequest.headers

        for (editParameter in editParameters.filter { it.location == ParameterType.HEADER.value }) {
            Logger.debug("edit parameter: $editParameter, Location: ${ParameterType.fromValue(editParameter.location)}")
            val existedHeader = headers.find { it.lowercase().startsWith("${editParameter.name}: ".lowercase()) }
            when (editParameter.action) {
                EditAction.ADD.value -> {
                    if (existedHeader == null) {
                        headers.add("${editParameter.name}: ${editParameter.value}")
                    }
                }

                EditAction.UPDATE.value -> {
                    if (existedHeader != null) {
                        headers.remove(existedHeader)
                        headers.add("${editParameter.name}: ${editParameter.value}")
                    }
                }

                EditAction.OVERRIDE.value -> {
                    if (existedHeader == null) {
                        headers.add("${editParameter.name}: ${editParameter.value}")
                    } else {
                        headers.remove(existedHeader)
                        headers.add("${editParameter.name}: ${editParameter.value}")
                    }
                }

                EditAction.DELETE.value -> {
                    if (existedHeader != null) {
                        headers.remove(existedHeader)
                    }
                }

                else -> {
                    Logger.warn(
                        "Edit ${EditAction.fromValue(editParameter.action)} on ${
                            ParameterType.fromValue(
                                editParameter.location
                            )
                        } not support yet."
                    )
                }
            }
            Logger.debug("Edit header ${editParameter.name} finished.")
            // 感觉不需要下面这行
            // analyzedRequest = burpCallbacks.helpers.buildHttpMessage(headers, rawRequest)
        }

        return headers
    }
}