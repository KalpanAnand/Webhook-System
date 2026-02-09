#!/bin/bash
set -e

echo "Creating EventType..."
curl -v -X POST http://localhost:8080/api/admin/event-types \
  -H "Content-Type: application/json" \
  -d '{"eventName": "order.created", "description": "Order created event"}'
echo ""

echo "Creating Client..."
curl -v -X POST http://localhost:8080/api/clients \
  -H "Content-Type: application/json" \
  -d '{"organizationName": "TestOrg", "contactEmail": "test@example.com"}' > client.json
echo ""
cat client.json
echo ""

CLIENT_ID=$(grep -o '"clientId":"[^"]*"' client.json | cut -d":" -f2 | tr -d '"')
echo "Client ID: $CLIENT_ID"

echo "Creating Subscription..."
curl -v -X POST http://localhost:8080/api/subscriptions \
  -H "Content-Type: application/json" \
  -d "{\"clientId\": \"$CLIENT_ID\", \"eventTypeId\": \"order.created\", \"webhookUrl\": \"http://localhost:9090/webhook\"}"
echo ""

echo "Ingesting Event..."
curl -v -X POST "http://localhost:8080/api/events?eventTypeId=order.created" \
  -H "Content-Type: text/plain" \
  -d '{"orderId": "123"}'
echo ""

echo "Waiting for dispatcher..."
sleep 7
