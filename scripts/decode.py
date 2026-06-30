#!/usr/bin/env python3
"""FSB5/Bank → WAV 转换器 (fsb5 + ffmpeg)"""
import sys
import os
import shutil
import struct

# 确保能找到 fsb5 模块
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

def find_ffmpeg():
    """找到 ffmpeg 路径"""
    path = shutil.which('ffmpeg')
    if path:
        return path
    for p in ['/usr/bin/ffmpeg', '/usr/local/bin/ffmpeg', '/data/data/com.termux/files/usr/bin/ffmpeg']:
        if os.path.isfile(p):
            return p
    raise FileNotFoundError('ffmpeg not found')

def find_ffprobe():
    path = shutil.which('ffprobe')
    if path:
        return path
    for p in ['/usr/bin/ffprobe', '/usr/local/bin/ffprobe', '/data/data/com.termux/files/usr/bin/ffprobe']:
        if os.path.isfile(p):
            return p
    return None

def convert_bank(input_path, output_dir='.', format='wav', mp3_bitrate='192k'):
    """
    将 .bank 文件转换为音频格式
    
    Args:
        input_path: .bank 文件路径
        output_dir: 输出目录
        format: wav, mp3, flac, aac, ogg, opus
        mp3_bitrate: MP3 比特率 (192k, 320k)
    
    Returns:
        list: 输出文件路径列表
    """
    if not os.path.isfile(input_path):
        print(f"错误: 文件不存在 {input_path}", file=sys.stderr)
        return []
    
    os.makedirs(output_dir, exist_ok=True)
    
    # Step 1: fsb5 解析
    from fsb5 import FSB5
    base = os.path.splitext(os.path.basename(input_path))[0]
    
    output_files = []
    try:
        fsb = FSB5(input_path)
        samples = fsb.rebuild()
        
        ffmpeg = find_ffmpeg()
        
        for i, (sample_data, sample_rate, channels) in enumerate(samples):
            # 写入临时 PCM
            tmp_pcm = os.path.join(output_dir, f"_tmp_{base}_{i}.pcm")
            with open(tmp_pcm, 'wb') as f:
                f.write(sample_data)
            
            # ffmpeg 转码
            out_ext = {'mp3': 'mp3', 'flac': 'flac', 'aac': 'm4a', 'ogg': 'ogg', 'opus': 'opus'}.get(format, 'wav')
            if format == 'wav':
                out_name = f"{base}_{i:03d}.wav"
            else:
                out_name = f"{base}_{i:03d}.{out_ext}"
            out_path = os.path.join(output_dir, out_name)
            
            cmd = [
                ffmpeg, '-y', '-f', f's{16 if struct.pack("h", 1) == b"\x01\x00" else 8}le',
                '-ar', str(sample_rate), '-ac', str(channels),
                '-i', tmp_pcm
            ]
            
            if format == 'mp3':
                cmd += ['-codec:a', 'libmp3lame', '-b:a', mp3_bitrate]
            elif format == 'flac':
                cmd += ['-codec:a', 'flac']
            elif format == 'aac':
                cmd += ['-codec:a', 'aac', '-b:a', '192k']
            elif format == 'ogg':
                cmd += ['-codec:a', 'libvorbis', '-q:a', '6']
            elif format == 'opus':
                cmd += ['-codec:a', 'libopus', '-b:a', '128k', '-vbr', 'on']
            else:
                cmd += ['-codec:a', 'pcm_s16le']
            
            cmd.append(out_path)
            
            import subprocess
            subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)
            os.remove(tmp_pcm)
            output_files.append(out_path)
            print(f"  ✅ {out_name}")
            
    except ImportError as e:
        print(f"错误: fsb5 模块加载失败: {e}", file=sys.stderr)
        return []
    except Exception as e:
        print(f"错误: 解码失败: {e}", file=sys.stderr)
        return []
    
    return output_files

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("用法: decode.py <bank文件> [输出目录] [格式] [mp3码率]")
        sys.exit(1)
    
    input_file = sys.argv[1]
    output = sys.argv[2] if len(sys.argv) > 2 else '.'
    fmt = sys.argv[3] if len(sys.argv) > 3 else 'wav'
    bitrate = sys.argv[4] if len(sys.argv) > 4 else '192k'
    
    files = convert_bank(input_file, output, fmt, bitrate)
    print(f"\n✅ 完成 | {len(files)} 个文件 → {output}")
