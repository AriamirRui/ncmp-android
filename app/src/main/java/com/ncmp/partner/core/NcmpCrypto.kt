package com.ncmp.partner.core

import java.math.BigInteger
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 网易云 weapi 加密，与桌面版 `src/core/signer.py` 完全一致：
 *
 * ```
 * params    = AES-CBC(AES-CBC(json, "0CoJUm6Qyw8W8jud"), randomStr)
 * encSecKey = RSA(reverse(randomStr), 0x010001, modulus)   // 十六进制、左补零到 256 位
 * ```
 *
 * 其中 randomStr 为会话级随机串（每次运行生成一次，与桌面版一致）。
 */
object NcmpCrypto {

    private const val IV = "0102030405060708"
    private const val AES_KEY = "0CoJUm6Qyw8W8jud"
    private const val PUB_KEY = "010001"
    private const val MODULUS =
        "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d2e4176" +
        "29ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b" +
        "424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7"

    private const val CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    private val random = SecureRandom()

    /** 生成随机字符串（桌面版为大小写字母 + 数字，长度 16） */
    fun randomString(length: Int = 16): String {
        val sb = StringBuilder(length)
        repeat(length) { sb.append(CHARS[random.nextInt(CHARS.length)]) }
        return sb.toString()
    }

    /** PKCS#7 填充（桌面版 _add_to_16：不足 16 倍数时补足整块） */
    private fun pkcs7Pad(data: ByteArray): ByteArray {
        val pad = 16 - data.size % 16
        val out = data.copyOf(data.size + pad)
        for (i in data.size until out.size) out[i] = pad.toByte()
        return out
    }

    private fun aesEncrypt(text: String, key: String): String {
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES"),
            IvParameterSpec(IV.toByteArray(Charsets.UTF_8))
        )
        val encrypted = cipher.doFinal(pkcs7Pad(text.toByteArray(Charsets.UTF_8)))
        return Base64.getEncoder().encodeToString(encrypted)
    }

    /** 双重 AES 加密，返回 params 字段 */
    fun encryptParams(json: String, randomStr: String): String =
        aesEncrypt(aesEncrypt(json, AES_KEY), randomStr)

    /** 生成 encSecKey 字段 */
    fun encSecKey(randomStr: String): String {
        val reversed = randomStr.reversed()
        val hex = reversed.toByteArray(Charsets.UTF_8).joinToString("") { "%02x".format(it) }
        val base = BigInteger(hex, 16)
        val result = base.modPow(BigInteger(PUB_KEY, 16), BigInteger(MODULUS, 16))
        return result.toString(16).padStart(256, '0')
    }
}
