#!/bin/bash
# Terra 值守模式 Dashboard 构建与部署脚本
# 构建 terra 并输出到 web/public/terramens/

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
OUTPUT_DIR="$PROJECT_ROOT/web/public/terramens"

echo "=== Terra Duty Dashboard Build ==="
echo "Source: $SCRIPT_DIR"
echo "Output: $OUTPUT_DIR"

# 安装依赖
if [ ! -d "$SCRIPT_DIR/node_modules" ]; then
    echo "Installing dependencies..."
    cd "$SCRIPT_DIR"
    npm install
fi

# 构建
echo "Building..."
cd "$SCRIPT_DIR"
npm run build

# 清理旧的输出目录
echo "Deploying to $OUTPUT_DIR..."
rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

# 复制构建产物
cp -r "$SCRIPT_DIR/dist/"* "$OUTPUT_DIR/"

echo "=== Build and deploy complete ==="
echo "Dashboard is available at /terramens/"
