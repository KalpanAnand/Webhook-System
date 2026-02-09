#!/bin/bash
set -e

BASE_URL="http://localhost:8080"
TEST_RESULTS="api-test-results.txt"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo "=========================================" > $TEST_RESULTS
echo "COMPREHENSIVE API TEST RESULTS" >> $TEST_RESULTS
echo "Date: $(date)" >> $TEST_RESULTS
echo "=========================================" >> $TEST_RESULTS
echo "" >> $TEST_RESULTS

PASSED=0
FAILED=0

test_endpoint() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4
    local expected_code=${5:-200}
    
    echo -e "${YELLOW}[TEST]${NC} $description"
    echo "[TEST] $description" >> $TEST_RESULTS
    echo "  Method: $method" >> $TEST_RESULTS
    echo "  Endpoint: $endpoint" >> $TEST_RESULTS
    
    if [ ! -z "$data" ]; then
        echo "  Request Body: $data" >> $TEST_RESULTS
        response=$(curl -s -w "\n%{http_code}" -X $method "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -d "$data" 2>&1)
    else
        response=$(curl -s -w "\n%{http_code}" -X $method "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" 2>&1)
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    echo "  HTTP Code: $http_code" >> $TEST_RESULTS
    echo "  Response: $body" >> $TEST_RESULTS
    
    if [ "$http_code" -eq "$expected_code" ]; then
        echo -e "${GREEN}  ✓ PASSED${NC}"
        echo "  Status: PASSED" >> $TEST_RESULTS
        ((PASSED++))
        echo "$body"
    else
        echo -e "${RED}  ✗ FAILED (Expected $expected_code, got $http_code)${NC}"
        echo "  Status: FAILED" >> $TEST_RESULTS
        ((FAILED++))
        echo "$body"
    fi
    echo "---" >> $TEST_RESULTS
    echo ""
}

echo "========================================="
echo "COMPREHENSIVE API TESTING"
echo "========================================="
echo ""

# ============================================
# 1. ADMIN EVENT TYPE APIs
# ============================================
echo -e "${BLUE}1. ADMIN EVENT TYPE APIs${NC}"
echo "1. ADMIN EVENT TYPE APIs" >> $TEST_RESULTS

# Create EventType
RESPONSE=$(test_endpoint "POST" "/api/admin/event-types" \
    '{"eventName": "api.test.order.created", "description": "API test order created"}' \
    "Create EventType - api.test.order.created" "201")
EVENT_TYPE_ID1=$(echo "$RESPONSE" | grep -o '"eventTypeId":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "  → Created EventType ID: $EVENT_TYPE_ID1"

# Create another EventType
RESPONSE=$(test_endpoint "POST" "/api/admin/event-types" \
    '{"eventName": "api.test.payment.success", "description": "API test payment success"}' \
    "Create EventType - api.test.payment.success" "201")
EVENT_TYPE_ID2=$(echo "$RESPONSE" | grep -o '"eventTypeId":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "  → Created EventType ID: $EVENT_TYPE_ID2"

# List all EventTypes
test_endpoint "GET" "/api/admin/event-types" "" "List all EventTypes" "200"

# Update EventType
if [ ! -z "$EVENT_TYPE_ID1" ]; then
    test_endpoint "PUT" "/api/admin/event-types/$EVENT_TYPE_ID1" \
        '{"description": "Updated description via API test"}' \
        "Update EventType" "200"
fi

# Delete EventType (optional - comment out if you want to keep test data)
# test_endpoint "DELETE" "/api/admin/event-types/$EVENT_TYPE_ID2" "" "Delete EventType" "200"

# ============================================
# 2. CLIENT APIs
# ============================================
echo ""
echo -e "${BLUE}2. CLIENT APIs${NC}"
echo "" >> $TEST_RESULTS
echo "2. CLIENT APIs" >> $TEST_RESULTS

# Register Client 1
RESPONSE=$(test_endpoint "POST" "/api/clients" \
    '{"organizationName": "APITestOrg1", "contactEmail": "apitest1@example.com"}' \
    "Register Client 1" "201")
CLIENT_ID1=$(echo "$RESPONSE" | grep -o '"clientId":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "  → Created Client ID: $CLIENT_ID1"

# Register Client 2
RESPONSE=$(test_endpoint "POST" "/api/clients" \
    '{"organizationName": "APITestOrg2", "contactEmail": "apitest2@example.com"}' \
    "Register Client 2" "201")
CLIENT_ID2=$(echo "$RESPONSE" | grep -o '"clientId":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "  → Created Client ID: $CLIENT_ID2"

# Test duplicate client registration (should fail)
test_endpoint "POST" "/api/clients" \
    '{"organizationName": "APITestOrg1", "contactEmail": "duplicate@example.com"}' \
    "Register duplicate Client (should fail)" "409"

# ============================================
# 3. SUBSCRIPTION APIs
# ============================================
echo ""
echo -e "${BLUE}3. SUBSCRIPTION APIs${NC}"
echo "" >> $TEST_RESULTS
echo "3. SUBSCRIPTION APIs" >> $TEST_RESULTS

# Create Subscription 1
if [ ! -z "$CLIENT_ID1" ] && [ ! -z "$EVENT_TYPE_ID1" ]; then
    RESPONSE=$(test_endpoint "POST" "/api/subscribers" \
        "{\"clientId\": \"$CLIENT_ID1\", \"eventTypeId\": \"$EVENT_TYPE_ID1\", \"webhookUrl\": \"http://localhost:9090/webhook1\"}" \
        "Create Subscription 1" "200")
    SUB_ID1=$(echo "$RESPONSE" | grep -o '"subscriptionId":"[^"]*"' | head -1 | cut -d'"' -f4)
    echo "  → Created Subscription ID: $SUB_ID1"
fi

# Create Subscription 2
if [ ! -z "$CLIENT_ID2" ] && [ ! -z "$EVENT_TYPE_ID2" ]; then
    RESPONSE=$(test_endpoint "POST" "/api/subscribers" \
        "{\"clientId\": \"$CLIENT_ID2\", \"eventTypeId\": \"$EVENT_TYPE_ID2\", \"webhookUrl\": \"http://localhost:9090/webhook2\"}" \
        "Create Subscription 2" "200")
    SUB_ID2=$(echo "$RESPONSE" | grep -o '"subscriptionId":"[^"]*"' | head -1 | cut -d'"' -f4)
    echo "  → Created Subscription ID: $SUB_ID2"
fi

# List all Subscriptions
test_endpoint "GET" "/api/subscribers" "" "List all Subscriptions" "200"

# List unsubscribed for a client
if [ ! -z "$CLIENT_ID1" ]; then
    test_endpoint "GET" "/api/subscribers/$CLIENT_ID1/unsubscribed" "" "List unsubscribed for Client 1" "200"
fi

# Update Subscription
if [ ! -z "$SUB_ID1" ] && [ ! -z "$EVENT_TYPE_ID2" ]; then
    test_endpoint "PUT" "/api/subscribers/$SUB_ID1" \
        "{\"eventTypeId\": \"$EVENT_TYPE_ID2\"}" \
        "Update Subscription 1" "200"
fi

# ============================================
# 4. EVENT APIs
# ============================================
echo ""
echo -e "${BLUE}4. EVENT APIs${NC}"
echo "" >> $TEST_RESULTS
echo "4. EVENT APIs" >> $TEST_RESULTS

# Ingest Event 1
if [ ! -z "$EVENT_TYPE_ID1" ]; then
    RESPONSE=$(test_endpoint "POST" "/api/events" \
        "{\"eventTypeId\": \"$EVENT_TYPE_ID1\", \"payload\": {\"orderId\": \"ORD-API-001\", \"amount\": 199.99, \"customer\": \"API Test Customer\"}}" \
        "Ingest Event 1" "200")
    EVENT_ID1=$(echo "$RESPONSE" | grep -o '"eventId":"[^"]*"' | head -1 | cut -d'"' -f4)
    echo "  → Created Event ID: $EVENT_ID1"
fi

# Ingest Event 2
if [ ! -z "$EVENT_TYPE_ID2" ]; then
    RESPONSE=$(test_endpoint "POST" "/api/events" \
        "{\"eventTypeId\": \"$EVENT_TYPE_ID2\", \"payload\": {\"paymentId\": \"PAY-API-002\", \"amount\": 299.99, \"status\": \"completed\", \"timestamp\": \"2026-02-08T00:00:00Z\"}}" \
        "Ingest Event 2" "200")
    EVENT_ID2=$(echo "$RESPONSE" | grep -o '"eventId":"[^"]*"' | head -1 | cut -d'"' -f4)
    echo "  → Created Event ID: $EVENT_ID2"
fi

# List all Events
test_endpoint "GET" "/api/events" "" "List all Events" "200"

# ============================================
# 5. DLQ APIs
# ============================================
echo ""
echo -e "${BLUE}5. DEAD LETTER QUEUE (DLQ) APIs${NC}"
echo "" >> $TEST_RESULTS
echo "5. DEAD LETTER QUEUE (DLQ) APIs" >> $TEST_RESULTS

# List DLQ entries
RESPONSE=$(test_endpoint "GET" "/api/admin/dlq" "" "List DLQ entries" "200")
DLQ_ID=$(echo "$RESPONSE" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

# Delete DLQ entry (if exists)
if [ ! -z "$DLQ_ID" ] && [ "$DLQ_ID" != "" ]; then
    test_endpoint "DELETE" "/api/admin/dlq/$DLQ_ID" "" "Delete DLQ entry" "200"
fi

# ============================================
# 6. UNSUBSCRIBE API
# ============================================
echo ""
echo -e "${BLUE}6. UNSUBSCRIBE API${NC}"
echo "" >> $TEST_RESULTS
echo "6. UNSUBSCRIBE API" >> $TEST_RESULTS

if [ ! -z "$SUB_ID2" ]; then
    test_endpoint "POST" "/api/subscribers/$SUB_ID2/unsubscribe" "" "Unsubscribe Subscription 2" "200"
fi

# ============================================
# 7. ERROR CASES
# ============================================
echo ""
echo -e "${BLUE}7. ERROR CASE TESTS${NC}"
echo "" >> $TEST_RESULTS
echo "7. ERROR CASE TESTS" >> $TEST_RESULTS

# Invalid client ID
test_endpoint "POST" "/api/subscribers" \
    '{"clientId": "invalid-client-xyz", "eventTypeId": "invalid-event-xyz", "webhookUrl": "http://localhost:9090/webhook"}' \
    "Create Subscription with invalid clientId (should fail)" "404"

# Invalid event type ID
if [ ! -z "$CLIENT_ID1" ]; then
    test_endpoint "POST" "/api/subscribers" \
        "{\"clientId\": \"$CLIENT_ID1\", \"eventTypeId\": \"invalid-event-xyz\", \"webhookUrl\": \"http://localhost:9090/webhook\"}" \
        "Create Subscription with invalid eventTypeId (should fail)" "404"
fi

# Update non-existent subscription
test_endpoint "PUT" "/api/subscribers/non-existent-sub-xyz" \
    '{"eventTypeId": "some-event"}' \
    "Update non-existent subscription (should fail)" "400"

# Delete non-existent event type
test_endpoint "DELETE" "/api/admin/event-types/non-existent-id-xyz" "" "Delete non-existent EventType (should fail)" "404"

# Delete non-existent DLQ entry
test_endpoint "DELETE" "/api/admin/dlq/non-existent-dlq-xyz" "" "Delete non-existent DLQ entry (should fail)" "404"

# ============================================
# SUMMARY
# ============================================
echo ""
echo "========================================="
echo -e "${BLUE}TEST SUMMARY${NC}"
echo "========================================="
echo "" >> $TEST_RESULTS
echo "=========================================" >> $TEST_RESULTS
echo "TEST SUMMARY" >> $TEST_RESULTS
echo "=========================================" >> $TEST_RESULTS
echo -e "${GREEN}Tests Passed: $PASSED${NC}"
echo -e "${RED}Tests Failed: $FAILED${NC}"
TOTAL=$((PASSED + FAILED))
if [ $TOTAL -gt 0 ]; then
    PERCENTAGE=$((PASSED * 100 / TOTAL))
    echo "Success Rate: $PERCENTAGE%"
    echo "Total Tests: $TOTAL"
    echo "Tests Passed: $PASSED" >> $TEST_RESULTS
    echo "Tests Failed: $FAILED" >> $TEST_RESULTS
    echo "Success Rate: $PERCENTAGE%" >> $TEST_RESULTS
    echo "Total Tests: $TOTAL" >> $TEST_RESULTS
fi
echo "========================================="
echo ""
echo "Detailed results saved to: $TEST_RESULTS"
