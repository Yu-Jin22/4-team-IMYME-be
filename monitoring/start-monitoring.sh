#!/bin/bash

# MINE 통합 모니터링 스택 시작 스크립트
# Grafana + Prometheus + Pinpoint

set -e

echo "🚀 MINE 모니터링 스택 시작 중..."
echo ""

# 현재 디렉토리 확인
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 1. Docker Compose로 모니터링 스택 시작
echo "📦 Docker Compose 실행 중..."
docker-compose -f docker-compose.monitoring.yml up -d

echo ""
echo "⏳ Pinpoint HBase 초기화 대기 중 (약 60초)..."
echo "   (최초 실행 시 2분까지 소요될 수 있습니다)"

# HBase 초기화 완료 대기 (최대 120초)
TIMEOUT=120
ELAPSED=0
while [ $ELAPSED -lt $TIMEOUT ]; do
    if docker logs imyme-pinpoint-hbase 2>&1 | grep -q "Master has completed initialization"; then
        echo "✅ HBase 초기화 완료!"
        break
    fi

    sleep 5
    ELAPSED=$((ELAPSED + 5))
    echo "   대기 중... ${ELAPSED}초 경과"
done

if [ $ELAPSED -ge $TIMEOUT ]; then
    echo "⚠️  HBase 초기화 대기 시간 초과 (120초)"
    echo "   docker logs imyme-pinpoint-hbase 명령으로 로그를 확인하세요."
fi

echo ""
echo "🎉 모니터링 스택이 시작되었습니다!"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 접속 정보"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "🔍 Pinpoint Web UI"
echo "   URL      : http://localhost:8079"
echo "   인증      : 없음"
echo "   Application: MINE-LOCAL"
echo "   Agent ID  : local-mine-01"
echo ""
echo "📈 Grafana"
echo "   URL      : http://localhost:3000"
echo "   Username : admin"
echo "   Password : admin"
echo ""
echo "🔬 Prometheus"
echo "   URL      : http://localhost:9092"
echo "   Targets  : http://localhost:9092/targets"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📝 다음 단계:"
echo "   1. Spring Boot 앱 시작: ./gradlew bootRun"
echo "   2. Pinpoint에서 트래픽 확인"
echo "   3. Grafana에서 대시보드 import (README.md 참고)"
echo ""
echo "🛑 종료하려면: ./stop-monitoring.sh"
echo ""