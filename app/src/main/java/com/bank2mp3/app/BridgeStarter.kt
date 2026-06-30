package com.bank2mp3.app
import android.content.Context

object BridgeStarter {
    fun start(context: Context): String {
        // Android W^X 阻止从 /data 执行二进制，桥接必须在 Operit 终端启动。
        // 用户需在 Operit 终端执行: python3 /sdcard/Download/Bank2Mp3/scripts/terminal_server.py
        return if (TerminalBridge.checkHealth()) "已连接 🟢"
            else "请在 Operit 终端启动: python3 /sdcard/Download/Bank2Mp3/scripts/terminal_server.py"
    }
}