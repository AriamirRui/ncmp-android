package com.ncmp.partner.core

/**
 * 评分策略（与桌面版 `Signer._get_score_and_tag` 一致）：
 * 名称或歌手包含英文字母时取区间上限，否则取下限。
 */
object ScorePolicy {

    private val englishPattern = Regex("[a-zA-Z]")

    fun scoreAndTag(name: String, authorName: String, strategy: Int): Pair<String, String> {
        val hasEnglish = englishPattern.containsMatchIn(name + authorName)
        val score = when (strategy) {
            1 -> if (hasEnglish) "2" else "1"
            2 -> if (hasEnglish) "3" else "2"
            3 -> if (hasEnglish) "4" else "3"
            else -> "4"
        }
        return score to "$score-A-1"
    }
}
