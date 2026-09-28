package com.ncmp.partner.core

import com.ncmp.partner.data.Config
import com.ncmp.partner.data.HistoryStore
import com.ncmp.partner.data.RunRecord
import com.ncmp.partner.notify.MailSender
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 任务运行器：对应桌面版 `src/core/pipeline.py`。
 * 负责 Cookie 校验 → 用户信息 → 每日任务 → 额外任务 的完整流程，
 * 并通过 [RunEvent] 把日志与进度实时推给界面。
 */
class TaskRunner(
    private val history: HistoryStore,
    private val notifyMail: Boolean = true,
) {
    @Volatile
    private var cancelRequested = false

    @Volatile
    var running: Boolean = false
        private set

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun requestCancel() {
        cancelRequested = true
    }

    /** 只读校验 Cookie（可返回昵称） */
    fun validate(config: Config, emit: (RunEvent) -> Unit): ValidationResult {
        val line = { level: String, msg: String -> emit(RunEvent.LogLine(level, "    $msg")) }
        val api = NcmpApi(config.musicU, config.csrf, line, { false })
        emit(RunEvent.Stage("正在验证 Cookie..."))
        val result = api.validate()
        line(if (result.valid) "INFO" else "ERROR", "Cookie 验证结果：${result.message}")
        result.profile?.let { emit(RunEvent.Account(it.nickname)) }
        return result
    }

    /** 执行完整任务流程 */
    fun run(config: Config, emit: (RunEvent) -> Unit): RunResult {
        cancelRequested = false
        running = true
        val runId = history.newId()

        fun log(level: String, message: String) {
            val line = "${timeFormat.format(Date())} [$level] $message"
            history.appendLog(runId, line)
            emit(RunEvent.LogLine(level, line))
        }

        var nickname = ""
        try {
            emit(RunEvent.Stage("正在验证 Cookie..."))
            val api = NcmpApi(config.musicU, config.csrf, { l, m -> log(l, m) }, { cancelRequested })

            val validation = api.validate()
            if (!validation.valid) {
                log("ERROR", validation.message)
                if (notifyMail) notifyFailure(config, "Cookie 失效提醒", "请更新 Cookie\n详细信息: ${validation.message}")
                return finish(runId, false, validation.message, emit)
            }

            emit(RunEvent.Stage("正在验证用户信息..."))
            val profile = validation.profile
            nickname = profile?.nickname.orEmpty()
            if (nickname.isNotEmpty()) {
                log("INFO", "用户名: $nickname")
                emit(RunEvent.Account(nickname))
            }

            emit(RunEvent.Stage("正在进行每日评分任务..."))
            val daily = api.dailyTask()
            emit(RunEvent.DailyProgress(daily.completedCount, daily.count))
            if (!daily.isComplete) {
                api.processDaily(daily, config) { done, total -> emit(RunEvent.DailyProgress(done, total)) }
            }

            emit(RunEvent.Stage("正在进行额外评分任务..."))
            api.processExtra(
                taskId = daily.id,
                config = config,
                onMeta = { done, total -> emit(RunEvent.ExtraProgress(done, total)) },
                onProgress = { done, total -> emit(RunEvent.ExtraProgress(done, total)) },
            )

            emit(RunEvent.Stage("任务执行完成"))
            return finish(runId, true, "执行成功", emit)
        } catch (e: CancelledException) {
            log("WARN", Cancellable.CANCEL_MESSAGE)
            return finish(runId, false, RunRecord.CANCELLED_SUMMARY, emit, cancelled = true)
        } catch (e: Exception) {
            log("ERROR", "执行失败: ${e.message}")
            if (notifyMail) notifyFailure(config, "执行失败提醒", "程序执行失败：${e.message}")
            return finish(runId, false, "任务执行失败，请查看日志", emit)
        } finally {
            running = false
            cancelRequested = false
        }
    }

    /** Cookie 自动刷新：手机号 + 密码登录换取新 Cookie */
    fun refreshCookie(config: Config, emit: (RunEvent) -> Unit): RefreshOutcome {
        emit(RunEvent.Stage("正在登录并刷新 Cookie..."))
        val result = CookieRefresher.login(
            phone = config.neteasePhone,
            password = config.neteasePassword.ifBlank { null },
            md5Password = config.neteaseMd5Password.ifBlank { null },
        )
        return if (result != null) {
            emit(RunEvent.LogLine("INFO", "${timeFormat.format(Date())} [INFO] Cookie 刷新成功"))
            RefreshOutcome(true, "Cookie 已刷新", result.first, result.second)
        } else {
            val msg = "登录失败，请检查手机号与密码"
            emit(RunEvent.LogLine("ERROR", "${timeFormat.format(Date())} [ERROR] $msg"))
            RefreshOutcome(false, msg, null, null)
        }
    }

    private fun finish(
        runId: String,
        success: Boolean,
        summary: String,
        emit: (RunEvent) -> Unit,
        cancelled: Boolean = false,
    ): RunResult {
        val record = RunRecord(
            id = runId,
            time = history.nowText(),
            kind = "任务运行",
            success = success,
            summary = summary,
        )
        history.save(record)
        val result = RunResult(success, summary, cancelled)
        emit(RunEvent.Finished(result))
        return result
    }

    private fun notifyFailure(config: Config, subject: String, content: String) {
        if (!config.mailConfigured) return
        try {
            MailSender.send(
                host = config.smtpServer,
                port = config.smtpPort,
                user = config.notifyEmail,
                password = config.emailPassword,
                to = config.notifyEmail,
                subject = "网易云音乐合伙人 - $subject",
                body = content,
            )
        } catch (_: Exception) {
        }
    }
}
