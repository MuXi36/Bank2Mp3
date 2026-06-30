package com.bank2mp3.app

import android.content.Context
import java.io.File
import java.io.FileOutputStream

object BridgeStarter {
    fun start(context: Context): String = try {
        val r = Runtime.getRuntime().exec(arrayOf("sh", "-c", "pkill -f terminal_server.py 2>/dev/null; nohup python3 /storage/emulated/0/Download/Bank2Mp3/scripts/terminal_server.py > /dev/null 2>&1 &"))
        r.waitFor()
        Thread.sleep(2000)
        if (TerminalBridge.checkHealth()) "启动成功 🟢" else "启动超时，请点🔄刷新"
    } catch (e: Exception) { "启动失败: ${e.message}" }
}