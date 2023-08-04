package me.lightless.burp.items


data class SignRuleItem(
    val ruleName: String,
    val filter: String,
    val content: String,
    val status: Boolean,
    val ruleType: Int,
    val toolFlag: Int,
    val createdTime: Long,
    val updatedTime: Long,
)
