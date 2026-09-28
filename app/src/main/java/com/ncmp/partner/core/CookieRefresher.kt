package com.ncmp.partner.core

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Cookie 自动刷新：对应桌面版 `src/utils/auth.py`。
 * 通过手机号 + 密码（或 MD5 密码）登录换取新的 MUSIC_U / __csrf。
 */
object CookieRefresher {

    private const val LOGIN_API = "https://ncma-web.vercel.app/login/cellphone"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val musicUPattern = Pattern.compile("MUSIC_U=([^;]+)")
    private val csrfPattern = Pattern.compile("__csrf=([^;]+)")

    /** @return 成功时返回 (MUSIC_U, __csrf)，失败返回 null */
    fun login(phone: String, password: String?, md5Password: String?): Pair<String, String>? {
        if (phone.isBlank()) return null
        val url = buildString {
            append(LOGIN_API).append("?phone=").append(phone)
            when {
                !md5Password.isNullOrBlank() -> append("&md5_password=").append(md5Password)
                !password.isNullOrBlank() -> append("&password=").append(password)
                else -> return null
            }
        }
        return try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!body.startsWith("{")) return null
                val json = JSONObject(body)
                if (json.optInt("code", -1) != 200) return null

                var musicU: String? = null
                var csrf: String? = null

                // 1) 响应体中的 cookie 字段
                val cookieField = json.optString("cookie", "")
                if (cookieField.isNotBlank()) {
                    musicU = firstGroup(musicUPattern, cookieField)
                    csrf = firstGroup(csrfPattern, cookieField)
                }
                // 2) Set-Cookie 响应头
                if (musicU.isNullOrBlank() || csrf.isNullOrBlank()) {
                    val setCookie = response.headers("Set-Cookie").joinToString("; ")
                    if (musicU.isNullOrBlank()) musicU = firstGroup(musicUPattern, setCookie)
                    if (csrf.isNullOrBlank()) csrf = firstGroup(csrfPattern, setCookie)
                }
                // 3) 接口返回的顶层字段
                if (musicU.isNullOrBlank()) musicU = json.optString("musicU", "").ifBlank { null }
                if (csrf.isNullOrBlank()) csrf = json.optString("csrf", "").ifBlank { null }

                if (!musicU.isNullOrBlank() && !csrf.isNullOrBlank()) musicU to csrf else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun firstGroup(pattern: Pattern, text: String): String? {
        val matcher = pattern.matcher(text)
        return if (matcher.find()) matcher.group(1)?.trim() else null
    }
}
