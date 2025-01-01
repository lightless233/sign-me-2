package me.lightless.burp.web.request

import me.lightless.burp.ToolFlag

/**
 * 创建规则的请求
 */
data class CreateSignRuleRequest(
    val ruleName: String,
    val filter: String,
    val content: String,
    val status: Boolean,
    val ruleType: Int,
    val toolFlag: String,
)

/**
 * 更新规则的请求
 */
data class UpdateSignRuleRequest(
    val ruleId: Long,
    val ruleName: String,
    val ruleType: Long,
    val filter: String,
    val content: String,
    val status: Boolean,
    val toolFlag: String,
)

/**
 * 删除签名规则的请求
 */
data class DeleteSignRuleRequest(
    val ruleId: Long,
)

/**
 * 修改规则状态
 */
data class ToggleSignRuleStatus(
    val ruleId: Long,
    val status: Boolean,
)