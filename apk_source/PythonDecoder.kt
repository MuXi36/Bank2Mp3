package com.bank2mp3.app

import android.content.Context
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * 自给自足 Python 解码器 — 使用内置 rootfs 的 ld-linux + python3 + decode.py
 */
class PythonDecoder(private val context: Context, private val onLog: (String) -> Unit) {

    data class Result(val outputFiles: List<String>, val log: List<String>)

    fun convert(bankPath: String, outputDir: File): Result {
        val logLines = mutableListOf<String>()
        fun log(msg: String) { logLines.add(msg); onLog(msg) }

        if (!PythonRuntime.init(context)) {
            log("运行时初始化失败")
            return Result(emptyList(), logLines)
        }
        log("启动内置终端...")
        log("引擎: Python 3.12 + fsb5 + ffmpeg")

        try {
            val cmd = PythonRuntime.buildCommand(
                "decode.py", bankPath, outputDir.absolutePath
            )
            log("运行: ${cmd.takeLast(3).joinToString(" ")}")

            val env = mapOf(
                "HOME" to PythonRuntime.getRootfsDir().absolutePath,
                "PATH" to "/usr/bin:/bin",
                "LD_LIBRARY_PATH" to PythonRuntime.getLibPath(),
                "PYTHONPATH" to PythonRuntime.getScriptsDir().absolutePath
            )

            val pb = ProcessBuilder(*cmd)
            pb.directory(PythonRuntime.getRootfsDir())
            pb.environment().putAll(env)
            pb.redirectErrorStream(true)

            val proc = pb.start()
            val reader = BufferedReader(InputStreamReader(proc.inputStream))
            val outputFiles = mutableListOf<String>()

            reader.useLines { lines ->
                lines.forEach { line ->
                    log(line)
                    if (line.startsWith("OK:")) {
                        outputFiles.add(line.removePrefix("OK:"))
                    }
                }
            }

            proc.waitFor()
            log("完成: ${outputFiles.size} 个文件")
            return Result(outputFiles, logLines)

        } catch (e: Exception) {
            log("错误: ${e.message}")
            return Result(emptyList(), logLines)
        }
    }

    fun batchConvert(bankDir: String, outputDir: File, toMp3: Boolean): Result {
        val logLines = mutableListOf<String>()
        fun log(msg: String) { logLines.add(msg); onLog(msg) }

        if (!PythonRuntime.init(context)) {
            log("运行时初始化失败")
            return Result(emptyList(), logLines)
        }
        log("批量转换: $bankDir")

        try {
            val bankFiles = File(bankDir).listFiles { f ->
                f.extension == "bank" && (f.name.contains(".assets.bank") || f.name.contains(".streams.bank"))
            } ?: emptyArray()

            val total = bankFiles.size
            log("找到 $total 个音频 bank")
            val allFiles = mutableListOf<String>()

            for (i in bankFiles.indices) {
                val bank = bankFiles[i]
                log("[${i + 1}/$total] ${bank.name}")
                val result = convert(bank.absolutePath, outputDir)
                allFiles.addAll(result.outputFiles)
            }

            log("全部完成: ${allFiles.size} 个文件")
            return Result(allFiles, logLines)

        } catch (e: Exception) {
            log("错误: ${e.message}")
            return Result(emptyList(), logLines)
        }
    }
}