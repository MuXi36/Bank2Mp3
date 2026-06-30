package com.bank2mp3.app
import android.content.Context

object BridgeStarter {
    fun start(context: Context): String = try {
        if (!PythonRuntime.isReady()) PythonRuntime.init(context)
        val cmd = PythonRuntime.buildCommand("terminal_server.py")
        val pb = ProcessBuilder(*cmd)
            .directory(PythonRuntime.getScriptsDir())
            .redirectErrorStream(true)
        pb.start()
        Thread.sleep(2500)
        if (TerminalBridge.checkHealth()) "启动成功 🟢" else "启动超时，请点🔄刷新"
    } catch (e: Exception) { "启动失败: ${e.message}" }
}