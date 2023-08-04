package me.lightless.burp.executor

import me.lightless.burp.items.EditParameter
import me.lightless.burp.items.SignRuleItem
import javax.script.SimpleBindings

interface IExecutor {
    fun execute(bindings: SimpleBindings, signRuleItem: SignRuleItem): List<EditParameter>
}