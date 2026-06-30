#!/bin/bash
SERVER="/storage/emulated/0/Download/Bank2Mp3/scripts/terminal_server.py"

case "${1:-status}" in
  start)
    if curl -s --connect-timeout 1 http://127.0.0.1:8899/health > /dev/null 2>&1; then
      echo "🟢 已在运行"
    else
      nohup python3 "$SERVER" > /dev/null 2>&1 &
      sleep 1
      curl -s http://127.0.0.1:8899/health && echo "🟢 已启动" || echo "🔴 启动失败"
    fi
    ;;
  stop)
    pkill -f terminal_server.py && echo "🔴 已停止" || echo "未在运行"
    ;;
  restart)
    pkill -f terminal_server.py 2>/dev/null
    sleep 1
    nohup python3 "$SERVER" > /dev/null 2>&1 &
    sleep 1
    curl -s http://127.0.0.1:8899/health && echo "🟢 已重启" || echo "🔴 失败"
    ;;
  status)
    curl -s --connect-timeout 1 http://127.0.0.1:8899/health && echo "🟢 运行中" || echo "🔴 未运行"
    ;;
esac
