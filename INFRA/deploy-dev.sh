#!/bin/bash
# ==========================================
# E106 dev 환경 배포 스크립트
# 사용법: ./deploy-dev.sh
# ==========================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.dev.yml"
ENV_FILE="$SCRIPT_DIR/.env.dev"

echo "=========================================="
echo " dev 배포 시작"
echo "=========================================="

# ------------------------------------------
# 1. 사전 검증
# ------------------------------------------
if [ ! -f "$ENV_FILE" ]; then
  echo "❌ .env.dev 파일이 없습니다."
  echo "   cp .env.dev.example .env.dev 후 값을 채워주세요."
  exit 1
fi

# ------------------------------------------
# 2. 최신 이미지 pull
# ------------------------------------------
echo ""
echo "[1/3] 최신 이미지 pull..."
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" pull backend-dev

# ------------------------------------------
# 3. 컨테이너 재시작
# ------------------------------------------
echo ""
echo "[2/3] 컨테이너 재시작..."
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d

# ------------------------------------------
# 4. 헬스체크 (최대 30초 대기)
# ------------------------------------------
echo ""
echo "[3/3] 헬스체크 대기 중..."

MAX_RETRY=60
RETRY_INTERVAL=2
HEALTH_URL="http://localhost:8081/api/v1/health"
HEALTH_TARGET_URL="http://backend-dev:8080/api/v1/health"
NETWORK_NAME=$(docker inspect e106-backend-dev --format '{{range $k, $_ := .NetworkSettings.Networks}}{{$k}}{{end}}')

if [ -z "$NETWORK_NAME" ]; then
  echo "Could not resolve Docker network for e106-backend-dev."
  exit 1
fi

for i in $(seq 1 $MAX_RETRY); do
  HEALTH_RESPONSE=$(docker run --rm --network "$NETWORK_NAME" curlimages/curl:8.12.1 -s -w $'\n%{http_code}' "$HEALTH_TARGET_URL" || true)
  HTTP_STATUS=$(printf '%s\n' "$HEALTH_RESPONSE" | tail -n 1)
  printf '%s\n' "$HEALTH_RESPONSE" | sed '$d' > /tmp/e106-dev-health.out
  if [ "$HTTP_STATUS" = "200" ]; then
    echo ""
    echo "=========================================="
    echo " dev 배포 완료 ✅"
    echo "=========================================="
    exit 0
  fi
  echo "  대기 중... ($i/$MAX_RETRY) status=$HTTP_STATUS"
  sleep $RETRY_INTERVAL
done

# ------------------------------------------
# 5. 헬스체크 실패
# ------------------------------------------
echo ""
echo "=========================================="
echo " dev 배포 실패 ❌"
echo "=========================================="
echo ""
echo "--- container 상태 ---"
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps
echo ""
echo "--- backend-dev port 매핑 ---"
docker port e106-backend-dev || true
echo ""
echo "--- health 응답 ---"
cat /tmp/e106-dev-health.out 2>/dev/null || true
echo ""
echo "--- health curl -v ---"
docker run --rm --network "$NETWORK_NAME" curlimages/curl:8.12.1 -v "$HEALTH_TARGET_URL" || true
echo ""
echo "--- backend-dev 로그 ---"
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" logs --tail=50 backend-dev
echo ""
echo "--- ai-scraper-dev 로그 ---"
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" logs --tail=50 ai-scraper-dev
exit 1
