package me.lightless.burp.web.controller

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.lightless.burp.Logger
import me.lightless.burp.ToolFlag
import me.lightless.burp.executor.JsRuleExecutor
import me.lightless.burp.items.SignRuleItem
import me.lightless.burp.models.SignRuleDAO
import me.lightless.burp.models.SignRuleModel
import me.lightless.burp.service.SignRuleService
import me.lightless.burp.utils.BurpUtils
import me.lightless.burp.web.ApiResult
import me.lightless.burp.web.request.*
import me.lightless.burp.web.response.CreateSignRuleResponse
import me.lightless.burp.web.response.UpdateSignRuleResponse
import me.lightless.burp.web.vo.SignRuleVO
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.SQLException


fun Route.signRuleRoutes() {

    /**
     * 创建签名规则
     */
    post("/create") {
        val request = call.receive<CreateSignRuleRequest>()

        // 校验参数是否合法
        if (request.ruleName.isBlank()) {
            return@post call.respond(ApiResult.err<Nothing>("ruleName is required."))
        }
        if (request.content.isBlank()) {
            return@post call.respond(ApiResult.err<Nothing>("content is required."))
        }
        if (request.ruleType != 1 && request.ruleType != 2) {
            return@post call.respond(ApiResult.err<Nothing>("ruleType is invalid."))
        }
        // TODO 如果是专家规则，需要检查JS里是否存在 main 方法

        // 把 toolFlag 转换为 Int
        val toolFlag = ToolFlag.getValue(request.toolFlag)

        // 插入规则信息
        try {
            val ruleDao = SignRuleService.createNewSignRule(
                request.ruleName,
                request.filter,
                request.content,
                request.status,
                toolFlag,
            )
            return@post call.respond(ApiResult.ok(CreateSignRuleResponse(ruleDao.id.value)))
        } catch (ex: SQLException) {
            return@post call.respond(ApiResult.err<Nothing>("Error while create new sign rule. Error: $ex\n${ex.stackTraceToString()}"))
        }
    }

    /**
     * 更新签名规则
     */
    post("/update") {
        val request = call.receive<UpdateSignRuleRequest>()

        // 校验参数是否合法
        if (request.ruleName.isBlank()) {
            return@post call.respond(ApiResult.err<Nothing>("ruleName is required."))
        }
        if (request.content.isBlank()) {
            return@post call.respond(ApiResult.err<Nothing>("content is required."))
        }

        // 把 toolFlag 转换为 Int
        val toolFlag = ToolFlag.getValue(request.toolFlag)

        try {
            val signRule = SignRuleService.updateSignRule(
                request.ruleId,
                request.ruleName,
                request.filter,
                request.content,
                request.status,
                toolFlag
            )
            return@post call.respond(ApiResult.ok(UpdateSignRuleResponse(signRule.id.value)))
        } catch (ex: SQLException) {
            return@post call.respond(ApiResult.err<Nothing>("Error while update sign rule. Id: ${request.ruleId}. Error: $ex\n${ex.stackTraceToString()}"))
        }
    }

    /**
     * 删除签名规则
     */
    post("/delete") {
        val request = call.receive<DeleteSignRuleRequest>()
        try {
            SignRuleService.deleteSignRuleById(request.ruleId)
            return@post call.respond(ApiResult.okWithoutData<Nothing>())
        } catch (ex: SQLException) {
            return@post call.respond(ApiResult.err<Nothing>("Error while delete sign rule. Id: ${request.ruleId}. Error: $ex\n${ex.stackTraceToString()}"))
        }
    }

    /**
     * 根据 id 获取规则内容
     */
    get("/get_by_id") {
        val parameters = call.request.queryParameters
        val ruleId = parameters["ruleId"]?.toLongOrNull()
            ?: return@get call.respond(ApiResult.err<Nothing>("'ruleId' is required."))

        try {
            val vo = SignRuleService.getSignRuleById(ruleId)?.let { SignRuleVO.fromDao(it) }
            return@get call.respond(ApiResult.ok(vo))
        } catch (ex: SQLException) {
            return@get call.respond(ApiResult.err<Nothing>("Error while get sign rule. Error: $ex\n${ex.stackTraceToString()}"))
        }

    }

    /**
     * 列出规则列表，如果传入了规则名，则执行模糊搜索
     */
    get("/list") {
        // TODO 暂时不考虑分页的问题，后续再增加
        val parameters = call.request.queryParameters

        // 如果 ruleName 为空，则获取所有规则，否则执行 like 查询
        try {
            val signRules = transaction {
                SignRuleService.listSignRule(parameters["ruleName"]).map { SignRuleVO.fromDao(it) }.toList()
            }
            return@get call.respond(ApiResult.ok(signRules))
        } catch (ex: SQLException) {
            return@get call.respond(ApiResult.err<Nothing>("Error while list sign rules. Error: $ex\n${ex.stackTraceToString()}"))
        }
    }

    /**
     * 修改规则状态
     */
    post("/toggle-status") {
        val request = call.receive<ToggleSignRuleStatus>()

        try {
            if (SignRuleService.toggleSignRuleStatus(request.ruleId, request.status) != null) {
                return@post call.respond(ApiResult.ok(request.ruleId))
            } else {
                return@post call.respond(ApiResult.err<Nothing>("RuleId not exist."))
            }
        } catch (ex: SQLException) {
            return@post call.respond(ApiResult.err<Nothing>("Error when toggle rule status. Error: $ex\n${ex.stackTraceToString()}"))
        }
    }

    /**
     * 测试规则
     */
    post("/test-rule") {
        val request = call.receive<TestSignRuleRequest>()
        // TODO HERE!
        if (request.rawRequest.isBlank()) {
            return@post call.respond(ApiResult.err<Nothing>("rawRequest is required."))
        }
        if (request.host.isBlank()) {
            return@post call.respond(ApiResult.err<Nothing>("host is required."))
        }
        if (request.protocol.isBlank() || request.protocol !in listOf("http", "https")) {
            return@post call.respond(ApiResult.err<Nothing>("protocol is required and must be 'http' or 'https'."))
        }
        if (request.port < 1 || request.port > 65535) {
            return@post call.respond(ApiResult.err<Nothing>("port is required and must be in 1..65535."))
        }
        if (request.ruleId < 1) {
            return@post call.respond(ApiResult.err<Nothing>("ruleId is required."))
        }

        // 根据规则 ID 获取规则内容
        val signRuleItem = transaction {
            SignRuleDAO.find {
                SignRuleModel.isDeleted eq false and
                        (SignRuleModel.status eq true) and
                        (SignRuleModel.id eq request.ruleId)
            }.firstOrNull()?.let {
                SignRuleItem(
                    it.ruleName,
                    it.filter,
                    it.content,
                    it.status,
                    it.ruleType,
                    it.toolFlag,
                    it.createdTime,
                    it.updatedTime
                )
            }
        }
        if (signRuleItem == null) {
            return@post call.respond(ApiResult.err<Nothing>("RuleId not exist or status is false."))
        }

        // 构建一个 httpService 对象
        val tempHTTPService =
            BurpUtils.burpCallbacks.helpers.buildHttpService(request.host, request.port, request.protocol)

        // 构建 bindings 对象
        val bindings = BurpUtils.buildBinding(request.rawRequest.toByteArray(), tempHTTPService)

        // 执行 JS 代码，获取执行结果，这里先临时新建一个 JS 引擎，后续再优化
        // TODO 如果后续添加了 SimpleRule，需要判断是哪种规则
        val jsRuleExecutor = JsRuleExecutor()
        val editParameters = jsRuleExecutor.execute(bindings, signRuleItem)

        // 修改 HTTP 包，并获取修改好的结果
        val newRequestMessage = BurpUtils.editHTTPRequestMessage(editParameters, request.rawRequest.toByteArray())

        return@post call.respond(ApiResult.ok(mapOf("requestMessage" to newRequestMessage.toString(Charsets.UTF_8))))
    }
}