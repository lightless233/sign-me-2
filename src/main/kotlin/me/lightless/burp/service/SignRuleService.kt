package me.lightless.burp.service

import me.lightless.burp.models.SignRuleDAO
import me.lightless.burp.models.SignRuleModel
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.Instant
import java.sql.SQLDataException

object SignRuleService {
    /**
     * 新增签名规则
     */
    fun createNewSignRule(
        ruleName: String, filter: String, content: String, status: Boolean, toolFlag: Int
    ) = transaction {

        // 先检查是否有同名的规则
        if (getSignRuleByName(ruleName) != null) {
            throw SQLDataException("rule name '$ruleName' already exist.")
        }

        SignRuleDAO.new {
            this.ruleName = ruleName
            this.filter = filter
            this.content = content
            this.status = status
            // 目前仅有复杂规则，后续再添加简单规则
            this.ruleType = 1
            this.toolFlag = toolFlag
        }
    }

    /**
     * 更新签名规则
     */
    fun updateSignRule(
        id: Long, ruleName: String, filter: String, content: String, status: Boolean, toolFlag: Int
    ) = transaction {
        val dao = this@SignRuleService.getSignRuleById(id) ?: throw SQLDataException("id: $id sign rule not exists.")
        dao.ruleName = ruleName
        dao.filter = filter
        dao.content = content
        dao.status = status
        dao.toolFlag = toolFlag
        dao.updatedTime = Instant.now().millis
        dao
    }

    /**
     * 删除一条规则
     */
    fun deleteSignRuleById(id: Long) = transaction {
        val dao = getSignRuleById(id) ?: throw SQLDataException("id: $id sign rule not exists.")
        dao.isDeleted = true
        dao.updatedTime = Instant.now().millis
    }

    /**
     * 列出所有签名规则，如果 ruleName 为空，则执行 like 查询，否则获取所有规则
     */
    fun listSignRule(ruleName: String?) = transaction {
        var condition = SignRuleModel.isDeleted eq false
        if (!ruleName.isNullOrBlank()) {
            condition = condition and (SignRuleModel.ruleName like "%$ruleName%")
        }

        SignRuleDAO.find { condition }.orderBy(SignRuleModel.id to SortOrder.ASC)
    }

    /**
     * 根据 id 获取 sign rule
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun getSignRuleById(id: Long) = transaction {
        SignRuleDAO.find { SignRuleModel.isDeleted eq false and (SignRuleModel.id eq id) }.firstOrNull()
    }

    /**
     * 根据规则名获取规则
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun getSignRuleByName(name: String) = transaction {
        SignRuleDAO.find { SignRuleModel.isDeleted eq false and (SignRuleModel.ruleName eq name) }.firstOrNull()
    }

    /**
     * 修改规则开关状态
     */
    fun toggleSignRuleStatus(id: Long, status: Boolean) = transaction {
        getSignRuleById(id)?.also {
            it.status = status
            it.updatedTime = Instant.now().millis
        }?.id?.value
    }

    /**
     * 测试签名规则
     */
    fun testSignRule(ruleId: Long, rawRequest: String) = transaction {
        // TODO
    }

}