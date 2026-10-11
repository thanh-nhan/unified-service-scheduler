#!/usr/bin/env bash
# Demo sequence (AC-E03.4) against a running app seeded with dev fixture data.
# Usage: scripts/demo.sh [base-url]   (default http://localhost:8080)
set -u

BASE="${1:-http://localhost:8080}"
DEALERSHIP=10000000-0000-0000-0000-000000000001
EV_CHECK=40000000-0000-0000-0000-000000000003
CUSTOMER_AN=50000000-0000-0000-0000-000000000001
VEHICLE_AN=60000000-0000-0000-0000-000000000001
CUSTOMER_BINH=50000000-0000-0000-0000-000000000002
VEHICLE_BINH=60000000-0000-0000-0000-000000000002
RUN_ID="demo-$(date +%s)"

# A future Monday-Saturday, so the dealership is open and the start time is not in the past.
DATE=""
for n in 7 8 9 10 11 12 13; do
  d=$(date -d "+$n days" +%F 2>/dev/null || date -v+${n}d +%F)
  dow=$(date -d "$d" +%u 2>/dev/null || date -j -f %F "$d" +%u)
  if [ "$dow" -le 6 ]; then DATE=$d; break; fi
done
START="${DATE}T09:00:00"

HAS_JQ=0; command -v jq >/dev/null 2>&1 && HAS_JQ=1
BODY_FILE=$(mktemp); trap 'rm -f "$BODY_FILE"' EXIT
STATUS=""

# call <label> <method> <path> [idempotency-key] [json-body]
call() {
  local label=$1 method=$2 path=$3 key=${4:-} data=${5:-}
  local args=(-s -o "$BODY_FILE" -w '%{http_code}' -X "$method" "$BASE$path")
  [ -n "$key" ] && args+=(-H "Idempotency-Key: $key")
  [ -n "$data" ] && args+=(-H 'Content-Type: application/json' -d "$data")
  STATUS=$(curl "${args[@]}")
  echo "== $label -> HTTP $STATUS"
}

show() { # show <jq filter> ; falls back to raw body without jq
  if [ "$HAS_JQ" = 1 ]; then jq -c "$1" "$BODY_FILE"; else cat "$BODY_FILE"; echo; fi
}

booking_json() { # booking_json <customer> <vehicle>
  printf '{"customerId":"%s","vehicleId":"%s","dealershipId":"%s","serviceTypeId":"%s","startTime":"%s"}' \
    "$1" "$2" "$DEALERSHIP" "$EV_CHECK" "$START"
}

echo "Base URL: $BASE   Slot: $START"

call "1. List service types" GET /api/v1/service-types
show '[.[] | {id, code, durationMinutes}]'

call "2. Availability on $DATE" GET "/api/v1/dealerships/$DEALERSHIP/availability?serviceTypeId=$EV_CHECK&date=$DATE"
show '{date, durationMinutes, slots: (.slots | length)}'

call "3. Book (expect 201)" POST /api/v1/appointments "$RUN_ID-1" "$(booking_json $CUSTOMER_AN $VEHICLE_AN)"
show '{id, status, startTime, endTime, technicianId, bayId}'
if [ "$HAS_JQ" = 1 ]; then APPT_ID=$(jq -r '.id' "$BODY_FILE")
else APPT_ID=$(sed -n 's/.*"id":"\([^"]*\)".*/\1/p' "$BODY_FILE" | head -1); fi

call "4. Replay, same Idempotency-Key (expect 200)" POST /api/v1/appointments "$RUN_ID-1" "$(booking_json $CUSTOMER_AN $VEHICLE_AN)"
show '{id, status}'

call "5. Conflicting booking, different customer (expect 409)" POST /api/v1/appointments "$RUN_ID-2" "$(booking_json $CUSTOMER_BINH $VEHICLE_BINH)"
show '{status, code, detail}'

call "6. Cancel (expect 200)" POST "/api/v1/appointments/$APPT_ID/cancel"
show '{id, status, cancelledAt}'

call "7. Rebook the freed slot (expect 201)" POST /api/v1/appointments "$RUN_ID-3" "$(booking_json $CUSTOMER_BINH $VEHICLE_BINH)"
show '{id, status, startTime}'
