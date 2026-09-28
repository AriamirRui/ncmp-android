package com.ncmp.partner

import com.ncmp.partner.core.NcmpCrypto
import com.ncmp.partner.core.ScorePolicy
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 与桌面版 Python 实现的一致性测试。
 *
 * 期望值由桌面版本项目的 `signer.py` 用同一个 randomStr 生成：
 *   randomStr  = "0123456789abcdef"
 *   payload    = {"taskId": "123", "workId": "456", "score": "4", "tags": "4-A-1",
 *                 "customTags": "%5B%5D", "comment": "", "syncYunCircle": "true",
 *                 "csrf_token": "abc123"}
 */
class CryptoParityTest {

    private val randomStr = "0123456789abcdef"

    private val payload =
        """{"taskId": "123", "workId": "456", "score": "4", "tags": "4-A-1", """ +
            """"customTags": "%5B%5D", "comment": "", "syncYunCircle": "true", """ +
            """"csrf_token": "abc123"}"""

    private val expectedParams =
        "jZBgk1M/6lP8NpJq8HHUWq1kNOLcbZt+AzcGI8GkLbQ7+O4nEKkcizZGXCn96rFDA8h00z2l4tLbqGmpeq8pV" +
            "citfOFDmRB7q6ubzDtVfa4G9txG/2Bjd8vk+I7nb4P3j3nQkdmuxBO88zdJ1z/CH66dZ51ErJlaDh8UTa/1bWm" +
            "X1Q8b6QSPTUXfYvp4SZtKKdLrRkYSP4cjsNFl+p4X13EzaXDLuCIkFXoVMIVzDjAy87ELEetrwUPVsk77V7g8" +
            "foXSHDnEHc0HTMkiPd3y+bT6XLo59Ui1zmtwMXCF6zQ="

    private val expectedEncSecKey =
        "35701388baf89fed412e11269b9c76625d095ecaf17f03fa018abe19ea2d38b949debf242ee39a71ca1f6cda" +
            "71b1b86a45aa909ee27f7e78e267d34e732f0de948206c3340a788d0003372183e2f753c1f78b66ac23d13" +
            "4ac1fc9b993156520ea826b8aa89a962d4491b4b8d7e08738e1da9b07aa39bf4a7ef0b1c210728cd52"

    @Test
    fun `params 与桌面版一致`() {
        assertEquals(expectedParams, NcmpCrypto.encryptParams(payload, randomStr))
    }

    @Test
    fun `encSecKey 与桌面版一致`() {
        assertEquals(expectedEncSecKey, NcmpCrypto.encSecKey(randomStr))
    }

    @Test
    fun `encSecKey 长度固定 256 位十六进制`() {
        val key = NcmpCrypto.encSecKey(NcmpCrypto.randomString(16))
        assertEquals(256, key.length)
        assertEquals(true, key.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun `随机串为 16 位字母数字`() {
        val s = NcmpCrypto.randomString(16)
        assertEquals(16, s.length)
        assertEquals(true, s.all { it.isLetterOrDigit() })
    }
}

/** 评分策略一致性（对应桌面版 Signer._get_score_and_tag） */
class ScorePolicyTest {

    @Test
    fun `策略3-默认-含英文取4分`() {
        assertEquals("4" to "4-A-1", ScorePolicy.scoreAndTag("Hello World", "Adele", 3))
    }

    @Test
    fun `策略3-默认-纯中文取3分`() {
        assertEquals("3" to "3-A-1", ScorePolicy.scoreAndTag("晴天", "周杰伦", 3))
    }

    @Test
    fun `策略1-区间1到2`() {
        assertEquals("1" to "1-A-1", ScorePolicy.scoreAndTag("晴天", "周杰伦", 1))
        assertEquals("2" to "2-A-1", ScorePolicy.scoreAndTag("Lemon", "米津玄師", 1))
    }

    @Test
    fun `策略2-区间2到3`() {
        assertEquals("2" to "2-A-1", ScorePolicy.scoreAndTag("晴天", "周杰伦", 2))
        assertEquals("3" to "3-A-1", ScorePolicy.scoreAndTag("Lemon", "米津玄師", 2))
    }

    @Test
    fun `策略4-固定4分`() {
        assertEquals("4" to "4-A-1", ScorePolicy.scoreAndTag("晴天", "周杰伦", 4))
        assertEquals("4" to "4-A-1", ScorePolicy.scoreAndTag("Lemon", "米津玄師", 4))
    }

    @Test
    fun `英文出现在歌手名同样生效`() {
        assertEquals("4" to "4-A-1", ScorePolicy.scoreAndTag("晴天", "Jay Chou", 3))
    }
}
