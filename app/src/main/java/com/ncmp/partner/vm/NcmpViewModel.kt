package com.ncmp.partner.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ncmp.partner.core.RunEvent
import com.ncmp.partner.core.TaskRunner
import com.ncmp.partner.data.Config
import com.ncmp.partner.data.ConfigStore
import com.ncmp.partner.data.HistoryStore
import com.ncmp.partner.data.RunRecord
import com.ncmp.partner.notify.Notifier
import com.ncmp.partner.work.DailyTaskScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UiLogLine(val level: String, val text: String)

data class UiState(
    val config: Config = Config(),
    val busy: Boolean = false,
    val cancelling: Boolean = false,
    val validating: Boolean = false,
    val stage: String = "就绪",
    val accountState: String = "未检测",
    val accountOk: Boolean? = null,
    val nickname: String = "",
    val avatarUrl: String = "",
    val dailyDone: Int = 0,
    val dailyTotal: Int = 5,
    val extraDone: Int = 0,
    val extraTotal: Int = 15,
    val logs: List<UiLogLine> = emptyList(),
    val lastResult: String = "",
    val lastSuccess: Boolean? = null,
    val history: List<RunRecord> = emptyList(),
    val detailRecord: RunRecord? = null,
    val detailLog: String = "",
    val toast: String? = null,
)

class NcmpViewModel(app: Application) : AndroidViewModel(app) {

    private val configStore = ConfigStore(app)
    private val historyStore = HistoryStore(app)
    private val runner = TaskRunner(historyStore)

    private val _state = MutableStateFlow(UiState(config = configStore.config.value))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var runJob: Job? = null
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    init {
        Notifier.ensureChannel(app)
        refreshHistory()
    }

    // ------------------------------------------------------------------
    // 配置
    // ------------------------------------------------------------------
    fun updateConfig(config: Config) {
        configStore.save(config)
        _state.value = _state.value.copy(config = config)
        if (config.autoDaily && config.hasCookie) {
            DailyTaskScheduler.schedule(getApplication(), config.autoHour)
        } else {
            DailyTaskScheduler.cancel(getApplication())
        }
    }

    fun scheduleAutoTask(config: Config) {
        if (config.autoDaily && config.hasCookie) {
            DailyTaskScheduler.schedule(getApplication(), config.autoHour)
        } else {
            DailyTaskScheduler.cancel(getApplication())
        }
    }

    // ------------------------------------------------------------------
    // 日志
    // ------------------------------------------------------------------
    private fun appendLog(level: String, text: String) {
        val logs = _state.value.logs + UiLogLine(level, text)
        _state.value = _state.value.copy(logs = if (logs.size > 1500) logs.takeLast(1500) else logs)
    }

    fun clearLogs() {
        _state.value = _state.value.copy(logs = emptyList())
    }

    fun dismissToast() {
        _state.value = _state.value.copy(toast = null)
    }

    private fun toast(message: String) {
        _state.value = _state.value.copy(toast = message)
        viewModelScope.launch {
            delay(3200)
            if (_state.value.toast == message) {
                _state.value = _state.value.copy(toast = null)
            }
        }
    }

    // ------------------------------------------------------------------
    // 验证 Cookie
    // ------------------------------------------------------------------
    fun validate() {
        val config = _state.value.config
        if (config.musicU.isBlank() || config.csrf.isBlank()) {
            toast("请先填写 MUSIC_U 与 __csrf")
            return
        }
        if (_state.value.busy || _state.value.validating) return
        _state.value = _state.value.copy(validating = true, accountState = "检测中…", accountOk = null)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runner.validate(config) { event -> handleEvent(event) }
            }
            _state.value = _state.value.copy(
                validating = false,
                accountState = if (result.valid) "Cookie 有效" else "Cookie 无效",
                accountOk = result.valid,
                nickname = result.profile?.nickname ?: _state.value.nickname,
                avatarUrl = result.profile?.avatarUrl ?: _state.value.avatarUrl,
            )
            toast(result.message)
        }
    }

    // ------------------------------------------------------------------
    // 运行任务
    // ------------------------------------------------------------------
    fun startTask() {
        val config = _state.value.config
        if (!config.hasCookie) {
            toast("尚未配置 Cookie，请先在「配置」中填写")
            return
        }
        if (_state.value.busy) return
        _state.value = _state.value.copy(
            busy = true,
            cancelling = false,
            stage = "正在启动…",
            dailyDone = 0,
            extraDone = 0,
            lastResult = "",
            lastSuccess = null,
        )
        appendLog("SYS", "—— 开始执行每日任务 ——")
        runJob = viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runner.run(config) { event -> handleEvent(event) }
            }
            _state.value = _state.value.copy(
                busy = false,
                cancelling = false,
                lastResult = if (result.cancelled) "任务已被用户终止" else result.summary,
                lastSuccess = result.success,
                stage = if (result.cancelled) "已终止" else if (result.success) "任务完成" else "任务失败",
            )
            if (!result.cancelled) {
                Notifier.notify(
                    getApplication(),
                    if (result.success) "每日任务完成" else "每日任务失败",
                    result.summary,
                )
            }
            refreshHistory()
        }
    }

    fun cancelTask() {
        if (!_state.value.busy) return
        runner.requestCancel()
        _state.value = _state.value.copy(cancelling = true, stage = "正在终止…")
        appendLog("SYS", "—— 收到终止请求，正在停止任务 ——")
    }

    fun refreshCookie() {
        val config = _state.value.config
        if (config.neteasePhone.isBlank() ||
            (config.neteasePassword.isBlank() && config.neteaseMd5Password.isBlank())
        ) {
            toast("请先配置手机号与密码")
            return
        }
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true, stage = "正在刷新 Cookie…")
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runner.refreshCookie(config) { event -> handleEvent(event) }
            }
            if (outcome.success && outcome.musicU != null && outcome.csrf != null) {
                updateConfig(config.copy(musicU = outcome.musicU, csrf = outcome.csrf))
            }
            _state.value = _state.value.copy(
                busy = false,
                stage = if (outcome.success) "Cookie 已刷新" else "刷新失败",
            )
            toast(outcome.message)
        }
    }

    // ------------------------------------------------------------------
    // 运行历史
    // ------------------------------------------------------------------
    fun refreshHistory() {
        viewModelScope.launch {
            val records = withContext(Dispatchers.IO) { historyStore.loadAll() }
            _state.value = _state.value.copy(history = records)
        }
    }

    fun openRecord(record: RunRecord) {
        viewModelScope.launch {
            val log = withContext(Dispatchers.IO) { historyStore.loadLog(record.id) }
            _state.value = _state.value.copy(detailRecord = record, detailLog = log)
        }
    }

    fun closeRecord() {
        _state.value = _state.value.copy(detailRecord = null, detailLog = "")
    }

    fun deleteRecord(record: RunRecord) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { historyStore.delete(record.id) }
            _state.value = _state.value.copy(detailRecord = null, detailLog = "")
            refreshHistory()
            toast("已删除该记录")
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { historyStore.clearAll() }
            refreshHistory()
            toast("已清空运行历史")
        }
    }

    // ------------------------------------------------------------------
    private fun handleEvent(event: RunEvent) {
        when (event) {
            is RunEvent.Stage -> _state.value = _state.value.copy(stage = event.text)
            is RunEvent.LogLine -> appendLog(event.level, event.message)
            is RunEvent.DailyProgress -> _state.value = _state.value.copy(
                dailyDone = event.done,
                dailyTotal = if (event.total > 0) event.total else _state.value.dailyTotal,
            )
            is RunEvent.ExtraProgress -> _state.value = _state.value.copy(
                extraDone = event.done,
                extraTotal = if (event.total > 0) event.total else _state.value.extraTotal,
            )
            is RunEvent.Account -> _state.value = _state.value.copy(nickname = event.nickname)
            is RunEvent.Finished -> {
                val result = event.result
                _state.value = _state.value.copy(
                    lastResult = if (result.cancelled) "任务已被用户终止" else result.summary,
                    lastSuccess = result.success,
                )
            }
        }
    }
}
