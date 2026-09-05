#!/usr/bin/env bash
set +e
BASE=http://localhost:5000

echo "=== 1. cards/stats/topup/monthly/by-card ==="
curl -sS -m 15 -w '\nHTTP=%{http_code}\n' "$BASE/api/cards/stats/topup/monthly/by-card?year=2035&cardNumber=9999999999999999"

echo
echo "=== 2. saldos/stats/balance/monthly ==="
curl -sS -m 15 -w '\nHTTP=%{http_code}\n' "$BASE/api/saldos/stats/balance/monthly?year=2026"

echo
echo "=== 3. saldos/stats/balance/monthly no params ==="
curl -sS -m 15 -w '\nHTTP=%{http_code}\n' "$BASE/api/saldos/stats/balance/monthly"

echo
echo "=== 4. cards/stats/topup/monthly ==="
curl -sS -m 15 -w '\nHTTP=%{http_code}\n' "$BASE/api/cards/stats/topup/monthly?year=2035"

echo
echo "=== 5. card stats handlers present? ==="
grep -n "class CardStatsTopup" card/src/main/java/com/sanedge/card/handler/CardStatsTopupGrpcHandler.java | head -3
grep -n "Panache.withSession" card/src/main/java/com/sanedge/card/handler/CardStatsTopupGrpcHandler.java | head -3
grep -n "class SaldoStatsBalance" saldo/src/main/java/com/sanedge/saldo/handler/SaldoStatsBalanceGrpcHandler.java | head -3
grep -n "Panache.withSession" saldo/src/main/java/com/sanedge/saldo/handler/SaldoStatsBalanceGrpcHandler.java | head -3

echo
echo "=== 6. recent card/saldo logs ==="
docker logs --since 5m card 2>&1 | grep -Ei 'error|exception|session' | tail -5
docker logs --since 5m saldo 2>&1 | grep -Ei 'error|exception|session' | tail -5
