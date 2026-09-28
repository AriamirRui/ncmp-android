package com.ncmp.partner.work

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.ncmp.partner.core.RunEvent
import com.ncmp.partner.core.TaskRunner
import com.ncmp.partner.data.ConfigStore
import com.ncmp.partner.data.HistoryStore
import com.ncmp.partner.notify.Notifier

/** 后台执行每日任务（WorkManager 定时触发） */
class DailyTaskWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val config = ConfigStore(applicationContext).load()
        if (!config.hasCookie) {
            Notifier.notify(applicationContext, "无法执行每日任务", "尚未配置 Cookie，请打开应用完成配置")
            return Result.failure()
        }

        Notifier.ensureChannel(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                setForeground(
                    ForegroundInfo(
                        1002,
                        Notifier.build(applicationContext, "音乐合伙人", "正在执行每日任务…", ongoing = true),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                    )
                )
            } catch (_: Exception) {
            }
        }

        val runner = TaskRunner(HistoryStore(applicationContext))
        var lastStage = "正在执行每日任务…"
        val result = runner.run(config) { event ->
            if (event is RunEvent.Stage) lastStage = event.text
        }

        val title = when {
            result.cancelled -> "任务已终止"
            result.success -> "每日任务完成"
            else -> "每日任务失败"
        }
        Notifier.notify(applicationContext, title, "$lastStage\n${result.summary}")
        return if (result.success) Result.success() else Result.retry()
    }
}
