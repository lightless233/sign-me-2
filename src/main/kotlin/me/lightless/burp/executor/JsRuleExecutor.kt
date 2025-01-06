package me.lightless.burp.executor

import com.fasterxml.jackson.databind.ObjectMapper
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine
import me.lightless.burp.Logger
import me.lightless.burp.items.EditParameter
import me.lightless.burp.items.SignRuleItem
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import javax.script.SimpleBindings

class JsRuleExecutor : IExecutor {

    init {
        Thread.currentThread().contextClassLoader = Context::class.java.classLoader
    }

    /**
     * JS 代码中常量定义部分
     */
    private val constantTemplate = """
        // 参数类型
        const ParameterType = {
            PARAM_URL: 0,
            PARAM_BODY: 1,
            PARAM_COOKIE: 2,
            PARAM_XML: 3,
            PARAM_XML_ATTR: 4,
            PARAM_MULTIPART_ATTR: 5,
            PARAM_JSON: 6,
            HEADER: 10
        };
        
        // 排序类型
        const SortType = {
            ASC: 1,
            DESC: 0,
        };
        
        // 操作类型
        const EditAction = {
            RAW: 0,
            ADD: 1,
            UPDATE: 2,
            OVERRIDE: 3,
            DELETE: 4,
        }
    """.trimIndent()

    /**
     * JS 代码中工具类定义部分
     */
    private val utilsTemplate = """
        const jsUtils = Java.type("me.lightless.burp.executor.JSUtils");
        function log(msg) {
            jsUtils.log(`[${'$'}{ruleName}] ${'$'}{msg}`);
        }
        
        const utils = {
            md5: (s) => { return jsUtils.md5(s); },
            sha1: (s) => { return jsUtils.sha1(s); },
            hash: (algo, s) => { return jsUtils.hash(algo, s); },
            
            base64encode: (s) => { return jsUtils.base64encode(s); },
            base64decode: (s) => { return jsUtils.base64decode(s); },
            
            getParametersByType: (l) => { return Java.from(jsUtils.getParametersByType(request["query"], l)); },
            getParametersByName: (n) => { return Java.from(jsUtils.getParametersByName(request["query"], n)); },
            getParametersByNameInLocation: (n, l) => { return Java.from(jsUtils.getParametersByNameInLocation(request["query"], n, l)); },
            getParametersByNameInLocationFirstOrNull: (n, l) => { return jsUtils.getParametersByNameInLocationFirstOrNull(request["query"], n, l); },
            getHeaderByName: (n) => { return Java.from(jsUtils.getHeaderByName(request["headers"], n)); },   
            getCookieByName: (n) => { return Java.from(jsUtils.getCookieByName(request["headers"], n)); },

            sortParameters: (pl, o) => { return Java.from(jsUtils.sortParameters(pl, o)); },
            
            getTimestamp: (t) => {
              if (t === 1) {
                return ((new Date().getTime()).toString()).substring(0, 10);
              } else {
                return (new Date().getTime()).toString();
              }
            },
            
            convParametersToMap: (pl) => {
              const p = {};
              pl.forEach(it => p[it.name] = it.value);
              return p;
            },
        }
    """.trimIndent()

    /**
     * JS 代码中 HTTP 类定义部分
     */
    private val httpClientTemplate = """
        const jsHttpClient = Java.type("me.lightless.burp.executor.HttpClient");
        const httpClient = {
          request: (m, u, p, b, j, f, h, r) => { return jsHttpClient.request(m, u, p, b, j, f, h, r); },
          get: (u, p, h, r) => { return jsHttpClient.get(u, p, h, r); },
          post: (u, p, b, j, f, h, r) => { return jsHttpClient.post(u, p, b, j, f, h, r); },
        };
    """.trimIndent()

    /**
     * JS 模板定义
     */
    private val jsTemplate = """
        "use strict";
        ///// constant start
        $constantTemplate
        ///// constant end
        
        ///// utils start
        $utilsTemplate
        ///
        $httpClientTemplate
        ///// utils end
        
        //////////
        {{code}}
        //////////
        
        let result;
        try {
            result = {success: true, error: ``, data: main()}
        } catch (e) {
            result = {success: false, error: `${'$'}{e.stack}, ${'$'}{e}`, data: null};
        }
        JSON.stringify(result);
    """.trimIndent()


    /**
     * JS Engine
     */
    private val engine = GraalJSScriptEngine.create(
        null,
        Context.newBuilder("js")
            .allowHostAccess(HostAccess.ALL)
            .allowHostClassLookup { true }
            .hostClassLoader(Context::class.java.classLoader)
            .allowExperimentalOptions(true)
            .option("js.nashorn-compat", "true")
            .option("js.ecmascript-version", "latest")
    )

    override fun execute(bindings: SimpleBindings, signRuleItem: SignRuleItem): List<EditParameter> {
        val replaceResult = mutableListOf<EditParameter>()

        // 把规则名称塞进去
        val ruleName = signRuleItem.ruleName
        bindings["ruleName"] = ruleName

        // 执行代码
        val engineResult = try {
            engine.eval(buildJS(signRuleItem.content), bindings)
        } catch (ex: Exception) {
            Logger.error("Error while execute JS rule, rule name: $ruleName. Error: $ex\n${ex.stackTraceToString()}")
            return emptyList()
        }

        // 解析 JS 脚本的输出结果
        val resultMap = ObjectMapper().readValue(engineResult as String, Map::class.java)
        Logger.debug("Rule $ruleName eval result: $resultMap")

        // 还需要再判断一下 JS 脚本是否执行成功了
        if (!(resultMap["success"] as Boolean)) {
            Logger.error("Rule $ruleName eval failed. Error: ${resultMap["error"] as String}")
            return emptyList()
        }

        // 获取参数替换信息
        try {
            if (resultMap["data"] != null && resultMap["data"] is List<*>) {
                (resultMap["data"] as List<*>).forEach {
                    it as Map<*, *>
                    replaceResult.add(
                        EditParameter(
                            it.getOrDefault("name", "") as String,
                            it.getOrDefault("value", "") as String,
                            it.getOrDefault("location", -1) as Int,
                            it.getOrDefault("action", -1) as Int
                        )
                    )
                }
            }
            return replaceResult
        } catch (ex: Exception) {
            Logger.error("Error while parse `$ruleName` replace info. Error: $ex\n${ex.stackTraceToString()}")
            return emptyList()
        }
    }

    /**
     * 构建真正执行的 JS 代码
     * 数据库中存储的只是用户写的部分，执行前还需要加一些东西，比如 utils 等等
     */
    private fun buildJS(code: String) = jsTemplate.replace("{{code}}", code)
}