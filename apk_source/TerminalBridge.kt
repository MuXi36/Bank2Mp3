package com.bank2mp3.app
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object TerminalBridge {
    private const val BASE = "http://127.0.0.1:8899"
    data class Response(val ok: Boolean, val code: Int=0, val stdout: String="", val stderr: String="", val error: String="", val wavCount: Int=0)
    fun checkHealth() = try { (URL("$BASE/health").openConnection() as HttpURLConnection).apply { connectTimeout=2000; readTimeout=2000; requestMethod="GET" }.responseCode == 200 } catch (_: Exception) { false }
    suspend fun convertSingle(bankPath: String, outputDir: String): Response = withContext(Dispatchers.IO) {
        post(JSONObject().apply { put("command", "python3 /storage/emulated/0/Download/Bank2Mp3/scripts/decode.py \"$bankPath\" \"$outputDir\""); put("timeout", 300) })
    }
    suspend fun batchConvert(bankDir: String, outputDir: String, toMp3: Boolean, classify: Boolean=false): Response = withContext(Dispatchers.IO) {
        post(JSONObject().apply { put("type","batch"); put("bank_dir",bankDir); put("output",outputDir); put("mp3",toMp3); put("classify",classify) }, 600)
    }
    suspend fun wavConvert(wavDir: String, format: String, bitrate: String): Response = withContext(Dispatchers.IO) {
        val ext = if (format=="aac") "m4a" else format
        val codec = when(format) { "mp3"->"-codec:a libmp3lame -b:a $bitrate"; "aac"->"-codec:a aac -b:a $bitrate"; "flac"->"-codec:a flac"; "ogg"->"-codec:a libvorbis -q:a $bitrate"; "opus"->"-codec:a libopus -b:a $bitrate"; else->"-codec:a libmp3lame -b:a 192k" }
        post(JSONObject().apply { put("command", "cd \"$wavDir\" && for f in *.wav; do ffmpeg -y -loglevel error -i \"\$f\" $codec \"\${f%.wav}.$ext\" 2>&1; done"); put("timeout", 600) })
    }
    private fun post(body: JSONObject, timeout: Int=300): Response = try {
        val conn = (URL("$BASE/exec").openConnection() as HttpURLConnection).apply { connectTimeout=2000; readTimeout=timeout*1000; requestMethod="POST"; setRequestProperty("Content-Type","application/json"); doOutput=true }
        conn.outputStream.write(body.toString().toByteArray())
        val json = JSONObject(conn.inputStream.bufferedReader().readText())
        Response(json.optBoolean("ok"), json.optInt("code"), json.optString("stdout",""), json.optString("stderr",""), json.optString("error",""), json.optInt("wav_count"))
    } catch (e: Exception) { Response(false, error=e.message?:"unknown") }
}
