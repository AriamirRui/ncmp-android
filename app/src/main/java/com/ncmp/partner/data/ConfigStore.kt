package com.ncmp.partner.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * 应用配置（对应桌面版 `config/setting.json`）。
 * 说明：安卓端不包含 GitHub Secrets 自动更新（需要 NaCl sealed box），
 * Cookie 刷新只更新本机配置。
 */
data class Config(
    val musicU: String = "",
    val csrf: String = "",
    val waitMin: Double = 15.0,
    val waitMax: Double = 20.0,
    val score: Int = 3,
    val notifyEmail: String = "",
    val emailPassword: String = "",
    val smtpServer: String = "smtp.gmail.com",
    val smtpPort: Int = 465,
    val neteasePhone: String = "",
    val neteasePassword: String = "",
    val neteaseMd5Password: String = "",
    val autoDaily: Boolean = false,
    val autoHour: Int = 1,
) {
    /** 随机等待时间（对应桌面版 get_wait_time） */
    fun randomWaitSeconds(): Double {
        val lo = min(waitMin, waitMax)
        val hi = max(waitMin, waitMax)
        return if (hi <= lo) lo else lo + Random.nextDouble() * (hi - lo)
    }

    /** 是否填写了可用的 Cookie（排除示例占位符） */
    val hasCookie: Boolean
        get() = musicU.isNotBlank() && !musicU.startsWith("YOUR") && !musicU.startsWith("您的")

    val mailConfigured: Boolean
        get() = notifyEmail.isNotBlank() && emailPassword.isNotBlank()

    fun toJson(): JSONObject = JSONObject().apply {
        put("Cookie_MUSIC_U", musicU)
        put("Cookie___csrf", csrf)
        put("wait_time_min", waitMin)
        put("wait_time_max", waitMax)
        put("score", score)
        put("notify_email", notifyEmail)
        put("email_password", emailPassword)
        put("smtp_server", smtpServer)
        put("smtp_port", smtpPort)
        put("netease_phone", neteasePhone)
        put("netease_password", neteasePassword)
        put("netease_md5_password", neteaseMd5Password)
        put("auto_daily", autoDaily)
        put("auto_hour", autoHour)
    }

    companion object {
        fun fromJson(json: JSONObject): Config = Config(
            musicU = json.optString("Cookie_MUSIC_U", ""),
            csrf = json.optString("Cookie___csrf", ""),
            waitMin = json.optDouble("wait_time_min", 15.0),
            waitMax = json.optDouble("wait_time_max", 20.0),
            score = json.optInt("score", 3),
            notifyEmail = json.optString("notify_email", ""),
            emailPassword = json.optString("email_password", ""),
            smtpServer = json.optString("smtp_server", "smtp.gmail.com"),
            smtpPort = json.optInt("smtp_port", 465),
            neteasePhone = json.optString("netease_phone", ""),
            neteasePassword = json.optString("netease_password", ""),
            neteaseMd5Password = json.optString("netease_md5_password", ""),
            autoDaily = json.optBoolean("auto_daily", false),
            autoHour = json.optInt("auto_hour", 1),
        )
    }
}

/** 配置读写（保存在应用私有目录 config.json） */
class ConfigStore(private val context: Context) {

    private val file: File get() = File(context.filesDir, "config.json")

    private val _config = MutableStateFlow(load())
    val config: StateFlow<Config> = _config.asStateFlow()

    fun load(): Config = try {
        if (file.exists()) Config.fromJson(JSONObject(file.readText(Charsets.UTF_8))) else Config()
    } catch (e: Exception) {
        Config()
    }

    fun save(config: Config) {
        file.writeText(config.toJson().toString(2), Charsets.UTF_8)
        _config.value = config
    }

    fun reload() {
        _config.value = load()
    }
}
