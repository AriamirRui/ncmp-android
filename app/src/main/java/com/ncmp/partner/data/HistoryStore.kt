package com.ncmp.partner.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** 一次运行的记录 */
data class RunRecord(
    val id: String,
    val time: String,
    val kind: String,
    val success: Boolean,
    val summary: String,
) {
    val cancelled: Boolean get() = !success && summary == CANCELLED_SUMMARY

    companion object {
        const val CANCELLED_SUMMARY = "任务已被用户终止"
    }
}

/** 运行历史与日志读写（对应桌面版 data/history/） */
class HistoryStore(private val context: Context) {

    private val dir: File get() = File(context.filesDir, "history").apply { if (!exists()) mkdirs() }

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val idFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun newId(): String = idFormat.format(Date()) + "_" + UUID.randomUUID().toString().take(6)

    fun nowText(): String = timeFormat.format(Date())

    private fun metaFile(id: String) = File(dir, "$id.json")
    private fun logFile(id: String) = File(dir, "$id.log")

    fun save(record: RunRecord) {
        val json = JSONObject().apply {
            put("id", record.id)
            put("time", record.time)
            put("kind", record.kind)
            put("success", record.success)
            put("summary", record.summary)
        }
        metaFile(record.id).writeText(json.toString(2), Charsets.UTF_8)
    }

    fun appendLog(id: String, line: String) {
        try {
            logFile(id).appendText(line + "\n", Charsets.UTF_8)
        } catch (_: Exception) {
        }
    }

    fun loadLog(id: String): String = try {
        val f = logFile(id)
        if (f.exists()) f.readText(Charsets.UTF_8) else ""
    } catch (e: Exception) {
        ""
    }

    fun loadAll(): List<RunRecord> {
        val files = dir.listFiles { f -> f.name.endsWith(".json") } ?: return emptyList()
        return files.mapNotNull { f ->
            try {
                val json = JSONObject(f.readText(Charsets.UTF_8))
                RunRecord(
                    id = json.optString("id", f.nameWithoutExtension),
                    time = json.optString("time", ""),
                    kind = json.optString("kind", "任务运行"),
                    success = json.optBoolean("success", false),
                    summary = json.optString("summary", ""),
                )
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.id }
    }

    fun delete(id: String) {
        metaFile(id).delete()
        logFile(id).delete()
    }

    fun clearAll() {
        dir.listFiles()?.forEach { it.delete() }
    }
}
