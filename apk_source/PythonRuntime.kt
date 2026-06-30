package com.bank2mp3.app

import android.content.Context
import java.io.File
import java.io.FileOutputStream

object PythonRuntime {
    private var ready = false
    private lateinit var rootfsDir: File
    private lateinit var ldPath: String
    private lateinit var pythonBin: String
    private lateinit var libPath: String
    private lateinit var scriptsDir: File

    private const val VERSION = 10

    fun init(context: Context): Boolean {
        if (ready) return true
        rootfsDir = File(context.filesDir, "rootfs")
        scriptsDir = File(rootfsDir, "scripts")
        val versionFile = File(rootfsDir, ".version")

        if (!versionFile.exists() || versionFile.readText().trim() != VERSION.toString()) {
            if (rootfsDir.exists()) rootfsDir.deleteRecursively()
            rootfsDir.mkdirs()
            scriptsDir.mkdirs()
            try {
                extractRootfs(context)
                copyFsb5(context)
                versionFile.writeText(VERSION.toString())
            } catch (e: Exception) {
                rootfsDir.deleteRecursively()
                throw RuntimeException("init: ${e.message}", e)
            }
        }

        ldPath = File(rootfsDir, "lib/ld-linux-aarch64.so.1").absolutePath
        pythonBin = File(rootfsDir, "bin/python3.12").absolutePath
        libPath = File(rootfsDir, "lib").absolutePath
        ready = true
        return true
    }

    private fun extractRootfs(context: Context) {
        context.assets.open("rootfs.dat").use { input ->
            java.util.zip.GZIPInputStream(input).use { gz ->
                val data = gz.readBytes()
                var pos = 0
                while (pos < data.size - 512) {
                    val name = String(data, pos, 512).takeWhile { it != '\u0000' }
                    if (name.isEmpty()) break
                    val sizeStr = String(data, pos + 124, 12).takeWhile { it != '\u0000' && it != ' ' }
                    val size = sizeStr.toLongOrNull(8) ?: 0
                    val type = data[pos + 156].toInt() and 0xFF
                    pos += 512
                    if (name.endsWith("/") || type == '5'.code) {
                        File(rootfsDir, name).mkdirs()
                    } else if (size > 0 && pos + size <= data.size) {
                        val target = File(rootfsDir, name)
                        target.parentFile?.mkdirs()
                        FileOutputStream(target).use { fos -> fos.write(data, pos, size.toInt()) }
                        if (name.startsWith("bin/") || name.startsWith("lib/ld-"))
                            target.setExecutable(true, false)
                    }
                    pos += ((size + 511) / 512 * 512).toInt()
                }
            }
        }
    }

    private fun copyFsb5(context: Context) {
        val fsb5Dir = File(scriptsDir, "fsb5")
        fsb5Dir.mkdirs()
        for (f in listOf("__init__.py", "utils.py", "vorbis.py", "vorbis_headers.py", "pcm.py")) {
            context.assets.open("Bank2Mp3/scripts/fsb5/$f").use { src ->
                File(fsb5Dir, f).outputStream().use { dst -> src.copyTo(dst) }
            }
        }
        // 同时复制 decode.py
        context.assets.open("Bank2Mp3/scripts/decode.py").use { src ->
            File(scriptsDir, "decode.py").outputStream().use { dst -> src.copyTo(dst) }
        }
    }

    fun isReady() = ready
    fun getLdPath() = ldPath
    fun getPythonBin() = pythonBin
    fun getLibPath() = libPath
    fun getScriptsDir() = scriptsDir
    fun getRootfsDir() = rootfsDir

    fun buildCommand(script: String, vararg args: String): Array<String> {
        return arrayOf(ldPath, "--library-path", libPath, pythonBin, File(scriptsDir, script).absolutePath, *args)
    }
}