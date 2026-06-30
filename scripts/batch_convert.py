#!/usr/bin/env python3
"""批量 Bank → WAV + 可选 MP3 + 可选自动分类到中文目录"""
import sys, os, subprocess, shutil
from pathlib import Path
from collections import Counter

SCRIPT_DIR = Path(__file__).parent
DECODE_PY = SCRIPT_DIR / "decode.py"
CLASSIFY_PY = SCRIPT_DIR / "classify.py"

sys.path.insert(0, str(SCRIPT_DIR))
from classify import classify_bank

def find_banks(scan_dir):
    banks = []
    for root, dirs, files in os.walk(scan_dir):
        for f in files:
            if f.endswith('.streams.bank'):
                banks.append(Path(root) / f)
    return sorted(banks)

def get_core_name(bank_path):
    core = bank_path.name
    for s in ['.streams.bank', '.assets.bank', '.bank']:
        if core.endswith(s): core = core[:-len(s)]; break
    return core

def batch_to_wav(bank_dir, out_dir, classify=False):
    banks = find_banks(bank_dir)
    if not banks:
        print(f"未找到 .streams.bank 文件: {bank_dir}")
        return

    out = Path(out_dir)
    out.mkdir(parents=True, exist_ok=True)
    print(f"找到 {len(banks)} 个音频 bank\n")

    ok_total, fail_total = 0, 0
    classified = {}

    for i, bank in enumerate(banks):
        core = get_core_name(bank)
        if classify:
            cat = classify_bank(bank.name)
            if not cat: cat = "📦未分类"
            sub = out / cat / core
            classified[bank.name] = cat
            # 提取纯中文标签做文件名前缀（去掉emoji）
            cn_tag = cat.split("/", 1)[-1] if "/" in cat else cat  # "晨岛" / "表情" / "通用"
            dec_prefix = cn_tag
            print(f"[{i+1}/{len(banks)}] {bank.name} → {cat}/{core}/")
        else:
            sub = out / core
            dec_prefix = ""
            print(f"[{i+1}/{len(banks)}] {bank.name}")
        sub.mkdir(parents=True, exist_ok=True)

        dec_cmd = ['python3', str(DECODE_PY), str(bank), str(sub)]
        if dec_prefix:
            dec_cmd += ['--prefix', dec_prefix]
        r = subprocess.run(dec_cmd, capture_output=True, text=True, timeout=300)
        for line in r.stdout.splitlines():
            if 'OK:' in line:        ok_total += 1
            elif line.startswith('✗'): fail_total += 1
        for line in r.stdout.splitlines()[-3:]:
            if line.strip(): print(f"  {line}")

    print(f"\n=== 汇总 ===")
    print(f"成功: {ok_total} WAV  |  失败: {fail_total}")

    if classify:
        print(f"\n📂 分类统计:")
        cats = Counter(classified.values())
        for cat, cnt in cats.most_common():
            print(f"  {cat}: {cnt} bank")

    wavs = sorted(Path(out).rglob("*.wav"))
    if wavs:
        total_size = sum(w.stat().st_size for w in wavs)
        print(f"\nWAV: {len(wavs)} 个 ({total_size/1024/1024:.1f}MB)")
        for w in wavs[:20]:
            rel = w.relative_to(out)
            print(f"  {rel}  ({w.stat().st_size/1024:.0f}KB)")
        if len(wavs) > 20: print(f"  ... +{len(wavs)-20} 个")

def wav_to_mp3_ffmpeg(wav_dir):
    wavs = sorted(Path(wav_dir).rglob("*.wav"))
    if not wavs:
        print(f"未找到 WAV: {wav_dir}")
        return
    print(f"转码 {len(wavs)} WAV → MP3 (192k)...")
    ok = 0
    for i, wav in enumerate(wavs):
        mp3 = wav.with_suffix('.mp3')
        r = subprocess.run([
            'ffmpeg', '-y', '-loglevel', 'error',
            '-i', str(wav), '-acodec', 'mp3', '-b:a', '192k', str(mp3)
        ], capture_output=True, timeout=120)
        if r.returncode == 0:
            ok += 1
            if i % 20 == 0 or i == len(wavs) - 1:
                rel = mp3.relative_to(wav_dir)
                print(f"  [{i+1}/{len(wavs)}] ✅ {rel} ({mp3.stat().st_size/1024:.0f}KB)")
        else:
            print(f"  [{i+1}/{len(wavs)}] ✗ {wav.name}")
    print(f"完成: {ok}/{len(wavs)} MP3")

if __name__ == '__main__':
    import argparse
    ap = argparse.ArgumentParser(
        formatter_class=argparse.RawDescriptionHelpFormatter,
        description="Sky .bank 批量转换 + 分类工具\n"
            "示例:\n"
            "  %(prog)s ./banks -o ./out                    # 基础批量\n"
            "  %(prog)s ./banks -o ./out --mp3              # +MP3\n"
            "  %(prog)s ./banks -o ./out --mp3 --classify   # +MP3 +中文分类\n")
    ap.add_argument('bank_dir', help='.bank 目录')
    ap.add_argument('-o', '--output', default='/storage/emulated/0/Download/Bank2Mp3_output')
    ap.add_argument('--mp3', action='store_true', help='WAV→MP3')
    ap.add_argument('--classify', action='store_true', help='自动分类到中文子目录')
    args = ap.parse_args()

    batch_to_wav(args.bank_dir, args.output, classify=args.classify)
    if args.mp3:
        print()
        wav_to_mp3_ffmpeg(args.output)