# 🎵 Bank2Mp3 — FMOD `.bank` 音频提取器

> Android 原生应用 · 终端桥接架构 · 梵高主题动效  
> 从游戏 `.bank` 文件中提取音频，批量转码为 MP3/AAC/FLAC/OGG/OPUS

[![Android](https://img.shields.io/badge/Android-8.0%2B-green?logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue?logo=kotlin)](https://kotlinlang.org)
[![Python](https://img.shields.io/badge/Python-3.8%2B-yellow?logo=python)](https://python.org)
[![Min SDK 26](https://img.shields.io/badge/minSdk-26-orange)](https://apilevels.com)

---

## ✨ 特性

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

---

## 🖼️ 预览

| 暗色 · 梵高星空 | 亮色 · 油画布 |
|:---:|:---:|
| 深蓝渐变 + 闪烁星空 + 漩涡弧线 + 12 颗呼吸光斑 | 暖奶油底 + 亚麻编织纹理 + 暖金短笔触 + 柔光斑 |

> 背景基于 Canvas 实时绘制，非图片素材。星空粒子以正弦波独立呼吸闪烁（100ms/帧）。

---

## 🏗️ 架构

```
┌─────────────────────────────────────────┐
│              Android APK                 │
│  ┌───────────────────────────────────┐  │
│  │     MainActivity (Kotlin)          │  │
│  │  • UI / 动效 / 主题               │  │
│  │  • OkHttp → localhost:8899        │  │
│  └──────────────┬────────────────────┘  │
│                 │ HTTP POST /exec        │
│                 │       /batch           │
│                 │       /wav2mp3         │
│  ┌──────────────▼────────────────────┐  │
│  │  terminal_server.py (Python)      │  │
│  │  • FMOD 解析 · FFmpeg 转码       │  │
│  │  • 批量处理 · 中文分类           │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

---

## 📲 安装

### 前提条件

- Android 8.0+ (API 26+)
- [ZeroTermux](https://github.com/hanxinhao000/ZeroTermux) 或其他 proot 终端
- Python 3.8+ + FFmpeg（终端环境中）

### 构建

```bash
# 克隆仓库
git clone https://github.com/MuXi36/Bank2Mp3.git
cd Bank2Mp3

# 编译 APK（需 ZeroTermux + JDK 17）
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
bash gradlew assembleDebug

# APK 输出路径
# app/build/outputs/apk/debug/app-debug.apk
```

### 首次运行

1. 安装 APK
2. 在 ZeroTermux 中启动桥接服务：
   ```bash
   python3 /sdcard/Download/Bank2Mp3/scripts/terminal_server.py
   ```
3. 或配置 Operit 工作流自动保活（每分钟检查）

---

## 🎮 使用

| 操作 | 路径 |
|------|------|
| 选择 `.bank` 文件 | 文件选择器 → 单文件转换 |
| 选择目录 | 文档树选择器 → 批量扫描 |
| 终端桥接 | 先启动 `terminal_server.py`，再点转换按钮 |
| 格式转换 | WAV 区：MP3/AAC/FLAC/OGG/OPUS 一键转码 |
| 中文分类 | 批量分类 → 自动创建中文目录 |
| 主题切换 | 右上角 ◇/◆ 按钮切换暗色/亮色 |
| 日志折叠 | 点击「▼ ★ 日志」标题栏折叠/展开 |

---

## 🎨 动效系统

| 动效 | 描述 |
|------|------|
| **标题呼吸** | ♫ 符号三重慢呼吸 + 菱形辉光脉动 |
| **坤坤彩蛋** | IKUN 呼吸缩放 + alpha 微光 |
| **卡片柔光** | 桥接栏 + 日志区 4.5s 周期柔光脉动 |
| **按钮入场** | 22 个按钮 stagger 弹入 (OvershootInterpolator) |
| **链式按压** | 模拟 GSAP elastic.out 多段弹跳 |
| **跑马灯** | 终端按钮金色光晕沿按钮周期扫过 |
| **刷新旋转** | SVG Lucide refresh-cw 图标 |
| **星空闪烁** | 200 小星 + 12 大星光斑，独立正弦波呼吸 |
| **漩涡弧线** | 梵高式半透明同心弧线 |
| **亚麻纹理** | 亮色画布纵横交叉细线 |

---

## 📂 项目结构

```
Bank2Mp3/
├── app/
│   ├── build.gradle                    # 应用构建配置
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/bank2mp3/app/
│       │   ├── MainActivity.kt         # 主界面 + 全部动效
│       │   ├── BridgeClient.kt         # HTTP 桥接客户端
│       │   └── PythonRuntime.kt        # Python 运行时管理
│       ├── res/
│       │   ├── layout/activity_main.xml
│       │   ├── drawable/               # 图标、背景
│       │   ├── values/colors.xml       # 色彩主题
│       │   └── raw/kunkun.mp3          # 彩蛋音频
│       └── jniLibs/                    # FMOD .so 库
├── scripts/
│   ├── terminal_server.py              # HTTP 桥接服务
│   ├── batch_convert.py                # 批量转换脚本
│   └── fmod_extract.py                 # FMOD 解析器
├── build.gradle                        # 顶层构建配置
├── settings.gradle
└── gradle.properties
```

---

## 🙏 感谢

衷心感谢以下项目和技术：

| 项目 | 用途 |
|------|------|
| [FMOD](https://www.fmod.com/) | .bank 音频引擎核心 |
| [FFmpeg](https://ffmpeg.org/) | 多格式音频转码 |
| [ZeroTermux](https://github.com/hanxinhao000/ZeroTermux) | Android proot 终端环境 |
| [Operit AI](https://github.com/Vael-Li/Operit) | AI 助手平台 + 工作流调度 |
| [Android Jetpack](https://developer.android.com/jetpack) | ViewBinding · Coroutines · Lifecycle |
| [Kotlin](https://kotlinlang.org/) | 现代 Android 开发语言 |
| [Lucide Icons](https://lucide.dev/) | refresh-cw SVG 图标 |
| [Shadcn/ui](https://ui.shadcn.com/) | 设计语言参考 |

特别感谢所有在深夜调试 Shizuku 权限、proot 路径映射和 Android 14 安装弹窗时提供帮助的开发者朋友们。

---

## 📄 许可证

MIT License · Copyright © 2025 Vael Li

---

<p align="center">
  <sub>Made with ❤️ by Vael · Powered by 凌晨 3 点的咖啡 ☕</sub>
</p>