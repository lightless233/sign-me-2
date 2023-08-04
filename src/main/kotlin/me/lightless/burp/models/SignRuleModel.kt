package me.lightless.burp.models

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.joda.time.Instant

object SignRuleModel : LongIdTable("sign_rule") {
    val ruleName = varchar("rule_name", 64)
    val filter = varchar("filter", 256)

    // 规则内容
    //   如果是简单规则，该字段是一个 JSON
    //   如果是复杂规则，直接是代码
    val content = text("content")

    // 规则状态：
    //   true - 开启
    //   false - 关闭
    val status = bool("status").default(false)

    // 规则类型：
    //   1 - 复杂规则
    //   2 - 简单规则
    val ruleType = integer("rule_type").default(1)

    // 生效范围，只对指定的 burp 工具生效
    val toolFlag = integer("tool_flag").default(0)

    val createdTime = long("created_time").clientDefault { Instant.now().millis }
    val updatedTime = long("updated_time").clientDefault { Instant.now().millis }
    val isDeleted = bool("is_deleted").default(false)
}

class SignRuleDAO(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<SignRuleDAO>(SignRuleModel)

    var ruleName by SignRuleModel.ruleName
    var filter by SignRuleModel.filter
    var content by SignRuleModel.content
    var status by SignRuleModel.status
    var ruleType by SignRuleModel.ruleType
    var toolFlag by SignRuleModel.toolFlag
    var createdTime by SignRuleModel.createdTime
    var updatedTime by SignRuleModel.updatedTime
    var isDeleted by SignRuleModel.isDeleted
}