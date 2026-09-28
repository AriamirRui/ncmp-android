package com.ncmp.partner.core

import android.util.Log
import com.ncmp.partner.data.Config
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * 网易云音乐合伙人接口客户端。
 * 所有请求与桌面版的任务模块、Cookie 校验模块一一对应。
 */
class NcmpApi(
    private val musicU: String,
    private val csrf: String,
    private val log: (level: String, message: String) -> Unit,
    private val cancel: () -> Boolean,
) {
    companion object {
        private const val TAG = "NcmpApi"

        const val URL_USER_INFO = "https://music.163.com/api/nuser/account/get"
        const val URL_DAILY_TASK = "https://interface.music.163.com/api/music/partner/daily/task/get"
        const val URL_EXTRA_LIST =
            "https://interface.music.163.com/api/music/partner/extra/wait/evaluate/work/list"
        const val URL_SIGN = "https://interface.music.163.com/weapi/music/partner/work/evaluate"
        const val URL_REPORT =
            "https://interface.music.163.com/weapi/partner/resource/interact/report"

        private const val REFERER = "https://mp.music.163.com/"
        private const val UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /** 会话级随机串（与桌面版 Signer 实例一致，运行期间保持不变） */
    private val randomStr = NcmpCrypto.randomString(16)

    private val cookieHeader = "MUSIC_U=$musicU; __csrf=$csrf"

    // ------------------------------------------------------------------
    // 基础请求
    // ------------------------------------------------------------------
    private fun newRequest(url: String, referer: String? = null): Request.Builder {
        val builder = Request.Builder()
            .url(url)
            .header("Cookie", cookieHeader)
            .header("User-Agent", UA)
            .header("Accept", "*/*")
        referer?.let { builder.header("Referer", it) }
        return builder
    }

    private fun getJson(url: String, referer: String? = null): JSONObject {
        Cancellable.check(cancel)
        client.newCall(newRequest(url, referer).get().build()).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) throw NcmpException("接口无响应 (HTTP ${response.code})")
            return JSONObject(body)
        }
    }

    private fun postEncrypted(url: String, data: JSONObject, referer: String? = null): JSONObject {
        Cancellable.check(cancel)
        val form = FormBody.Builder()
            .add("params", NcmpCrypto.encryptParams(data.toString(), randomStr))
            .add("encSecKey", NcmpCrypto.encSecKey(randomStr))
            .build()
        val request = newRequest(url, referer).post(form).build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) throw NcmpException("接口无响应 (HTTP ${response.code})")
            return JSONObject(body)
        }
    }

    private fun codeOf(json: JSONObject): Int = json.optInt("code", -1)

    private fun messageOf(json: JSONObject): String =
        json.optString("message").ifBlank { json.optString("msg", "未知错误") }

    // ------------------------------------------------------------------
    // 账号与任务
    // ------------------------------------------------------------------
    /** 获取用户资料（昵称/头像） */
    fun profile(): Profile? {
        val json = getJson(URL_USER_INFO)
        val profile = json.optJSONObject("profile") ?: return null
        return Profile(
            nickname = profile.optString("nickname", ""),
            avatarUrl = profile.optString("avatarUrl", ""),
            userId = profile.optString("userId", ""),
        )
    }

    /** 校验 Cookie：账号信息 + 合伙人任务访问权限 */
    fun validate(): ValidationResult {
        return try {
            val profileJson = getJson(URL_USER_INFO)
            val profileObj = profileJson.optJSONObject("profile")
            if (codeOf(profileJson) != 200 || profileObj == null) {
                return ValidationResult(false, "Cookie 已失效或账号信息不完整")
            }
            val taskJson = getJson(URL_DAILY_TASK)
            if (codeOf(taskJson) != 200) {
                return ValidationResult(false, "当前账号可能没有音乐合伙人权限")
            }
            ValidationResult(
                valid = true,
                message = "Cookie 有效",
                profile = Profile(
                    nickname = profileObj.optString("nickname", ""),
                    avatarUrl = profileObj.optString("avatarUrl", ""),
                    userId = profileObj.optString("userId", ""),
                ),
            )
        } catch (e: CancelledException) {
            throw e
        } catch (e: Exception) {
            ValidationResult(false, "Cookie 验证失败: ${e.message}")
        }
    }

    /** 获取每日任务 */
    fun dailyTask(): DailyTaskData {
        val json = getJson(URL_DAILY_TASK)
        if (codeOf(json) != 200) {
            throw NcmpException("获取每日任务失败: ${messageOf(json)} (响应码: ${codeOf(json)})")
        }
        val data = json.optJSONObject("data") ?: JSONObject()
        val count = data.optInt("count", 0)
        val completedCount = data.optInt("completedCount", 0)
        val worksArray = data.optJSONArray("works") ?: JSONArray()
        val works = ArrayList<DailyWork>(worksArray.length())
        for (i in 0 until worksArray.length()) {
            val item = worksArray.optJSONObject(i) ?: continue
            val workObj = item.optJSONObject("work") ?: continue
            works.add(
                DailyWork(
                    work = parseWork(workObj),
                    completed = item.optBoolean("completed", false),
                    score = item.optDouble("score", 0.0),
                )
            )
        }
        val task = DailyTaskData(
            id = data.optString("id", ""),
            count = count,
            completedCount = completedCount,
            works = works,
        )
        log("INFO", "今日任务：${if (task.isComplete) "已完成" else "未完成"}[$completedCount/$count]")
        return task
    }

    /** 处理每日评分任务，每完成一首回调进度 */
    fun processDaily(task: DailyTaskData, config: Config, onProgress: (done: Int, total: Int) -> Unit) {
        log("INFO", "开始评分...")
        val total = task.works.size
        var done = task.works.count { it.completed }
        onProgress(done, total)
        for (item in task.works) {
            if (item.completed) {
                log("INFO", "${item.work.name}「${item.work.authorName}」已有评分：${item.score.toInt()}分")
                continue
            }
            signWork(task.id, item.work, isExtra = false, config = config)
            done += 1
            onProgress(done, total)
        }
    }

    /** 获取额外评分任务列表与已完成数量 */
    fun extraList(): Pair<List<ExtraWork>, Int> {
        val json = getJson(URL_EXTRA_LIST, REFERER)
        if (codeOf(json) != 200) {
            throw NcmpException("获取额外任务失败: ${messageOf(json)}")
        }
        val array = json.optJSONArray("data") ?: JSONArray()
        val all = ArrayList<ExtraWork>(array.length())
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val workObj = item.optJSONObject("work") ?: continue
            all.add(ExtraWork(parseWork(workObj), item.optBoolean("completed", false)))
        }
        val completed = all.count { it.completed }
        return all.filter { !it.completed } to completed
    }

    /** 评分一首作品（含等待、限流重试、资源异常跳过） */
    fun signWork(taskId: String, work: Work, isExtra: Boolean, config: Config) {
        var attempt = 0
        while (true) {
            attempt += 1
            val delay = config.randomWaitSeconds()
            log("INFO", "等待 %.1f 秒后继续...".format(delay))
            Cancellable.sleep(delay, cancel)

            val data = JSONObject().apply {
                put("taskId", taskId)
                put("workId", work.id)
                val (score, tag) = scoreAndTag(work, config)
                put("score", score)
                put("tags", tag)
                put("customTags", "%5B%5D")
                put("comment", "")
                put("syncYunCircle", "true")
                put("csrf_token", csrf)
                if (isExtra) put("extraResource", "true")
            }

            val json = postEncrypted("$URL_SIGN?csrf_token=$csrf", data)
            val code = codeOf(json)
            val (score, _) = scoreAndTag(work, config)

            if (code == 200) {
                log("INFO", "${work.name}「${work.authorName}」评分完成：${score}分")
                return
            }

            val message = messageOf(json)
            if (message.contains("频繁")) {
                val retryDelay = config.randomWaitSeconds()
                log("WARN", "遇到频率限制，等待 %.1f 秒后重试...".format(retryDelay))
                Cancellable.sleep(retryDelay, cancel)
                if (attempt >= 5) {
                    throw NcmpException("评分失败: 触发频率限制且重试多次未成功")
                }
                continue
            }
            if (code == 405 && message.contains("资源状态异常")) {
                log("WARN", "歌曲「${work.name}」资源状态异常，跳过")
                return
            }
            throw NcmpException("评分失败: $message (响应码: $code)")
        }
    }

    /** 处理全部额外评分任务 */
    fun processExtra(taskId: String, config: Config,
                     onMeta: (done: Int, total: Int) -> Unit,
                     onProgress: (done: Int, total: Int) -> Unit) {
        val (pending, completedCount) = extraList()
        val maxCount = 15
        onMeta(completedCount, maxCount)

        if (completedCount >= maxCount) {
            log("INFO", "今日已完成 $completedCount 个额外评分任务，已达到每日上限")
            return
        }
        if (pending.isEmpty()) {
            log("INFO", "额外评定完成数: $completedCount")
            return
        }

        log("INFO", "发现 ${pending.size} 个待额外评定任务")
        val remaining = maxCount - completedCount
        var success = 0

        for (item in pending) {
            if (success >= remaining) {
                log("INFO", "已完成 $success 个额外评分任务，总计完成 ${completedCount + success} 个")
                break
            }
            try {
                Cancellable.check(cancel)
                reportListen(item.work)
                signWork(taskId, item.work, isExtra = true, config = config)
                success += 1
                log("INFO", "成功完成第 $success/$remaining 个额外评分任务")
                onProgress(completedCount + success, maxCount)

                if (success < remaining) {
                    val delay = config.randomWaitSeconds()
                    log("INFO", "等待 %.1f 秒后继续...".format(delay))
                    Cancellable.sleep(delay, cancel)
                }
            } catch (e: CancelledException) {
                throw e
            } catch (e: Exception) {
                log("WARN", "处理歌曲 ${item.work.name} 失败，尝试下一个: ${e.message}")
            }
        }

        log("INFO", "额外评分任务处理完成，成功评分 $success 首")
        if (success < remaining) {
            log("WARN", "未能完成所有额外评分任务，仅完成 $success/$remaining 个")
        }
    }

    /** 上报表扬听歌记录 */
    private fun reportListen(work: Work) {
        val data = JSONObject().apply {
            put("workId", work.id)
            put("resourceId", work.resourceId)
            put("bizResourceId", "")
            put("interactType", "PLAY_END")
            put("csrf_token", csrf)
        }
        val json = postEncrypted("$URL_REPORT?csrf_token=$csrf", data, REFERER)
        if (codeOf(json) != 200) {
            throw NcmpException("上报听歌记录失败: ${messageOf(json)}")
        }
        log("INFO", "歌曲 ${work.name} 听歌记录上报成功")
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------
    private fun parseWork(obj: JSONObject): Work = Work(
        id = obj.optString("id", ""),
        name = obj.optString("name", ""),
        authorName = obj.optString("authorName", ""),
        resourceId = obj.optString("resourceId", ""),
    )

    /** 评分策略：按歌名/歌手是否含英文决定分数区间（与桌面版一致） */
    private fun scoreAndTag(work: Work, config: Config): Pair<String, String> =
        ScorePolicy.scoreAndTag(work.name, work.authorName, config.score)
}
