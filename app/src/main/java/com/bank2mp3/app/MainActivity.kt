package com.bank2mp3.app

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bank2mp3.app.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private var job: Job? = null
    private var bridgeAlive = false

    private val outputDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Bank2Mp3_output")
    private val inputDir  = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Bank2Mp3_input")

    private var bankPath = ""
    private var folderPath = ""
    private var outputPath = outputDir.absolutePath

    private val bankPicker   = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { onBankPicked(it) } }
    private val folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let { onFolderPicked(it) } }
    private val outputPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let { onOutputPicked(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        outputDir.mkdirs(); inputDir.mkdirs()
        log("✅ 就绪 | 输出: ${outputDir.absolutePath}")
        bindUI()
        CoroutineScope(Dispatchers.IO).launch {
            try { PythonRuntime.init(this@MainActivity) } catch (e: Exception) { ui { log("rootfs: ${e.message}") } }
            checkBridge()
        }
    }

    private fun bindUI() {
        b.btnPickFile.setOnClickListener   { bankPicker.launch(arrayOf("*\/*")) }
        b.btnPickFolder.setOnClickListener { folderPicker.launch(null) }
        b.btnPickOutput.setOnClickListener { outputPicker.launch(null) }
        b.btnResetOutput.setOnClickListener {
            outputPath = outputDir.absolutePath; b.tvOutputDir.text = "Download/Bank2Mp3_output"; log("输出目录已重置")
        }

        b.btnBridgeRefresh.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch { checkBridge() }
        }
        b.btnBridgeStart.setOnClickListener {
            b.btnBridgeStart.isEnabled = false; b.bridgeStatus.text = "⏳ 启动中..."
            CoroutineScope(Dispatchers.IO).launch {
                val msg = BridgeStarter.start(this@MainActivity)
                delay(2000)
                ui { b.btnBridgeStart.isEnabled = true; log(msg) }
                checkBridge()
            }
        }

        b.btnTerminalConvert.setOnClickListener  { if (bankPath.isNotEmpty()) terminalConvertSingle(bankPath) else toast("请先选文件") }
        b.btnTerminalBatch.setOnClickListener    { if (folderPath.isNotEmpty()) terminalBatch(folderPath, false) else toast("请先选目录") }
        b.btnTerminalBatchMp3.setOnClickListener { if (folderPath.isNotEmpty()) terminalBatch(folderPath, true) else toast("请先选目录") }
        b.btnTerminalClassify.setOnClickListener { if (folderPath.isNotEmpty()) terminalBatch(folderPath, true) else toast("请先选目录") }

        b.btnWavToMp3.setOnClickListener   { wavConvert("mp3", "192k", "MP3 192k") }
        b.btnWavToMp3HQ.setOnClickListener { wavConvert("mp3", "320k", "MP3 320k") }
        b.btnWavToAac.setOnClickListener   { wavConvert("aac", "192k", "AAC M4A") }
        b.btnWavToFlac.setOnClickListener  { wavConvert("flac", "0", "FLAC") }
        b.btnWavToOgg.setOnClickListener   { wavConvert("ogg", "6", "OGG") }
        b.btnWavToOpus.setOnClickListener  { wavConvert("opus", "128k", "OPUS") }

        b.btnCopyLog.setOnClickListener {
            val cm = getSystemService(android.content.ClipboardManager::class.java)
            cm.setPrimaryClip(android.content.ClipData.newPlainText("log", b.tvLog.text?.toString() ?: "")); toast("已复制")
        }
    }

    private fun setBtns(on: Boolean) {
        b.btnTerminalConvert.isEnabled  = on && bankPath.isNotEmpty()
        b.btnTerminalBatch.isEnabled    = on
        b.btnTerminalBatchMp3.isEnabled = on
        b.btnTerminalClassify.isEnabled = on
        b.btnWavToMp3.isEnabled = on; b.btnWavToMp3HQ.isEnabled = on
        b.btnWavToAac.isEnabled = on; b.btnWavToFlac.isEnabled = on
        b.btnWavToOgg.isEnabled = on; b.btnWavToOpus.isEnabled = on
    }

    private suspend fun checkBridge() {
        val ok = TerminalBridge.checkHealth()
        bridgeAlive = ok
        ui {
            if (ok) {
                b.bridgeStatus.text = "🟢 终端桥接已连接"; b.bridgeStatus.setTextColor(0xFF27AE60.toInt()); setBtns(true)
            } else {
                b.bridgeStatus.text = "🔴 未连接 — 点 🚀启动"; b.bridgeStatus.setTextColor(0xFFE74C3C.toInt()); setBtns(false)
            }
        }
    }

    private fun onBankPicked(uri: Uri) {
        resolveFilePath(uri)?.let {
            bankPath = it; b.tvFile.text = File(it).name
            b.btnTerminalConvert.isEnabled = bridgeAlive; log("已选择: ${File(it).name}")
            return
        }
        try {
            val name = getDisplayName(uri) ?: "input.bank"
            val tmp = File(cacheDir, name)
            contentResolver.openInputStream(uri)?.use { s -> tmp.outputStream().use { d -> s.copyTo(d) } }
            bankPath = tmp.absolutePath; b.tvFile.text = name
            b.btnTerminalConvert.isEnabled = bridgeAlive; log("已选择(缓存): $name")
        } catch (e: Exception) { log("✗ ${e.message}") }
    }
    private fun onFolderPicked(uri: Uri) {
        folderPath = resolveTreePath(uri) ?: uri.toString()
        b.tvFile.text = if (folderPath.startsWith("/")) File(folderPath).name else "已选目录"
        log("目录: $folderPath")
    }
    private fun onOutputPicked(uri: Uri) {
        resolveTreePath(uri)?.let { outputPath = it; b.tvOutputDir.text = it; File(it).mkdirs(); log("输出: $it") }
    }

    private fun terminalConvertSingle(path: String) {
        if (!bridgeAlive) { toast("桥接未连接"); return }
        job?.cancel(); job = CoroutineScope(Dispatchers.IO).launch {
            val sharedPath = if (path.startsWith(cacheDir.absolutePath) || path.startsWith(filesDir.absolutePath)) {
                val s = File(inputDir, File(path).name); File(path).copyTo(s, overwrite = true)
                ui { log("缓存 → ${s.absolutePath}") }; s.absolutePath
            } else path
            ui { showProgress(true); log("⚡ 终端: Bank → WAV...") }
            val r = TerminalBridge.convertSingle(sharedPath, outputPath)
            ui { showProgress(false); for (l in r.stdout.lines().takeLast(40)) log(l); log(if (r.ok) "✅ 完成" else "✗ ${r.error}") }
        }
    }
    private fun terminalBatch(path: String, mp3: Boolean) {
        if (!bridgeAlive) { toast("桥接未连接"); return }
        job?.cancel(); job = CoroutineScope(Dispatchers.IO).launch {
            ui { showProgress(true); log("⚡ 批量: Bank → ${if (mp3) "MP3" else "WAV"}...") }
            val r = TerminalBridge.batchConvert(path, outputPath, mp3)
            ui { showProgress(false); for (l in r.stdout.lines().takeLast(30)) log(l); log(if (r.ok) "✅ 批量完成" else "✗ ${r.error}") }
        }
    }
    private fun wavConvert(format: String, bitrate: String, label: String) {
        if (!bridgeAlive) { toast("桥接未连接"); return }
        job?.cancel(); job = CoroutineScope(Dispatchers.IO).launch {
            ui { showProgress(true); log("⬆ WAV → $label...") }
            val r = TerminalBridge.wavConvert(outputPath, format, bitrate)
            ui { showProgress(false); for (l in r.stdout.lines().takeLast(20)) log(l); log(if (r.ok) "✅ $label 完成" else "✗ ${r.error}") }
        }
    }

    private fun resolveFilePath(uri: Uri): String? {
        try {
            contentResolver.query(uri, arrayOf(android.provider.MediaStore.MediaColumns.DATA), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(c.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA))?.takeIf { File(it).exists() }?.let { return it }
            }
            if (DocumentsContract.isDocumentUri(this, uri)) {
                val docId = DocumentsContract.getDocumentId(uri); val i = docId.indexOf(':')
                if (i >= 0) { val p = "/storage/emulated/0/${docId.substring(i + 1)}"; if (File(p).exists()) return p }
            }
        } catch (_: Exception) {}
        return null
    }
    private fun resolveTreePath(uri: Uri) = try {
        val docId = DocumentsContract.getTreeDocumentId(uri); val i = docId.indexOf(':')
        if (i >= 0) "/storage/emulated/0/${docId.substring(i + 1)}".also { File(it).mkdirs() } else null
    } catch (_: Exception) { null }
    private fun getDisplayName(uri: Uri) = try {
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)) else null
        }
    } catch (_: Exception) { null }

    private fun showProgress(on: Boolean) { b.progressContainer.visibility = if (on) View.VISIBLE else View.GONE }
    private fun log(msg: String) { runOnUiThread { b.tvLog.text = ((b.tvLog.text?.toString() ?: "") + "\n" + msg).lines().takeLast(150).joinToString("\n") } }
    private suspend fun ui(block: () -> Unit) = withContext(Dispatchers.Main) { block() }
    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
