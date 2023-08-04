package me.lightless.burp.web.vo

import me.lightless.burp.ToolFlag
import me.lightless.burp.models.SignRuleDAO

data class SignRuleVO(
    val ruleId: Long,
    val ruleName: String,
    val ruleType: Int,
    val filter: String,
    val content: String,
    val status: Boolean,
    val toolFlag: Int,
    val readableToolFlag: List<String>,
) {

    companion object {

        /**
         * 将 dao 转换为 vo
         */
        @JvmStatic
        fun fromDao(dao: SignRuleDAO): SignRuleVO {

            return SignRuleVO(
                dao.id.value,
                dao.ruleName,
                dao.ruleType,
                dao.filter,
                dao.content,
                dao.status,
                dao.toolFlag,
                ToolFlag.getSplitKeys(dao.toolFlag),
            )
        }
    }

}