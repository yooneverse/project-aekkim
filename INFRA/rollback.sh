#!/bin/bash
# ==========================================
# E106 prod 수동 롤백 스크립트
# 배포 후 문제 발견 시 이전 버전으로 복구
# 사용법: ./rollback.sh
# ==========================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.prod.yml"
ENV_FILE="$SCRIPT_DIR/.env.prod"

echo "=========================================="
echo " prod 롤백 시작"
echo "=========================================="

# ------------------------------------------
# 1. 사전 검증
# ------------------------------------------
if [ ! -f "$ENV_FILE" ]; then
  echo "❌ .env.prod 파일이 없습니다."
  exit 1
fi

source "$ENV_FILE"
IMAGE="${REGISTRY_URL}:prod"
IMAGE_PREV="${REGISTRY_URL}:prod-prev"
AI_IMAGE="${AI_REGISTRY_URL}:prod"
AI_IMAGE_PREV="${AI_REGISTRY_URL}:prod-prev"

# 백업 이미지 존재 확인
if ! docker image inspect "$IMAGE_PREV" > /dev/null 2>&1; then
  echo "❌ 롤백할 이미지(prod-prev)가 없습니다."
  echo "   최초 배포이거나 백업이 생성되지 않았습니다."
  exit 1
fi

# ------------------------------------------
# 2. 이전 이미지로 복구
# ------------------------------------------
echo ""
echo "[1/3] 이전 이미지로 복구..."
docker tag "$IMAGE_PREV" "$IMAGE"
echo "  $IMAGE_PREV → $IMAGE"
if docker image inspect "$AI_IMAGE_PREV" > /dev/null 2>&1; then
  docker tag "$AI_IMAGE_PREV" "$AI_IMAGE"
  echo "  $AI_IMAGE_PREV → $AI_IMAGE"
fi

# ------------------------------------------
# 3. 컨테이너 재시작
# ------------------------------------------
echo ""
echo "[2/3] 컨테이너 재시작..."
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d

# ------------------------------------------
# 4. 헬스체크 (최대 60초 대기)
# ------------------------------------------
echo ""
echo "[3/3] 헬스체크 대기 중..."

MAX_RETRY=30
RETRY_INTERVAL=2
HEALTH_URL="http://localhost:8082/api/v1/health"

for i in $(seq 1 $MAX_RETRY); do
  if curl -sf "$HEALTH_URL" > /dev/null 2>&1; then
    echo ""
    echo "=========================================="
    echo " ✅ 롤백 완료 — 이전 버전으로 복구됨"
    echo "=========================================="
    exit 0
  fi
  echo "  대기 중... ($i/$MAX_RETRY)"
  sleep $RETRY_INTERVAL
done

# ------------------------------------------
# 5. 롤백 실패
# ------------------------------------------
echo ""
echo "=========================================="
echo " 🚨 롤백 실패 — 수동 확인 필요"
echo "=========================================="
echo ""
echo "--- backend-prod 로그 ---"
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" logs --tail=50 backend-prod
echo ""
echo "--- ai-scraper-prod 로그 ---"
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" logs --tail=50 ai-scraper-prod
exit 1
