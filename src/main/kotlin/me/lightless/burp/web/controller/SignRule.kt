package me.lightless.burp.web.controller

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.lightless.burp.ToolFlag
import me.lightless.burp.service.SignRuleService
import me.lightless.burp.web.ApiResult
import me.lightless.burp.web.request.CreateSignRuleRequest
import me.lightless.burp.web.request.DeleteSignRuleRequest
import me.lightless.burp.web.request.ToggleSignRuleStatus
import me.lightless.burp.web.request.UpdateSignRuleRequest
import me.lightless.burp.web.response.CreateSignRuleResponse
import me.lightless.burp.web.response.UpdateSignRuleResponse
import me.lightless.burp.web.vo.SignRuleVO
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
}