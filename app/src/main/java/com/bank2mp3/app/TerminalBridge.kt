package com.bank2mp3.app

import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

object TerminalBridge {
    private const val BASE = "http://127.0.0.1:8899"
    
    suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = URL("$BASE/health").openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.connect()
            conn.responseCode == 200
        } catch (e: Exception) { false }
    }
    
    suspend fun exec(command: String): String = withContext(Dispatchers.IO) {
        post("exec", JSONObject().apply { put("command", command) })
    }
    
    suspend fun batchConvert(bankDir: String, outputDir: String, format: String = "wav", mp3Bitrate: String = "192k", classify: Boolean = false): String = withContext(Dispatchers.IO) {
        post("batch", JSONObject().apply {
            put("bank_dir", bankDir)
            put("output_dir", outputDir)
            put("format", format)
            put("mp3_bitrate", mp3Bitrate)
            put("classify", classify)
        })
    }
    
    suspend fun wav2Format(wavDir: String, outputDir: String, format: String = "mp3", bitrate: String = "192k"): String = withContext(Dispatchers.IO) {
        post("wav2mp3", JSONObject().apply {
            put("wav_dir", wavDir)
            put("output_dir", outputDir)
            put("format", format)
            put("mp3_bitrate", bitrate)
        })
    }
    
    private fun post(endpoint: String, json: JSONObject): String {
        val url = URL("$BASE/$endpoint")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 300000
        conn.readTimeout = 300000
        conn.setRequestProperty("Content-Type", "application/json")
        conn.outputStream.write(json.toString().toByteArray())
        conn.outputStream.flush()
        
        return if (conn.responseCode == 200) {
            BufferedReader(InputStreamReader(conn.inputStream)).readText()
        } else {
            "{\"ok\":false,\"error\":\"HTTP ${conn.responseCode}\"}"
        }
    }
}
