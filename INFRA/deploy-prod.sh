#!/bin/bash
# ==========================================
# E106 prod deployment script
# - backs up current image
# - deploys latest image
# - rolls back automatically on health check failure
# ==========================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.prod.yml"
ENV_FILE="$SCRIPT_DIR/.env.prod"

echo "=========================================="
echo " prod deployment start"
echo "=========================================="

if [ ! -f "$ENV_FILE" ]; then
  echo ".env.prod file not found."
  echo "Create INFRA/.env.prod before deployment."
  exit 1
fi

source "$ENV_FILE"
IMAGE="${REGISTRY_URL}:prod"
IMAGE_PREV="${REGISTRY_URL}:prod-prev"

echo ""
echo "[1/4] Backing up current image..."

if docker image inspect "$IMAGE" > /dev/null 2>&1; then
  docker tag "$IMAGE" "$IMAGE_PREV"
  echo "  Backed up $IMAGE to $IMAGE_PREV"
else
  echo "  No existing prod image found. Skipping backup."
fi

echo ""
echo "[2/4] Pulling latest image..."
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" pull backend-prod

echo ""
echo "[3/4] Restarting containers..."
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d

echo ""
echo "[4/4] Waiting for health check..."

MAX_RETRY=60
RETRY_INTERVAL=2
HEALTH_URL="http://localhost:8082/api/v1/health"
HEALTH_TARGET_URL="http://backend-prod:8080/api/v1/health"
NETWORK_NAME=$(docker inspect e106-backend-prod --format '{{range $k, $_ := .NetworkSettings.Networks}}{{$k}}{{end}}')

if [ -z "$NETWORK_NAME" ]; then
  echo "Could not resolve Docker network for e106-backend-prod."
  exit 1
fi

for i in $(seq 1 $MAX_RETRY); do
  HEALTH_RESPONSE=$(docker run --rm --network "$NETWORK_NAME" curlimages/curl:8.12.1 -s -w $'\n%{http_code}' "$HEALTH_TARGET_URL" || true)
  HTTP_STATUS=$(printf '%s\n' "$HEALTH_RESPONSE" | tail -n 1)
  printf '%s\n' "$HEALTH_RESPONSE" | sed '$d' > /tmp/e106-prod-health.out
  if [ "$HTTP_STATUS" = "200" ]; then
    echo ""
    echo "=========================================="
    echo " prod deployment complete"
    echo "=========================================="
    exit 0
  fi
  echo "  waiting... ($i/$MAX_RETRY) status=$HTTP_STATUS"
  sleep $RETRY_INTERVAL
done

echo ""
echo "=========================================="
echo " health check failed -> starting rollback"
echo "=========================================="
echo ""
echo "--- container status ---"
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps
echo ""
echo "--- backend-prod port mapping ---"
docker port e106-backend-prod || true
echo ""
echo "--- health response ---"
cat /tmp/e106-prod-health.out 2>/dev/null || true
echo ""
echo "--- health curl -v ---"
docker run --rm --network "$NETWORK_NAME" curlimages/curl:8.12.1 -v "$HEALTH_TARGET_URL" || true
echo ""
echo "--- backend-prod inspect ---"
docker inspect e106-backend-prod --format 'status={{.State.Status}} exitCode={{.State.ExitCode}} error={{.State.Error}} restartCount={{.RestartCount}}' || true
echo ""
echo "--- backend-prod direct logs ---"
docker logs --tail=200 e106-backend-prod || true

if docker image inspect "$IMAGE_PREV" > /dev/null 2>&1; then
  echo ""
  echo "Rolling back to previous image..."
  docker tag "$IMAGE_PREV" "$IMAGE"
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d

  echo "Waiting for rollback health check..."
  for i in $(seq 1 $MAX_RETRY); do
    HEALTH_RESPONSE=$(docker run --rm --network "$NETWORK_NAME" curlimages/curl:8.12.1 -s -w $'\n%{http_code}' "$HEALTH_TARGET_URL" || true)
    HTTP_STATUS=$(printf '%s\n' "$HEALTH_RESPONSE" | tail -n 1)
    printf '%s\n' "$HEALTH_RESPONSE" | sed '$d' > /tmp/e106-prod-health-rollback.out
    if [ "$HTTP_STATUS" = "200" ]; then
      echo ""
      echo "=========================================="
      echo " rollback complete -> previous version restored"
      echo "=========================================="
      exit 1
    fi
    echo "  rollback waiting... ($i/$MAX_RETRY) status=$HTTP_STATUS"
    sleep $RETRY_INTERVAL
  done

  echo ""
  echo "=========================================="
  echo " rollback also failed -> manual intervention required"
  echo "=========================================="
  echo ""
  echo "--- rollback health response ---"
  cat /tmp/e106-prod-health-rollback.out 2>/dev/null || true
  echo ""
  echo "--- rollback health curl -v ---"
  docker run --rm --network "$NETWORK_NAME" curlimages/curl:8.12.1 -v "$HEALTH_TARGET_URL" || true
  echo ""
  echo "--- backend-prod inspect ---"
  docker inspect e106-backend-prod --format 'status={{.State.Status}} exitCode={{.State.ExitCode}} error={{.State.Error}} restartCount={{.RestartCount}}' || true
  echo ""
  echo "--- backend-prod direct logs ---"
  docker logs --tail=200 e106-backend-prod || true
  echo ""
  echo "--- container status ---"
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps
  echo ""
  echo "--- backend-prod logs ---"
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" logs --tail=150 backend-prod
  echo ""
  echo "--- mysql-prod logs ---"
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" logs --tail=100 mysql-prod
  echo ""
  echo "--- redis-prod logs ---"
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" logs --tail=100 redis-prod
  exit 1
else
  echo "  No backup image found. Rollback is not available."
  echo ""
  echo "--- backend-prod inspect ---"
  docker inspect e106-backend-prod --format 'status={{.State.Status}} exitCode={{.State.ExitCode}} error={{.State.Error}} restartCount={{.RestartCount}}' || true
  echo ""
  echo "--- backend-prod direct logs ---"
  docker logs --tail=200 e106-backend-prod || true
  echo ""
  echo "--- container status ---"
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps
  echo ""
  echo "--- backend-prod logs ---"
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" logs --tail=150 backend-prod
  exit 1
fi
