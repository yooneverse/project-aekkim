#!/bin/bash
# ==========================================
# E106 EC2 서버 초기 세팅 스크립트
# EC2 받자마자 1회만 실행
# 사용법: chmod +x server-init.sh && ./server-init.sh
# ==========================================

set -e  # 에러 발생 시 즉시 중단

echo "=========================================="
echo " E106 서버 초기 세팅 시작"
echo "=========================================="

# ------------------------------------------
# 1. 시스템 패키지 업데이트
# ------------------------------------------
echo ""
echo "[1/6] 시스템 패키지 업데이트..."
sudo apt-get update
sudo apt-get upgrade -y

# ------------------------------------------
# 2. Docker 설치 (공식 가이드)
# ------------------------------------------
echo ""
echo "[2/6] Docker 설치..."

# 필수 패키지 설치
sudo apt-get install -y ca-certificates curl gnupg

# GPG 키 등록
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --batch --yes --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

# Docker 공식 apt 저장소 추가
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 저장소 추가 후 다시 update
sudo apt-get update

# Docker 설치
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# 현재 유저를 docker 그룹에 추가 (sudo 없이 docker 사용)
sudo usermod -aG docker $USER

echo "[안내] docker 그룹 권한 반영을 시도했습니다."
echo "[안내] sudo 없이 docker 명령이 동작하지 않으면 새 터미널을 열거나 재로그인 후 다시 시도하세요."

# Docker 설치 확인
echo ""
echo "--- Docker 설치 확인 ---"
sudo systemctl status docker --no-pager
sudo docker ps
echo "Docker 설치 완료 ✅"

# ------------------------------------------
# 3. Nginx 설치
# ------------------------------------------
echo ""
echo "[3/6] Nginx 설치..."
sudo apt-get install -y nginx

# ------------------------------------------
# 4. Nginx 설정 적용
# ------------------------------------------
echo ""
echo "[4/6] Nginx 설정 적용..."

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# e106.conf 복사
sudo cp "$SCRIPT_DIR/nginx/e106.conf" /etc/nginx/sites-available/e106.conf

# 기본 사이트 비활성화
sudo rm -f /etc/nginx/sites-enabled/default

# e106 설정 활성화 (심링크)
sudo ln -sf /etc/nginx/sites-available/e106.conf /etc/nginx/sites-enabled/e106.conf

# 설정 문법 검사 + 적용
sudo nginx -t && sudo systemctl reload nginx
echo "Nginx 설정 적용 완료 ✅"

# ------------------------------------------
# 5. 환경변수 파일 준비
# ------------------------------------------
echo ""
echo "[5/6] 환경변수 파일 준비..."

if [ ! -f "$SCRIPT_DIR/.env.dev" ]; then
  cp "$SCRIPT_DIR/.env.dev.example" "$SCRIPT_DIR/.env.dev"
  echo ".env.dev 생성됨"
fi

if [ ! -f "$SCRIPT_DIR/.env.prod" ]; then
  cp "$SCRIPT_DIR/.env.prod.example" "$SCRIPT_DIR/.env.prod"
  echo ".env.prod 생성됨"
fi

# ------------------------------------------
# 6. Docker Hub 로그인
# ------------------------------------------
echo ""
echo "[6/6] Docker Hub 로그인..."
echo "아래 명령어로 직접 로그인하세요:"
echo ""
echo "  docker login"
echo ""

# ------------------------------------------
# 완료
# ------------------------------------------
echo "=========================================="
echo " 초기 세팅 완료!"
echo "=========================================="
echo ""
echo "⚠️  남은 작업:"
echo "  1. infra/.env.dev, .env.prod 의 비밀번호/시크릿 값 채우기"
echo "  2. docker login registry.example.com 실행"
echo "  3. e106.conf 의 server_name 을 api.example.com 로 확인"
echo "  4. docker 권한이 바로 적용되지 않으면 새 터미널 또는 재로그인 후 확인"
echo ""
