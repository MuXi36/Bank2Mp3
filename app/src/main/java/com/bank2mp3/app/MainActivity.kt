package com.bank2mp3.app

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
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
    private var isDarkTheme = true

    companion object {
        private const val PREFS = "bank2mp3_prefs"
        private const val KEY_DARK_THEME = "dark_theme"
    }

    private val outputDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Bank2Mp3_output")
    private val inputDir  = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Bank2Mp3_input")

    private var bankPath = ""
    private var folderPath = ""
    private var outputPath = outputDir.absolutePath

    private val bankPicker   = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { onBankPicked(it) } }
    private val folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let { onFolderPicked(it) } }
    private val outputPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let { onOutputPicked(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        isDarkTheme = getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DARK_THEME, true)
        setTheme(if (isDarkTheme) R.style.Theme_Bank2Mp3 else R.style.Theme_Bank2Mp3_Light)
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        outputDir.mkdirs(); inputDir.mkdirs()
        log("✅ 就绪 | 输出: ${outputDir.absolutePath}")
        bindUI()
        applyThemeColors()
        applyButtonAnimations()
        startTitleBreathing()
        startCardGlow()
        CoroutineScope(Dispatchers.IO).launch {
            try { PythonRuntime.init(this@MainActivity) } catch (e: Exception) { ui { log("rootfs: ${e.message}") } }
            checkBridge()
        }
    }