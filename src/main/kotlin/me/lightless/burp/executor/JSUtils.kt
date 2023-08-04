package me.lightless.burp.executor

import com.fasterxml.jackson.databind.ObjectMapper
import me.lightless.burp.Logger
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.graalvm.polyglot.HostAccess
import java.security.MessageDigest
import java.time.Duration
import java.util.Base64

@Suppress("unused")
object JSUtils {

    @JvmStatic
    @HostAccess.Export
    fun log(msg: String) {
        Logger.info(msg);
    }

    /**
     * 计算指定字符串的MD5值
     */
    @JvmStatic
    @HostAccess.Export
    fun md5(s: String) = MessageDigest.getInstance("md5").digest(s.toByteArray())
        .joinToString(separator = "") { byte -> "%02x".format(byte) }

    /**
     * 计算指定字符串的 sha1 值
     */
    @JvmStatic
    @HostAccess.Export
    fun sha1(s: String) = MessageDigest.getInstance("sha1").digest(s.toByteArray())
        .joinToString(separator = "") { byte -> "%02x".format(byte) }

    /**
     * 计算指定字符串的指定hash算法值
     */
    @JvmStatic
    @HostAccess.Export
    fun hash(algo: String, s: String) = MessageDigest.getInstance(algo).digest(s.toByteArray())
        .joinToString(separator = "") { byte -> "%02x".format(byte) }

    /**
     * base64 编码
     */
    @JvmStatic
    @HostAccess.Export
    fun base64encode(s: String): String = Base64.getEncoder().encodeToString(s.toByteArray())

    /**
     * base64 解码
     */
    @JvmStatic
    @HostAccess.Export
    fun base64decode(s: String) = Base64.getDecoder().decode(s).decodeToString()

    /**
     * 根据参数类型，获取该位置的所有参数
     */
    @JvmStatic
    @HostAccess.Export
    fun getParametersByType(parameters: List<Map<String, Any>>, type: Int): List<Map<String, Any>> {
        return parameters.filter { it["location"] as Int == type }
    }

    /**
     * 根据参数名获取所有参数
     */
    @JvmStatic
    @HostAccess.Export
    fun getParametersByName(parameters: List<Map<String, Any>>, name: String): List<Map<String, Any>> =
        parameters.filter { it["name"] == name }

    /***
     * 根据参数名称，获取指定位置的参数
     */
    @JvmStatic
    @HostAccess.Export
    fun getParametersByNameInLocation(
        parameters: List<Map<String, Any>>, name: String, type: Int
    ): List<Map<String, Any>> = parameters.filter { it["name"] == name && it["location"] as Int == type }

    /**
     * 根据参数名和指定位置，获取第一个参数，如果不存在则返回 null
     */
    @JvmStatic
    @HostAccess.Export
    fun getParametersByNameInLocationFirstOrNull(parametersList: List<Map<String, Any>>, name: String, type: Int) =
        parametersList.firstOrNull { it["name"] == name && it["location"] as Int == type }

    /**
     * 对给定的参数列表进行排序
     */
    @JvmStatic
    @HostAccess.Export
    fun sortParameters(parameters: List<Map<String, Any>>, order: Int): List<Map<String, Any>> = if (order == 1) {
        parameters.sortedBy { it["name"] as String }
    } else {
        parameters.sortedByDescending { it["name"] as String }
    }

    /**
     * 获取指定名称的 HTTP 头
     */
    @JvmStatic
    @HostAccess.Export
    fun getHeaderByName(headers: Map<String, String>, name: String) =
        headers.filter { it.key.lowercase() == name.lowercase() }

    /**
     * 获取指定名称的 Cookie
     */
    @JvmStatic
    @HostAccess.Export
    fun getCookieByName(cookies: Map<String, String>, name: String) =
        cookies.filter { it.key.lowercase() == name.lowercase() }

}

@Suppress("unused")
object HttpClient {
    private val client =
        OkHttpClient.Builder().connectTimeout(Duration.ofSeconds(12)).writeTimeout(Duration.ofSeconds(12))
            .readTimeout(Duration.ofSeconds(12)).build()

    private val clientNoRedirect =
        OkHttpClient.Builder().connectTimeout(Duration.ofSeconds(12)).writeTimeout(Duration.ofSeconds(12))
            .readTimeout(Duration.ofSeconds(12)).followRedirects(false).followSslRedirects(false).build()

    private fun addHeaders(requestBuilder: Request.Builder, headers: Map<String, Any>): Request.Builder {
        headers.forEach {
            if (it.value is String) {
                requestBuilder.addHeader(it.key, it.value as String)
            } else if (it.value is Int) {
                requestBuilder.addHeader(it.key, it.value.toString())
            }
        }

        return requestBuilder
    }

    private fun buildURLWithParams(url: String, params: Map<String, Any>) =
        url.toHttpUrl().newBuilder().also { builder ->
            params.forEach {
                if (it.value is String) {
                    builder.addQueryParameter(it.key, it.value as String)
                } else if (it.value is Int) {
                    builder.addQueryParameter(it.key, it.value.toString())
                }
            }
        }

    @JvmStatic
    @HostAccess.Export
    fun request(
        method: String,
        url: String,
        params: Map<String, Any> = emptyMap(),
        body: Map<String, Any> = emptyMap(),
        json: Map<String, Any> = emptyMap(),
        form: Map<String, Any> = emptyMap(),
        headers: Map<String, Any> = emptyMap(),
        followRedirect: Boolean = true
    ): Response {
        // 构建请求 URL
        val urlBuilder = buildURLWithParams(url, params)

        // 添加请求头
        val requestBuilder = Request.Builder().url(urlBuilder.build())
        addHeaders(requestBuilder, headers)

        // 根据传入的参数，构建 request body
        val requestBody: RequestBody? =
            if (body.isNotEmpty()) {
                val builder = FormBody.Builder()
                body.forEach {
                    builder.add(it.key, it.value.toString())
                }
                builder.build()
            } else if (json.isNotEmpty()) {
                val mediaType = "application/json; charset=utf-8".toMediaType()
                ObjectMapper().writeValueAsString(json).toRequestBody(mediaType)
            } else if (form.isNotEmpty()) {
                val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
                form.forEach {
                    builder.addFormDataPart(it.key, it.value.toString())
                }
                builder.build()
            } else {
                null
            }

        val httpClient = if (followRedirect) client else clientNoRedirect
        val request = requestBuilder.method(method, requestBody).build()
        return httpClient.newCall(request).execute()
    }

    @JvmStatic
    @HostAccess.Export
    fun get(
        url: String,
        params: Map<String, Any> = emptyMap(),
        headers: Map<String, Any> = emptyMap(),
        followRedirect: Boolean = true
    ) =
        request("GET", url, params, headers = headers, followRedirect = followRedirect)

    @JvmStatic
    @HostAccess.Export
    fun post(
        url: String,
        params: Map<String, Any> = emptyMap(),
        body: Map<String, Any> = emptyMap(),
        json: Map<String, Any> = emptyMap(),
        form: Map<String, Any> = emptyMap(),
        headers: Map<String, Any> = emptyMap(),
        followRedirect: Boolean = true
    ) = request("POST", url, params, body, json, form, headers, followRedirect)

}