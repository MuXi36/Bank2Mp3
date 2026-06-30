#!/usr/bin/env python3
"""
Sky .bank 音频自动分类器
基于 SoundDefs.lua 中的 bundle 体系 + 文件名前缀分析
"""

import os, sys, json, re

# ═══════════════════════════════════════════
# 分类规则（中文）
# ═══════════════════════════════════════════

BANK_CATEGORIES = {
    # ── 音乐 ──
    "Music_Dawn":       "🎵音乐/晨岛",
    "Music_Prairie":    "🎵音乐/云野",  
    "Music_Rain":       "🎵音乐/雨林",
    "Music_Sunset":     "🎵音乐/霞谷",
    "Music_Dusk":       "🎵音乐/暮土",
    "Music_Night":      "🎵音乐/禁阁",
    "Music_Storm":      "🎵音乐/暴风眼",
    "Music_Orbit":      "🎵音乐/重生之路",
    "Music_Aviary":     "🎵音乐/云巢",
    "Music_Sheets":     "🎵音乐/乐谱",
    "Music_CollectQuest":"🎵音乐/收集任务",
    "Music_AP":         "🎵音乐/季节活动",
    "Music_":           "🎵音乐/其他",

    # ── 音效 ──
    "SFX_Common":        "🔊音效/通用",
    "SFX_AvatarAnim":    "🔊音效/角色动作",
    "SFX_Emote":         "🔊音效/表情",
    "SFX_Instrument":    "🔊音效/乐器",
    "SFX_Shout":         "🔊音效/呼唤",
    "SFX_SpiritAnim":    "🔊音效/先祖",
    "SFX_LightCrature":  "🔊音效/光之生物",
    "SFX_CandleSpace":   "🔊音效/烛火空间",
    "SFX_DarkCreature":  "🔊音效/暗之生物",
    "SFX_MainStreet":    "🔊音效/遇境",
    "SFX_Dawn":          "🔊音效/晨岛",
    "SFX_Prairie":       "🔊音效/云野",
    "SFX_Rain":          "🔊音效/雨林",
    "SFX_Sunset":        "🔊音效/霞谷",
    "SFX_Dusk":          "🔊音效/暮土",
    "SFX_Night":         "🔊音效/禁阁",
    "SFX_Storm":         "🔊音效/暴风眼",
    "SFX_Orbit":         "🔊音效/重生之路",
    "SFX_Aurora":        "🔊音效/欧若拉",
    "SFX_":              "🔊音效/其他",

    # ── 通用/系统 ──
    "MasterBank":        "⚙️系统/主控",
    "Common":            "⚙️系统/通用",
    "Boot":              "⚙️系统/启动",
}

# .strings.bank → 事件名（无音频）
SKIP_PATTERNS = [".strings.bank"]

def classify_bank(filename: str) -> str:
    """根据文件名返回分类路径"""
    basename = os.path.basename(filename)
    
    # 排除 .strings.bank
    for pat in SKIP_PATTERNS:
        if pat in basename:
            return None
    
    # 去除 .assets / .streams 后缀，提取核心名
    core = basename
    for suffix in [".streams.bank", ".assets.bank", ".bank"]:
        if core.endswith(suffix):
            core = core[:-len(suffix)]
            break
    
    # 匹配分类规则
    for prefix, category in BANK_CATEGORIES.items():
        if core.startswith(prefix):
            return category
    
    return "📦未分类"

def classify_wav(wav_path: str, bank_name: str) -> str:
    """对单个 WAV 文件进行分类，附加中文标签"""
    basename = os.path.basename(wav_path)
    category = classify_bank(bank_name)
    if category is None:
        return None
    
    # 从 event 路径中提取层级信息用于子分类
    parts = basename.split('/')
    name = parts[-1] if len(parts) > 1 else basename
    
    return {
        "category": category,
        "name": name,
        "original": basename,
        "bank": bank_name
    }

def get_category_dir(output_base: str, category: str) -> str:
    """获取分类输出目录"""
    d = os.path.join(output_base, category)
    os.makedirs(d, exist_ok=True)
    return d

if __name__ == "__main__":
    import argparse
    p = argparse.ArgumentParser()
    p.add_argument("--bank", help="bank 文件名")
    p.add_argument("--list", action="store_true", help="列出所有分类")
    args = p.parse_args()
    
    if args.list:
        seen = set()
        for prefix, cat in BANK_CATEGORIES.items():
            if cat not in seen:
                seen.add(cat)
                print(cat)
    elif args.bank:
        cat = classify_bank(args.bank)
        if cat:
            print(cat)
        else:
            print("SKIP")
    else:
        print(json.dumps(list(set(BANK_CATEGORIES.values())), ensure_ascii=False, indent=2))