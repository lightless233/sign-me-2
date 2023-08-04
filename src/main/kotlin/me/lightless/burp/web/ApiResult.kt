package me.lightless.burp.web

data class ApiResult<T>(
    val code: Int,
    val message: String,
    val data: T?
) {
    companion object {
        fun <T> ok(data: T): ApiResult<T> {
            return ApiResult(2000, "success", data)
        }

        fun <T> okWithoutData(): ApiResult<T> {
            return ApiResult(2000, "success", null)
        }

        fun <T> err(message: String): ApiResult<T> {
            return ApiResult(4000, message, null)
        }
    }
}