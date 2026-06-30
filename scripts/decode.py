#!/usr/bin/env python3
"""FSB5/Bank → WAV 转换器 (fsb5 + ffmpeg)
用法: python3 decode.py <input.bank> [output_dir]

兼容两种运行模式：
- Operit 终端: ffmpeg from PATH, fsb5 from system
- 本地 sandbox: ffmpeg from rootfs, fsb5 from embedded modules
"""
import struct
import sys
import os
import subprocess
import concurrent.futures
from pathlib import Path

import shutil
# 查找 ffmpeg: 优先 rootfs，降级系统 PATH
_SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
_ROOTFS_BIN = os.path.normpath(os.path.join(_SCRIPT_DIR, '..', '..', 'rootfs', 'bin'))
_FFMPEG_CANDIDATE = os.path.join(_ROOTFS_BIN, 'ffmpeg')
if os.path.isfile(_FFMPEG_CANDIDATE) and os.access(_FFMPEG_CANDIDATE, os.X_OK):
    FFMPEG = _FFMPEG_CANDIDATE
    os.environ['PATH'] = _ROOTFS_BIN + ':' + os.environ.get('PATH', '/usr/bin:/bin')
else:
    # 终端环境：shutil.which + 硬编码兜底
    FFMPEG = shutil.which('ffmpeg') or '/usr/bin/ffmpeg'

try:
    import fsb5
except ImportError:
    # 尝试从脚本目录加载嵌入式 fsb5
    sys.path.insert(0, os.path.join(_SCRIPT_DIR, 'fsb5'))
    try:
        import fsb5
    except ImportError:
        print("需要安装 fsb5 库: pip3 install --break-system-packages fsb5")
        sys.exit(1)


def parse_bank(filepath):
    """解析 FMOD Bank (RIFF 容器)，提取内嵌 FSB5 块"""
    with open(filepath, 'rb') as f:
        data = f.read()
    if data[0:4] != b'RIFF':
        raise ValueError("Not a RIFF file")
    
    sndh_pos = data.find(b'SNDH')
    if sndh_pos < 0:
        raise ValueError("No SNDH chunk found")
    
    chunk_size = struct.unpack_from('<I', data, sndh_pos + 4)[0]
    fsb_count = (chunk_size - 4) // 8
    pair_start = sndh_pos + 12
    
    entries = []
    for j in range(fsb_count):
        off = struct.unpack_from('<I', data, pair_start + j * 8)[0]
        sz = struct.unpack_from('<I', data, pair_start + j * 8 + 4)[0]
        if off > 0 and sz > 0 and off + sz <= len(data):
            entries.append(data[off:off + sz])
    return entries


def convert_bank(input_path, output_dir):
    """转换 bank 文件 → WAV"""
    input_path = Path(input_path)
    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    
    print(f"解析: {input_path.name}")
    
    with open(input_path, 'rb') as f:
        header = f.read(4)
    
    if header == b'RIFF':
        fsb5_entries = parse_bank(input_path)
    elif header == b'FSB5':
        fsb5_entries = [open(input_path, 'rb').read()]
    else:
        raise ValueError(f"不支持的文件格式: {header}")
    
    all_samples = []
    for ei, raw in enumerate(fsb5_entries):
        prefix = f"{ei}_" if len(fsb5_entries) > 1 else ""
        try:
            fsb = fsb5.FSB5(raw)
            for s in fsb.samples:
                all_samples.append((fsb, s, prefix))
        except Exception as e:
            print(f"  FSB5[{ei}] 解析失败: {e}")
    
    print(f"FSB5块: {len(fsb5_entries)}, 样本数: {len(all_samples)}")
    
    def convert_one(args):
        fsb, sample, prefix = args
        try:
            name = prefix + (sample.name or f"sample_{sample.index}")
            
            ogg_data = fsb.rebuild_sample(sample)
            if not ogg_data:
                print(f"  ✗ {name}: rebuild_sample 返回空")
                return None
            
            ogg_path = output_dir / f"{name}.ogg"
            ogg_path.write_bytes(ogg_data)
            
            wav_path = output_dir / f"{name}.wav"
            result = subprocess.run([
                FFMPEG, '-y', '-loglevel', 'error',
                '-i', str(ogg_path),
                '-acodec', 'pcm_s16le',
                str(wav_path)
            ], capture_output=True, text=True, timeout=60)
            
            if result.returncode == 0 and wav_path.exists():
                ogg_path.unlink()
                size_kb = wav_path.stat().st_size / 1024
                dur = size_kb / (sample.frequency * sample.channels * 2) * 1024
                print(f"  ✅ {name} → {wav_path.name} ({size_kb:.0f}KB, {dur:.1f}s)")
                print(f"OK:{wav_path.name}")
                return str(wav_path)
            else:
                err = result.stderr.strip()[-150:] if result.stderr else "unknown"
                print(f"  ✗ {name}: ffmpeg — {err}")
                return str(ogg_path)
        
        except Exception as e:
            print(f"  ✗ {name}: {e}")
            return None
    
    results = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        futures = {pool.submit(convert_one, args): args for args in all_samples}
        for f in concurrent.futures.as_completed(futures):
            r = f.result()
            if r:
                results.append(r)
    
    wav_count = sum(1 for r in results if r.endswith('.wav'))
    print(f"\n完成: {wav_count}/{len(all_samples)} WAV → {output_dir}")
    return results


if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("用法: python3 decode.py <bank文件> [输出目录]")
        sys.exit(1)
    
    input_file = sys.argv[1]
    output = sys.argv[2] if len(sys.argv) > 2 else os.path.join(
        os.path.dirname(input_file) or '.', 'output')
    convert_bank(input_file, output)