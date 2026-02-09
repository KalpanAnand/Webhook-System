#!/bin/bash
set -e

BASE_URL="http://localhost:8080"
echo "========================================="
echo "Testing Webhook System APIs"
echo "========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test counter
TESTS_PASSED=0
TESTS_FAILED=0

test_api() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4
    local expected_status=${5:-200}
    
    echo -e "${YELLOW}Testing: $description${NC}"
    echo -e "${BLUE}  $method $endpoint${NC}"
    
    if [ -z "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X $method "$BASE_URL$endpoint" -H "Content-Type: application/json" 2>&1)
    else
        response=$(curl -s -w "\n%{http_code}" -X $method "$BASE_URL$endpoint" -H "Content-Type: application/json" -d "$data" 2>&1)
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" -eq "$expected_status" ]; then
        echo -e "${GREEN}✓ PASSED (HTTP $http_code)${NC}"
        echo "  Response: $(echo "$body" | head -c 300)"
        if [ ${#body} -gt 300 ]; then
            echo "... (truncated)"
        fi
        echo ""
        ((TESTS_PASSED++))
        echo "$body"
    else
        echo -e "${RED}✗ FAILED (Expected HTTP $expected_status, got $http_code)${NC}"
        echo "  Response: $body"
        ((TESTS_FAILED++))
        echo ""
    fi
    echo "---"
}

# Helper to extract JSON value
extract_json_value() {
    local json=$1
    local key=$2
    echo "$json" | grep -o "\"$key\":\"[^\"]*\"" | head -1 | cut -d'"' -f4
}

# 1. Test Admin EventType APIs
echo "========================================="
echo "1. ADMIN EVENT TYPE APIs"
echo "========================================="

# Create EventType 1
RESPONSE=$(test_api "POST" "/api/admin/event-types" '{"eventName": "test.order.created", "description": "Test order created event"}' "Create EventType - test.order.created" "201")
EVENT_TYPE_ID1=$(extract_json_value "$RESPONSE" "eventTypeId")
echo "Created EventType ID: $EVENT_TYPE_ID1"
echo ""

# Create EventType 2
RESPONSE=$(test_api "POST" "/api/admin/event-types" '{"eventName": "test.payment.processed", "description": "Test payment processed event"}' "Create EventType - test.payment.processed" "201")
EVENT_TYPE_ID2=$(extract_json_value "$RESPONSE" "eventTypeId")
echo "Created EventType ID: $EVENT_TYPE_ID2"
echo ""

# List all EventTypes
test_api "GET" "/api/admin/event-types" "" "List all EventTypes" "200"

# Update EventType
if [ ! -z "$EVENT_TYPE_ID1" ] && [ "$EVENT_TYPE_ID1" != "" ]; then
    test_api "PUT" "/api/admin/event-types/$EVENT_TYPE_ID1" '{"description": "Updated description for test.order.created"}' "Update EventType" "200"
fi

# 2. Test Client APIs
echo ""
echo "========================================="
echo "2. CLIENT APIs"
echo "========================================="

# Register Client 1
RESPONSE=$(test_api "POST" "/api/clients" '{"organizationName": "TestOrg1", "contactEmail": "test1@example.com"}' "Register Client 1" "201")
CLIENT_ID1=$(extract_json_value "$RESPONSE" "clientId")
echo "Created Client ID: $CLIENT_ID1"
echo ""

# Register Client 2
RESPONSE=$(test_api "POST" "/api/clients" '{"organizationName": "TestOrg2", "contactEmail": "test2@example.com"}' "Register Client 2" "201")
CLIENT_ID2=$(extract_json_value "$RESPONSE" "clientId")
echo "Created Client ID: $CLIENT_ID2"
echo ""

# 3. Test Subscription APIs
echo ""
echo "========================================="
echo "3. SUBSCRIPTION APIs"
echo "========================================="

# Create Subscription 1
if [ ! -z "$CLIENT_ID1" ] && [ ! -z "$EVENT_TYPE_ID1" ] && [ "$CLIENT_ID1" != "" ] && [ "$EVENT_TYPE_ID1" != "" ]; then
    RESPONSE=$(test_api "POST" "/api/subscribers" "{\"clientId\": \"$CLIENT_ID1\", \"eventTypeId\": \"$EVENT_TYPE_ID1\", \"webhookUrl\": \"http://localhost:9090/webhook1\"}" "Create Subscription 1" "200")
    SUB_ID1=$(extract_json_value "$RESPONSE" "subscriptionId")
    echo "Created Subscription ID: $SUB_ID1"
    echo ""
fi

# Create Subscription 2
if [ ! -z "$CLIENT_ID2" ] && [ ! -z "$EVENT_TYPE_ID2" ] && [ "$CLIENT_ID2" != "" ] && [ "$EVENT_TYPE_ID2" != "" ]; then
    RESPONSE=$(test_api "POST" "/api/subscribers" "{\"clientId\": \"$CLIENT_ID2\", \"eventTypeId\": \"$EVENT_TYPE_ID2\", \"webhookUrl\": \"http://localhost:9090/webhook2\"}" "Create Subscription 2" "200")
    SUB_ID2=$(extract_json_value "$RESPONSE" "subscriptionId")
    echo "Created Subscription ID: $SUB_ID2"
    echo ""
fi

# List all Subscriptions
test_api "GET" "/api/subscribers" "" "List all Subscriptions" "200"

# List unsubscribed for a client
if [ ! -z "$CLIENT_ID1" ] && [ "$CLIENT_ID1" != "" ]; then
    test_api "GET" "/api/subscribers/$CLIENT_ID1/unsubscribed" "" "List unsubscribed for Client 1" "200"
fi

# Update Subscription
if [ ! -z "$SUB_ID1" ] && [ ! -z "$EVENT_TYPE_ID2" ] && [ "$SUB_ID1" != "" ] && [ "$EVENT_TYPE_ID2" != "" ]; then
    test_api "PUT" "/api/subscribers/$SUB_ID1" "{\"eventTypeId\": \"$EVENT_TYPE_ID2\"}" "Update Subscription 1" "200"
fi

# 4. Test Event APIs
echo ""
echo "========================================="
echo "4. EVENT APIs"
echo "========================================="

# Ingest Event 1
if [ ! -z "$EVENT_TYPE_ID1" ] && [ "$EVENT_TYPE_ID1" != "" ]; then
    RESPONSE=$(test_api "POST" "/api/events" "{\"eventTypeId\": \"$EVENT_TYPE_ID1\", \"payload\": {\"orderId\": \"123\", \"amount\": 99.99, \"customer\": \"John Doe\"}}" "Ingest Event 1" "200")
    EVENT_ID1=$(extract_json_value "$RESPONSE" "eventId")
    echo "Created Event ID: $EVENT_ID1"
    echo ""
fi

# Ingest Event 2
if [ ! -z "$EVENT_TYPE_ID2" ] && [ "$EVENT_TYPE_ID2" != "" ]; then
    RESPONSE=$(test_api "POST" "/api/events" "{\"eventTypeId\": \"$EVENT_TYPE_ID2\", \"payload\": {\"paymentId\": \"pay-456\", \"amount\": 199.99, \"status\": \"completed\"}}" "Ingest Event 2" "200")
    EVENT_ID2=$(extract_json_value "$RESPONSE" "eventId")
    echo "Created Event ID: $EVENT_ID2"
    echo ""
fi

# List all Events
test_api "GET" "/api/events" "" "List all Events" "200"

# 5. Test DLQ API
echo ""
echo "========================================="
echo "5. DEAD LETTER QUEUE (DLQ) APIs"
echo "========================================="

# List DLQ entries
RESPONSE=$(test_api "GET" "/api/admin/dlq" "" "List DLQ entries" "200")
DLQ_ID=$(echo "$RESPONSE" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4 || echo "")

# Delete DLQ entry (if exists)
if [ ! -z "$DLQ_ID" ] && [ "$DLQ_ID" != "" ]; then
    test_api "DELETE" "/api/admin/dlq/$DLQ_ID" "" "Delete DLQ entry" "200"
fi

# 6. Test Unsubscribe
echo ""
echo "========================================="
echo "6. UNSUBSCRIBE API"
echo "========================================="

if [ ! -z "$SUB_ID2" ] && [ "$SUB_ID2" != "" ]; then
    test_api "POST" "/api/subscribers/$SUB_ID2/unsubscribe" "" "Unsubscribe Subscription 2" "200"
fi

# 7. Test Error Cases (these should fail with appropriate status codes)
echo ""
echo "========================================="
echo "7. ERROR CASE TESTS (Expected Failures)"
echo "========================================="

# Try to create subscription with invalid clientId
test_api "POST" "/api/subscribers" '{"clientId": "invalid-client-999", "eventTypeId": "invalid-event-999", "webhookUrl": "http://localhost:9090/webhook"}' "Create Subscription with invalid IDs (should fail)" "404"

# Try to update non-existent subscription
test_api "PUT" "/api/subscribers/non-existent-sub-999" '{"eventTypeId": "some-event"}' "Update non-existent subscription (should fail)" "400"

# Try to delete non-existent event type
test_api "DELETE" "/api/admin/event-types/non-existent-id-999" "" "Delete non-existent EventType (should fail)" "404"

# Try to delete non-existent DLQ entry
test_api "DELETE" "/api/admin/dlq/non-existent-dlq-999" "" "Delete non-existent DLQ entry (should fail)" "404"

# Summary
echo ""
echo "========================================="
echo "TEST SUMMARY"
echo "========================================="
echo -e "${GREEN}Tests Passed: $TESTS_PASSED${NC}"
echo -e "${RED}Tests Failed: $TESTS_FAILED${NC}"
TOTAL=$((TESTS_PASSED + TESTS_FAILED))
if [ $TOTAL -gt 0 ]; then
    PERCENTAGE=$((TESTS_PASSED * 100 / TOTAL))
    echo "Success Rate: $PERCENTAGE%"
fi
echo "========================================="
