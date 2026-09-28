package com.ncmp.partner.core

/** 作品（歌曲）信息 */
data class Work(
    val id: String,
    val name: String,
    val authorName: String,
    val resourceId: String = "",
)

/** 每日任务中的一首作品 */
data class DailyWork(
    val work: Work,
    val completed: Boolean,
    val score: Double = 0.0,
)

/** 每日任务数据 */
data class DailyTaskData(
    val id: String,
    val count: Int,
    val completedCount: Int,
    val works: List<DailyWork>,
) {
    val isComplete: Boolean get() = count == completedCount && count > 0
}

/** 额外评分任务 */
data class ExtraWork(
    val work: Work,
    val completed: Boolean,
)

/** 用户资料 */
data class Profile(
    val nickname: String,
    val avatarUrl: String,
    val userId: String,
)

/** Cookie 校验结果 */
data class ValidationResult(
    val valid: Boolean,
    val message: String,
    val profile: Profile? = null,
)

/** 单次运行结果 */
data class RunResult(
    val success: Boolean,
    val summary: String,
    val cancelled: Boolean = false,
)

/** Cookie 刷新结果 */
data class RefreshOutcome(
    val success: Boolean,
    val message: String,
    val musicU: String?,
    val csrf: String?,
)

/** 运行事件（用于驱动界面） */
sealed interface RunEvent {
    data class Stage(val text: String) : RunEvent
    data class LogLine(val level: String, val message: String) : RunEvent
    data class DailyProgress(val done: Int, val total: Int) : RunEvent
    data class ExtraProgress(val done: Int, val total: Int) : RunEvent
    data class Account(val nickname: String) : RunEvent
    data class Finished(val result: RunResult) : RunEvent
}

/** 用户主动终止任务时抛出 */
class CancelledException : Exception("任务已被用户终止")

/** 期望中的业务错误（如接口返回非 200） */
class NcmpException(message: String) : Exception(message)
