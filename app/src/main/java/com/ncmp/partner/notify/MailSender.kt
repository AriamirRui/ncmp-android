package com.ncmp.partner.notify

import android.util.Base64
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.SNIHostName

/**
 * 极简 SMTP 客户端（SSL / STARTTLS），对应桌面版 `src/utils/notification.py`。
 * 仅依赖 JDK 自带的 javax.net.ssl，无需引入 JavaMail。
 */
object MailSender {

    fun send(
        host: String,
        port: Int,
        user: String,
        password: String,
        to: String,
        subject: String,
        body: String,
    ): Boolean {
        return try {
            if (port == 465) sendSsl(host, port, user, password, to, subject, body)
            else sendStartTls(host, port, user, password, to, subject, body)
        } catch (e: Exception) {
            false
        }
    }

    // ------------------------------------------------------------------
    private fun sendSsl(host: String, port: Int, user: String, password: String,
                        to: String, subject: String, body: String): Boolean {
        val socket = SSLSocketFactory.getDefault().createSocket() as SSLSocket
        socket.connect(InetSocketAddress(host, port), 15000)
        applySni(socket, host)
        socket.startHandshake()
        return conversation(socket, host, user, password, to, subject, body)
    }

    private fun sendStartTls(host: String, port: Int, user: String, password: String,
                             to: String, subject: String, body: String): Boolean {
        val plain = java.net.Socket()
        plain.connect(InetSocketAddress(host, port), 15000)
        plain.soTimeout = 20000
        val reader = BufferedReader(InputStreamReader(plain.getInputStream(), Charsets.US_ASCII))
        val writer = BufferedWriter(OutputStreamWriter(plain.getOutputStream(), Charsets.US_ASCII))
        try {
            expect(reader, 220)
            command(writer, reader, "EHLO ncmp.android", 250)
            command(writer, reader, "STARTTLS", 220)
            val factory = SSLContext.getDefault().socketFactory
            val ssl = factory.createSocket(plain, host, port, true) as SSLSocket
            applySni(ssl, host)
            ssl.startHandshake()
            return conversation(ssl, host, user, password, to, subject, body)
        } finally {
            try { plain.close() } catch (_: Exception) {}
        }
    }

    private fun applySni(socket: SSLSocket, host: String) {
        try {
            val params = socket.sslParameters
            params.serverNames = listOf(SNIHostName(host))
            socket.sslParameters = params
        } catch (_: Exception) {
        }
    }

    private fun conversation(socket: SSLSocket, host: String, user: String, password: String,
                             to: String, subject: String, body: String): Boolean {
        socket.soTimeout = 20000
        val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.US_ASCII))
        val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.US_ASCII))
        try {
            expect(reader, 220)
            command(writer, reader, "EHLO ncmp.android", 250)
            // AUTH LOGIN
            command(writer, reader, "AUTH LOGIN", 334)
            command(writer, reader, base64(user), 334)
            command(writer, reader, base64(password), 235)

            command(writer, reader, "MAIL FROM:<$user>", 250)
            command(writer, reader, "RCPT TO:<$to>", 250)
            command(writer, reader, "DATA", 354)

            val message = buildMessage(user, to, subject, body)
            writer.write(message)
            writer.write("\r\n.\r\n")
            writer.flush()
            expect(reader, 250, allowAny = true)
            try { command(writer, reader, "QUIT", 221) } catch (_: Exception) {}
            return true
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun buildMessage(from: String, to: String, subject: String, body: String): String {
        val encodedSubject = "=?UTF-8?B?" + base64(subject) + "?="
        val encodedBody = base64(body)
        return buildString {
            append("From: ").append(from).append("\r\n")
            append("To: ").append(to).append("\r\n")
            append("Subject: ").append(encodedSubject).append("\r\n")
            append("MIME-Version: 1.0\r\n")
            append("Content-Type: text/plain; charset=UTF-8\r\n")
            append("Content-Transfer-Encoding: base64\r\n")
            append("\r\n")
            // base64 正文按 76 列换行
            encodedBody.chunked(76).forEach { append(it).append("\r\n") }
        }
    }

    private fun base64(text: String): String =
        Base64.encodeToString(text.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    private fun command(writer: BufferedWriter, reader: BufferedReader,
                        line: String, expectCode: Int): String {
        writer.write(line)
        writer.write("\r\n")
        writer.flush()
        return expect(reader, expectCode)
    }

    /** 读取 SMTP 响应（支持多行 "250-xxx"），校验状态码 */
    private fun expect(reader: BufferedReader, code: Int, allowAny: Boolean = false): String {
        val sb = StringBuilder()
        while (true) {
            val line = reader.readLine() ?: break
            sb.append(line).append('\n')
            if (line.length >= 4 && line[3] == '-') continue
            val actual = line.take(3).toIntOrNull()
            if (!allowAny && actual != code) {
                throw IllegalStateException("SMTP 期望 $code 实际 $line")
            }
            break
        }
        return sb.toString()
    }
}
