package com.ncmp.partner.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ncmp.partner.data.ConfigStore

/** 开机后重新登记定时任务（避免系统清理后不再执行） */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val config = ConfigStore(context).load()
        if (config.autoDaily && config.hasCookie) {
            DailyTaskScheduler.schedule(context, config.autoHour)
        }
    }
}
