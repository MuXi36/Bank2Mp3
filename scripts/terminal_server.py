#!/usr/bin/env python3
"""Bank2Mp3 终端桥接 Server
监听 localhost:8899，接收 APK 的命令请求并执行"""
import json, subprocess, threading, os, sys
from http.server import HTTPServer, BaseHTTPRequestHandler
from pathlib import Path

PORT = 8899
SCRIPT_DIR = Path(__file__).parent

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        """健康检查 / 状态 / 文件列表"""
        if self.path == '/health':
            self._json({'status': 'ok', 'cwd': os.getcwd()})
        elif self.path.startswith('/status'):
            self._json({'status': 'running', 'port': PORT})
        else:
            self._json({'error': 'use POST'}, 404)

    def do_POST(self):
        """执行命令或批量任务"""
        try:
            length = int(self.headers.get('Content-Length', 0))
            body = self.rfile.read(length).decode('utf-8')
            req = json.loads(body)
            cmd_type = req.get('type', 'exec')

            if cmd_type == 'exec':
                # 单条命令
                cmd = req['command']
                timeout = req.get('timeout', 300)
                self._exec_cmd(cmd, timeout)

            elif cmd_type == 'batch':
                # 批量 bank → WAV + MP3
                bank_dir = req['bank_dir']
                out_dir = req.get('output', '/storage/emulated/0/Download/Bank2Mp3_output')
                to_mp3 = req.get('mp3', True)
                classify = req.get('classify', False)
                self._batch_convert(bank_dir, out_dir, to_mp3, classify)

            elif cmd_type == 'wav2mp3':
                # WAV → MP3
                wav_dir = req.get('wav_dir', '/storage/emulated/0/Download/Bank2Mp3_output')
                self._wav_to_mp3(wav_dir)

            else:
                self._json({'error': f'unknown type: {cmd_type}'}, 400)

        except Exception as e:
            self._json({'error': str(e)}, 500)

    def _exec_cmd(self, cmd, timeout):
        print(f'[exec] {cmd[:120]}')
        try:
            r = subprocess.run(cmd, shell=True, capture_output=True,
                               text=True, timeout=timeout, cwd='/tmp')
            self._json({
                'ok': r.returncode == 0,
                'code': r.returncode,
                'stdout': r.stdout[-5000:],
                'stderr': r.stderr[-2000:]
            })
        except subprocess.TimeoutExpired:
            self._json({'ok': False, 'error': 'timeout'}, 408)

    def _batch_convert(self, bank_dir, out_dir, to_mp3, classify):
        batch_py = SCRIPT_DIR / 'batch_convert.py'
        cmd = f'python3 {batch_py} "{bank_dir}" -o "{out_dir}"'
        if to_mp3:
            cmd += ' --mp3'
        if classify:
            cmd += ' --classify'
        print(f'[batch] {bank_dir} classify={classify}')
        try:
            r = subprocess.run(cmd, shell=True, capture_output=True,
                               text=True, timeout=600)
            ok_count = r.stdout.count('✅')
            if ok_count == 0:
                ok_count = sum(1 for line in r.stdout.splitlines() if 'OK:' in line)

            self._json({
                'ok': r.returncode == 0,
                'code': r.returncode,
                'stdout': r.stdout[-8000:],
                'stderr': r.stderr[-2000:],
                'wav_count': ok_count,
            })
        except subprocess.TimeoutExpired:
            self._json({'ok': False, 'error': 'batch timeout'}, 408)

    def _wav_to_mp3(self, wav_dir):
        print(f'[wav2mp3] {wav_dir}')
        try:
            # ffmpeg 批量
            cmd = f'''cd "{wav_dir}" && for f in *.wav; do
  ffmpeg -y -loglevel error -i "$f" -acodec mp3 -b:a 192k "${{f%.wav}}.mp3" 2>/dev/null && echo "OK:$f" || echo "FAIL:$f"
done'''
            r = subprocess.run(cmd, shell=True, capture_output=True,
                               text=True, timeout=600)
            ok = r.stdout.count('OK:')
            fail = r.stdout.count('FAIL:')
            self._json({
                'ok': r.returncode == 0,
                'stdout': r.stdout[-3000:],
                'mp3_ok': ok, 'mp3_fail': fail
            })
        except subprocess.TimeoutExpired:
            self._json({'ok': False, 'error': 'timeout'}, 408)

    def _json(self, data, code=200):
        self.send_response(code)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()
        self.wfile.write(json.dumps(data, ensure_ascii=False).encode())

    def log_message(self, fmt, *args):
        pass  # 静默


def main():
    print(f'Bank2Mp3 Bridge → http://127.0.0.1:{PORT}')
    print(f'端点: POST /exec | /batch | /wav2mp3   GET /health')
    server = HTTPServer(('127.0.0.1', PORT), Handler)
    server.serve_forever()

if __name__ == '__main__':
    main()
