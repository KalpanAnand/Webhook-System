# Webhook Dispatching System

A robust webhook delivery system built with Spring Boot and SQLite.

## Features
- **Event Ingestion**: API to ingest events.
- **Subscription Management**: Register clients and subscribe to event types.
- **Reliable Delivery**: Dispatcher worker with exponential backoff and retries.
- **Resilience**: Handles network failures and ensures delivery attempts.

## Getting Started

### Prerequisites
- Java 17+
- Maven

### Running the Application
1. Run with Maven:
   ```sh
   ./mvnw spring-boot:run
   ```
2. The application will start on `http://localhost:8080`.

### API Endpoints
- **POST** `/api/clients`: Register a new client.
- **POST** `/api/admin/event-types`: Create event types (Admin).
- **POST** `/api/subscriptions`: Subscribe to events.
- **POST** `/api/events`: Ingest an event.

## Testing
Run unit tests:
```sh
./mvnw test
```

## Architecture
- **API Layer**: Controllers for Clients, Subscriptions, and Events.
- **Service Layer**: Business logic for ingestion.
- **Worker**: Scheduled task (`DispatcherService`) for processing delivery attempts.
- **Database**: SQLite for simple, file-based persistence.
