package com.ncmp.partner.core

import kotlin.math.max
import kotlin.math.min

/** 可中断的等待与取消检查（对应桌面版的 sleep_interruptible） */
object Cancellable {

    const val CANCEL_MESSAGE = "任务已被用户终止"

    fun check(cancel: () -> Boolean) {
        if (cancel()) throw CancelledException()
    }

    /** 等待 [seconds] 秒，期间每 200ms 检查一次取消请求 */
    fun sleep(seconds: Double, cancel: () -> Boolean) {
        val endAt = System.currentTimeMillis() + (max(0.0, seconds) * 1000).toLong()
        while (true) {
            check(cancel)
            val remain = endAt - System.currentTimeMillis()
            if (remain <= 0) return
            try {
                Thread.sleep(min(200L, remain))
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw CancelledException()
            }
        }
    }
}
