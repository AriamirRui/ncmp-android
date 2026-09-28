package com.ncmp.partner

import android.app.Application
import com.ncmp.partner.data.ConfigStore
import com.ncmp.partner.notify.Notifier
import com.ncmp.partner.work.DailyTaskScheduler

class NcmpApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Notifier.ensureChannel(this)

        // 启动时校正定时任务（配置变更或重装后保持一致）
        val config = ConfigStore(this).load()
        if (config.autoDaily && config.hasCookie) {
            DailyTaskScheduler.schedule(this, config.autoHour)
        }
    }
}
