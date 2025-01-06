package me.lightless.burp

const val VERSION = "1.2.0-SNAPSHOT"

/**
 * Burp Parameter Location Constant
 */
enum class ParameterType(val value: Int) {
    PARAM_URL(0),
    PARAM_BODY(1),
    PARAM_COOKIE(2),
    PARAM_XML(3),
    PARAM_XML_ATTR(4),
    PARAM_MULTIPART_ATTR(5),
    PARAM_JSON(6),
    HEADER(10), ;

    companion object {
        private val valueMap = entries.associateBy { it.value }
        fun fromValue(value: Int): ParameterType? = valueMap[value]
    }
}


/**
 * Burp Tool Constant
 */
enum class ToolFlag(val value: Int) {
    TOOL_SUITE(1 shl 0),
    TOOL_TARGET(1 shl 1),
    TOOL_PROXY(1 shl 2),
    TOOL_SPIDER(1 shl 3),
    TOOL_SCANNER(1 shl 4),
    TOOL_INTRUDER(1 shl 5),
    TOOL_REPEATER(1 shl 6),
    TOOL_SEQUENCER(1 shl 7),
    TOOL_DECODER(1 shl 8),
    TOOL_COMPARER(1 shl 9),
    TOOL_EXTENDER(1 shl 10), ;

    companion object {
        private val valueMap = entries.associateBy { it.value }
        fun fromValue(value: Int): ToolFlag? = valueMap[value]

        fun getSplitValue(value: Int): List<Int> {
            val result = mutableListOf<Int>()
            valueMap.forEach { (k, v) ->
                if (value and k != 0) {
                    result.add(k)
                }
            }

            return result
        }

        fun getSplitKeys(value: Int): List<String> {
            val result = mutableListOf<String>()
            valueMap.forEach { (k, v) ->
                if (value and k != 0) {
                    result.add(v.toString())
                }
            }

            return result
        }

        private fun getValueByKey(key: String): Int? {
            return entries.find { it.toString() == key }?.value
        }

        fun getValue(key: String): Int {
            var flag = 0
            key.split(",").map { it.trim() }.forEach {
                getValueByKey(it)?.let { v -> flag = flag or v }
            }
            return flag
        }
    }
}

/**
 * PARAM_JSON/PARAM_XML/PARAM_XML_ATTR/PARAM_MULTIPART_ATTR 不被 BurpAPI 支持，因此需要自行处理
 *   0 - 原始，用于 PARAM_JSON/PARAM_XML/PARAM_XML_ATTR/PARAM_MULTIPART_ATTR 类型，替换整个 body 为指定内容
 *   1 - 追加，如果参数不存在则新增，如果参数存在则什么都不做
 *   2 - 更新，如果参数不存在则什么都不做，如果参数存在则更新参数值
 *   3 - 覆盖，如果参数存在，则更新参数值，如果参数不存在，则新增该参数
 *   4 - 删除，移除指定参数
 */
enum class EditAction(val value: Int) {
    RAW(0),
    ADD(1),
    UPDATE(2),
    OVERRIDE(3),
    DELETE(4), ;

    companion object {
        private val valueMap = entries.associateBy { it.value }
        fun fromValue(value: Int): EditAction? = valueMap[value]
    }
}