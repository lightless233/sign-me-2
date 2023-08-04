package me.lightless.burp.items

/**
 * 规则最终返回的参数修改信息，插件根据这个类对 HTTP 数据包进行修改
 */
data class EditParameter(
    val name: String,
    val value: String,
    val location: Int,
    // 需要对参数进行的操作：
    //   1 - 追加，如果参数不存在则新增，如果参数存在则什么都不做
    //   2 - 更新，如果参数不存在则什么都不做，如果参数存在则更新参数值
    //   3 - 覆盖，如果参数存在，则更新参数值，如果参数不存在，则新增该参数
    //   4 - 删除，移除指定参数
    val action: Int
)
