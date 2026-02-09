# Webhook System - Frontend

A modern, professional web dashboard for managing the Webhook System.

## Features

- **Dashboard**: Overview with statistics for Event Types, Clients, Subscriptions, and Events
- **Event Types Management**: Create, view, update, and delete event types
- **Client Registration**: Register new clients with organization details
- **Subscription Management**: Create, update, and unsubscribe from event types
- **Event Ingestion**: Ingest new events with JSON payloads
- **Dead Letter Queue**: View and manage failed webhook deliveries

## Design

- **Modern Dark Theme**: Professional dark color scheme with gradient accents
- **Responsive Design**: Works on desktop, tablet, and mobile devices
- **Interactive UI**: Smooth animations and transitions
- **Real-time Updates**: Auto-refreshes data every 30 seconds
- **Toast Notifications**: User-friendly success/error messages

## Technology Stack

- **HTML5**: Semantic markup
- **CSS3**: Modern styling with CSS variables and animations
- **Vanilla JavaScript**: No framework dependencies
- **Font Awesome**: Icons for better UX

## Access

Once the Spring Boot application is running, access the frontend at:

```
http://localhost:8080/
```

or

```
http://localhost:8080/index.html
```

## File Structure

```
src/main/resources/static/
├── index.html      # Main HTML file
├── styles.css      # All styling
└── script.js       # All JavaScript logic and API calls
```

## API Integration

The frontend integrates with all backend APIs:

- `GET /api/admin/event-types` - List event types
- `POST /api/admin/event-types` - Create event type
- `PUT /api/admin/event-types/{id}` - Update event type
- `DELETE /api/admin/event-types/{id}` - Delete event type
- `POST /api/clients` - Register client
- `GET /api/subscribers` - List subscriptions
- `POST /api/subscribers` - Create subscription
- `PUT /api/subscribers/{id}` - Update subscription
- `POST /api/subscribers/{id}/unsubscribe` - Unsubscribe
- `GET /api/events` - List events
- `POST /api/events` - Ingest event
- `GET /api/admin/dlq` - List DLQ entries
- `DELETE /api/admin/dlq/{id}` - Delete DLQ entry

## Usage

1. **Dashboard**: View system statistics at a glance
2. **Event Types**: Create event types that can be subscribed to
3. **Clients**: Register organizations that will receive webhooks
4. **Subscriptions**: Connect clients to event types with webhook URLs
5. **Events**: Ingest events that will trigger webhook deliveries
6. **DLQ**: Monitor and manage failed webhook deliveries

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

## Notes

- The frontend uses CORS to communicate with the backend API
- All API calls are made to `http://localhost:8080/api`
- Data auto-refreshes every 30 seconds
- Toast notifications appear for 3 seconds
