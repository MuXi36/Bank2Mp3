# 🎵 Bank2Mp3 — FMOD `.bank` Audio Extractor

> Android Native App · Terminal Bridge · Van Gogh Themed Motion  
> Extract audio from game `.bank` files, batch transcode to MP3/AAC/FLAC/OGG/OPUS

<details>
<summary>📖 中文版 / Chinese (点击展开)</summary>

> Android 原生应用 · 终端桥接架构 · 梵高主题动效  
> 从游戏 `.bank` 文件中提取音频，批量转码为 MP3/AAC/FLAC/OGG/OPUS

</details>

[![Android](https://img.shields.io/badge/Android-8.0%2B-green?logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue?logo=kotlin)](https://kotlinlang.org)
[![Python](https://img.shields.io/badge/Python-3.8%2B-yellow?logo=python)](https://python.org)
[![Min SDK 26](https://img.shields.io/badge/minSdk-26-orange)](https://apilevels.com)

---

## ✨ Features · 特性

<details>
<summary>中文</summary>

| 功能 | 说明 |
|------|------|
| 🔍 **.bank 解析** | 基于 FMOD API 提取音频流，输出无损 WAV |
| 🔄 **多格式转码** | MP3 (192k/320k) · AAC · FLAC · OGG · OPUS (via FFmpeg) |
| 📦 **批量处理** | 整个目录批量转换，支持递归扫描 |
| 🏷️ **中文分类** | 按音频中文名自动归类到目录 |
| 🌐 **终端桥接** | Python HTTP 服务 → APK 通过 localhost:8899 通信 |
| 🎨 **双主题动态背景** | 暗色·梵高《星月夜》星空 / 亮色·油画布肌理 |
| 🌟 **动态粒子效果** | 200+ 闪烁星空、12 颗呼吸光斑、漩涡弧线 |
| 📋 **日志折叠** | 点击标题栏折叠/展开运行日志 |
| ⏱️ **工作流守护** | Operit 工作流每分钟自动保活桥接服务 |
| 🐔 **坤坤彩蛋** | 致敬经典 |

</details>

| Feature | Description |
|---------|-------------|
| 🔍 **.bank Parsing** | FMOD API audio extraction → lossless WAV |
| 🔄 **Multi-format** | MP3 (192k/320k) · AAC · FLAC · OGG · OPUS via FFmpeg |
| 📦 **Batch Processing** | Directory-wide conversion with recursive scanning |
| 🏷️ **Chinese Classify** | Auto-sort by Chinese audio name into folders |
| 🌐 **Terminal Bridge** | Python HTTP server ↔ APK via `localhost:8899` |
| 🎨 **Dual Themes** | Dark·Van Gogh Starry Night / Light·Oil Canvas |
| 🌟 **Particle Effects** | 200+ twinkling stars, 12 glowing halos, swirl arcs |
| 📋 **Collapsible Log** | Tap title bar to fold/unfold the log panel |
| ⏱️ **Workflow Guardian** | Operit workflow keeps bridge alive every minute |
| 🐔 **Easter Egg** | A classic tribute ❤️ |

---

## 🖼️ Preview · 预览

| Dark · Starry Night | Light · Oil Canvas |
|:---:|:---:|
| Deep blue gradient + twinkling stars + swirl arcs + 12 glowing halos | Warm cream + linen weave + golden brush strokes + soft light spots |

> Backgrounds are Canvas-rendered at runtime — no static images. Stars breathe independently via sine-wave (100ms/frame).  
> 背景基于 Canvas 实时绘制，非图片素材。星空粒子以正弦波独立呼吸闪烁（100ms/帧）。

---

## 🏗️ Architecture · 架构

```
┌─────────────────────────────────────────┐
│              Android APK                 │
│  ┌───────────────────────────────────┐  │
│  │     MainActivity (Kotlin)          │  │
│  │  • UI / Effects / Themes          │  │
│  │  • OkHttp → localhost:8899        │  │
│  └──────────────┬────────────────────┘  │
│                 │ HTTP POST /exec        │
│                 │       /batch           │
│                 │       /wav2mp3         │
│  ┌──────────────▼────────────────────┐  │
│  │  terminal_server.py (Python)      │  │
│  │  • FMOD parsing · FFmpeg encode  │  │
│  │  • Batch · Classification         │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

---

## 📲 Install · 安装

### Prerequisites · 前提

- Android 8.0+ (API 26+)
- [ZeroTermux](https://github.com/hanxinhao000/ZeroTermux) or any proot terminal
- Python 3.8+ + FFmpeg (in terminal environment)

### Build · 构建

```bash
git clone https://github.com/MuXi36/Bank2Mp3.git
cd Bank2Mp3

# Requires ZeroTermux + JDK 17
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
bash gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### First Run · 首次运行

1. Install the APK · 安装 APK
2. Start the bridge in ZeroTermux · 在终端启动桥接：
   ```bash
   python3 /sdcard/Download/Bank2Mp3/scripts/terminal_server.py
   ```
3. Or auto-start via Operit workflow (checks every minute) · 或配置 Operit 工作流自动保活

---

## 🎮 Usage · 使用

| Operation | Path |
|-----------|------|
| Pick `.bank` file | File picker → single conversion |
| Pick directory | Document tree → batch scan |
| Terminal bridge | Start `terminal_server.py` first, then tap convert |
| Format conversion | WAV panel: MP3/AAC/FLAC/OGG/OPUS one-tap |
| Chinese classify | Batch classify → auto folder by name |
| Theme toggle | Top-right ◇/◆ button (dark/light) |
| Log collapse | Tap 「▼ ★ 日志」 title bar to toggle |

---

## 🎨 Motion System · 动效系统

| Effect | Description |
|--------|-------------|
| **Title Breathing** | ♫ triple slow breathe + diamond glow pulsation |
| **Kunkun Egg** | IKUN breathe scale + alpha shimmer |
| **Card Glow** | Bridge bar + log card 4.5s soft glow cycle |
| **Button Stagger** | 22 buttons bounce in (OvershootInterpolator) |
| **Chain Spring** | GSAP elastic.out multi-bounce on press |
| **Marquee** | Gold glow sweeps across terminal buttons |
| **Refresh Spin** | SVG Lucide refresh-cw icon rotation |
| **Star Twinkle** | 200 small + 12 big halos, independent sine breathe |
| **Swirl Arcs** | Van Gogh semi-transparent concentric arcs |
| **Linen Texture** | Light mode cross-hatch weave pattern |

---

## 📂 Project Structure · 项目结构

```
Bank2Mp3/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/bank2mp3/app/
│       │   ├── MainActivity.kt       # UI + all effects
│       │   ├── BridgeClient.kt       # HTTP bridge client
│       │   └── PythonRuntime.kt      # Python runtime manager
│       ├── res/
│       │   ├── layout/activity_main.xml
│       │   ├── drawable/             # icons, backgrounds
│       │   ├── values/colors.xml     # color themes
│       │   └── raw/kunkun.mp3        # easter egg audio
│       └── jniLibs/                  # FMOD .so libs
├── scripts/
│   ├── terminal_server.py            # HTTP bridge server
│   ├── batch_convert.py              # batch conversion
│   └── fmod_extract.py               # FMOD parser
├── build.gradle
├── settings.gradle
└── gradle.properties
```

---

## 🙏 Acknowledgments · 感谢

These projects made Bank2Mp3 possible:

| Project | Role |
|---------|------|
| [FMOD](https://www.fmod.com/) | .bank audio engine core |
| [FFmpeg](https://ffmpeg.org/) | Multi-format audio transcoding |
| [ZeroTermux](https://github.com/hanxinhao000/ZeroTermux) | Android proot terminal |
| [Operit AI](https://github.com/Vael-Li/Operit) | AI assistant + workflow scheduler |
| [Android Jetpack](https://developer.android.com/jetpack) | ViewBinding · Coroutines · Lifecycle |
| [Kotlin](https://kotlinlang.org/) | Modern Android language |
| [Lucide Icons](https://lucide.dev/) | refresh-cw SVG icon |
| [Shadcn/ui](https://ui.shadcn.com/) | Design language reference |

Special thanks to all developers who helped debug Shizuku permissions, proot path mapping, and Android 14 install prompts at 3 AM 🌙

---

## 📄 License · 许可证

MIT License · Copyright © 2025 Vael Li

---

<p align="center">
  <sub>Made with ❤️ by <a href="https://github.com/MuXi36">MuXi36</a> · Powered by late-night coffee ☕</sub>
</p>