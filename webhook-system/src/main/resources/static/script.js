const API_BASE_URL = 'http://localhost:8080/api';

// Navigation
document.querySelectorAll('.nav-item').forEach(item => {
    item.addEventListener('click', (e) => {
        e.preventDefault();
        const section = item.getAttribute('data-section');
        showSection(section);
        
        document.querySelectorAll('.nav-item').forEach(nav => nav.classList.remove('active'));
        item.classList.add('active');
    });
});

function showSection(sectionId) {
    document.querySelectorAll('.content-section').forEach(section => {
        section.classList.remove('active');
    });
    document.getElementById(sectionId).classList.add('active');
    
    // Load data for the section
    switch(sectionId) {
        case 'dashboard':
            loadDashboard();
            break;
        case 'event-types':
            loadEventTypes();
            break;
        case 'clients':
            loadClients();
            break;
        case 'subscriptions':
            loadSubscriptions();
            break;
        case 'events':
            loadEvents();
            break;
        case 'webhook-deliveries':
            loadWebhookDeliveries();
            break;
        case 'dlq':
            loadDLQ();
            break;
    }
}

// Modal Functions
function showModal(modalId) {
    document.getElementById(modalId).classList.add('active');
}

function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('active');
}

// Close modal on outside click
document.querySelectorAll('.modal').forEach(modal => {
    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            modal.classList.remove('active');
        }
    });
});

// Toast Notification
function showToast(message, type = 'info') {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = `toast ${type} active`;
    
    setTimeout(() => {
        toast.classList.remove('active');
    }, 3000);
}

// API Functions
async function apiCall(endpoint, method = 'GET', body = null) {
    try {
        const options = {
            method,
            headers: {
                'Content-Type': 'application/json',
            }
        };
        
        if (body) {
            options.body = JSON.stringify(body);
        }
        
        const response = await fetch(`${API_BASE_URL}${endpoint}`, options);
        const data = await response.json();
        
        if (!response.ok) {
            throw new Error(data.message || `HTTP error! status: ${response.status}`);
        }
        
        return data;
    } catch (error) {
        console.error('API Error:', error);
        throw error;
    }
}

// Dashboard
async function loadDashboard() {
    try {
        const [eventTypes, clients, subscriptions, events] = await Promise.all([
            apiCall('/admin/event-types'),
            apiCall('/clients'),
            apiCall('/subscribers'),
            apiCall('/events')
        ]);
        
        document.getElementById('stat-event-types').textContent = eventTypes.length || 0;
        document.getElementById('stat-clients').textContent = clients.length || 0;
        document.getElementById('stat-subscriptions').textContent = 
            subscriptions.filter(s => s.status === 'ACTIVE').length || 0;
        document.getElementById('stat-events').textContent = events.length || 0;
    } catch (error) {
        console.error('Error loading dashboard:', error);
        // Set defaults if API fails
        document.getElementById('stat-event-types').textContent = '0';
        document.getElementById('stat-clients').textContent = '0';
        document.getElementById('stat-subscriptions').textContent = '0';
        document.getElementById('stat-events').textContent = '0';
    }
}

// Event Types
async function loadEventTypes() {
    try {
        const eventTypes = await apiCall('/admin/event-types');
        const tbody = document.getElementById('event-types-table');
        
        if (eventTypes.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="loading">No event types found</td></tr>';
            return;
        }
        
        tbody.innerHTML = eventTypes.map(et => `
            <tr>
                <td>${et.eventTypeId}</td>
                <td><strong>${et.eventName}</strong></td>
                <td>${et.description || 'N/A'}</td>
                <td>
                    <button class="btn btn-sm btn-primary" onclick="editEventType('${et.eventTypeId}', '${et.eventName}', '${(et.description || '').replace(/'/g, "\\'")}')">
                        <i class="fas fa-edit"></i> Edit
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="deleteEventType('${et.eventTypeId}')">
                        <i class="fas fa-trash"></i> Delete
                    </button>
                </td>
            </tr>
        `).join('');
    } catch (error) {
        document.getElementById('event-types-table').innerHTML = 
            '<tr><td colspan="4" class="loading">Error loading event types</td></tr>';
        showToast('Error loading event types', 'error');
    }
}

function showEventTypeModal() {
    document.getElementById('event-type-form').reset();
    showModal('event-type-modal');
}

async function createEventType(e) {
    e.preventDefault();
    const formData = new FormData(e.target);
    const data = {
        eventName: formData.get('eventName'),
        description: formData.get('description')
    };
    
    try {
        await apiCall('/admin/event-types', 'POST', data);
        showToast('Event type created successfully', 'success');
        closeModal('event-type-modal');
        loadEventTypes();
        loadDashboard();
    } catch (error) {
        showToast(error.message || 'Error creating event type', 'error');
    }
}

function editEventType(id, name, description) {
    document.getElementById('update-event-type-id').value = id;
    document.getElementById('update-event-name').value = name;
    document.getElementById('update-event-description').value = description;
    showModal('update-event-type-modal');
}

async function updateEventType(e) {
    e.preventDefault();
    const formData = new FormData(e.target);
    const id = formData.get('eventTypeId');
    const data = {
        description: formData.get('description')
    };
    
    try {
        await apiCall(`/admin/event-types/${id}`, 'PUT', data);
        showToast('Event type updated successfully', 'success');
        closeModal('update-event-type-modal');
        loadEventTypes();
    } catch (error) {
        showToast(error.message || 'Error updating event type', 'error');
    }
}

async function deleteEventType(id) {
    if (!confirm('Are you sure you want to delete this event type?')) {
        return;
    }
    
    try {
        await apiCall(`/admin/event-types/${id}`, 'DELETE');
        showToast('Event type deleted successfully', 'success');
        loadEventTypes();
        loadDashboard();
    } catch (error) {
        showToast(error.message || 'Error deleting event type', 'error');
    }
}

// Clients
async function loadClients() {
    try {
        const clients = await apiCall('/clients');
        const tbody = document.getElementById('clients-table');
        
        if (clients.length === 0) {
            tbody.innerHTML = '<tr><td colspan="3" class="loading">No clients registered yet</td></tr>';
            return;
        }
        
        tbody.innerHTML = clients.map(client => `
            <tr>
                <td><strong>${client.clientId}</strong></td>
                <td>${client.organizationName}</td>
                <td>${client.contactEmail}</td>
            </tr>
        `).join('');
    } catch (error) {
        document.getElementById('clients-table').innerHTML = 
            '<tr><td colspan="3" class="loading">Error loading clients</td></tr>';
        showToast('Error loading clients', 'error');
    }
}

function showClientModal() {
    document.getElementById('client-form').reset();
    showModal('client-modal');
}

async function createClient(e) {
    e.preventDefault();
    const formData = new FormData(e.target);
    const data = {
        organizationName: formData.get('organizationName'),
        contactEmail: formData.get('contactEmail')
    };
    
    try {
        const response = await apiCall('/clients', 'POST', data);
        showToast(`Client registered successfully! Client ID: ${response.clientId}`, 'success');
        closeModal('client-modal');
        loadClients();
        loadDashboard();
        // Refresh subscription selects
        loadSubscriptionSelects();
    } catch (error) {
        showToast(error.message || 'Error registering client', 'error');
    }
}

// Subscriptions
async function loadSubscriptions() {
    try {
        const subscriptions = await apiCall('/subscribers');
        const tbody = document.getElementById('subscriptions-table');
        
        if (subscriptions.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="loading">No subscriptions found</td></tr>';
            return;
        }
        
        tbody.innerHTML = subscriptions.map(sub => `
            <tr>
                <td>${sub.subscriptionId}</td>
                <td>${sub.clientId}</td>
                <td>${sub.eventTypeId}</td>
                <td><span class="json-payload" title="${sub.webhookUrl}">${sub.webhookUrl}</span></td>
                <td><span class="status-badge ${sub.status.toLowerCase()}">${sub.status}</span></td>
                <td>
                    ${sub.status === 'ACTIVE' ? `
                        <button class="btn btn-sm btn-primary" onclick="editSubscription('${sub.subscriptionId}', '${sub.eventTypeId}')">
                            <i class="fas fa-edit"></i> Edit
                        </button>
                        <button class="btn btn-sm btn-danger" onclick="unsubscribe('${sub.subscriptionId}')">
                            <i class="fas fa-ban"></i> Unsubscribe
                        </button>
                    ` : `
                        <span class="text-muted">Inactive</span>
                    `}
                </td>
            </tr>
        `).join('');
    } catch (error) {
        document.getElementById('subscriptions-table').innerHTML = 
            '<tr><td colspan="6" class="loading">Error loading subscriptions</td></tr>';
        showToast('Error loading subscriptions', 'error');
    }
}

async function loadSubscriptionSelects() {
    try {
        // Load clients and event types for dropdowns
        const [eventTypes] = await Promise.all([
            apiCall('/admin/event-types')
        ]);
        
        // Populate event type selects
        const eventSelects = [
            document.getElementById('subscription-event-select'),
            document.getElementById('update-subscription-event'),
            document.getElementById('event-type-select')
        ];
        
        eventSelects.forEach(select => {
            if (select) {
                const currentValue = select.value;
                select.innerHTML = '<option value="">Select an event type...</option>' +
                    eventTypes.map(et => 
                        `<option value="${et.eventTypeId}">${et.eventName} (${et.eventTypeId})</option>`
                    ).join('');
                if (currentValue) select.value = currentValue;
            }
        });
        
        // For client select, we'll need to handle it differently since there's no GET endpoint
        // We'll show a text input instead or fetch from a different source
    } catch (error) {
        console.error('Error loading selects:', error);
    }
}

function showSubscriptionModal() {
    document.getElementById('subscription-form').reset();
    loadSubscriptionSelects();
    showModal('subscription-modal');
}

async function createSubscription(e) {
    e.preventDefault();
    const formData = new FormData(e.target);
    const data = {
        clientId: formData.get('clientId'),
        eventTypeId: formData.get('eventTypeId'),
        webhookUrl: formData.get('webhookUrl')
    };
    
    try {
        await apiCall('/subscribers', 'POST', data);
        showToast('Subscription created successfully', 'success');
        closeModal('subscription-modal');
        loadSubscriptions();
        loadDashboard();
    } catch (error) {
        showToast(error.message || 'Error creating subscription', 'error');
    }
}

function editSubscription(id, currentEventTypeId) {
    document.getElementById('update-subscription-id').value = id;
    loadSubscriptionSelects().then(() => {
        document.getElementById('update-subscription-event').value = currentEventTypeId;
    });
    showModal('update-subscription-modal');
}

async function updateSubscription(e) {
    e.preventDefault();
    const formData = new FormData(e.target);
    const id = formData.get('subscriptionId');
    const data = {
        eventTypeId: formData.get('eventTypeId')
    };
    
    try {
        await apiCall(`/subscribers/${id}`, 'PUT', data);
        showToast('Subscription updated successfully', 'success');
        closeModal('update-subscription-modal');
        loadSubscriptions();
    } catch (error) {
        showToast(error.message || 'Error updating subscription', 'error');
    }
}

async function unsubscribe(id) {
    if (!confirm('Are you sure you want to unsubscribe?')) {
        return;
    }
    
    try {
        await apiCall(`/subscribers/${id}/unsubscribe`, 'POST');
        showToast('Unsubscribed successfully', 'success');
        loadSubscriptions();
        loadDashboard();
    } catch (error) {
        showToast(error.message || 'Error unsubscribing', 'error');
    }
}

// Events
async function loadEvents() {
    try {
        const events = await apiCall('/events');
        const tbody = document.getElementById('events-table');
        
        if (events.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="loading">No events found</td></tr>';
            return;
        }
        
        tbody.innerHTML = events.map(evt => {
            let payload = evt.payload;
            try {
                const parsed = JSON.parse(payload);
                payload = JSON.stringify(parsed, null, 2);
            } catch (e) {
                // Keep as is if not valid JSON
            }
            
            return `
                <tr>
                    <td>${evt.eventId}</td>
                    <td>${evt.eventTypeId}</td>
                    <td><span class="json-payload" title="${payload}">${payload.substring(0, 50)}${payload.length > 50 ? '...' : ''}</span></td>
                    <td>${new Date(evt.receivedAt).toLocaleString()}</td>
                </tr>
            `;
        }).join('');
    } catch (error) {
        document.getElementById('events-table').innerHTML = 
            '<tr><td colspan="4" class="loading">Error loading events</td></tr>';
        showToast('Error loading events', 'error');
    }
}

function showEventModal() {
    document.getElementById('event-form').reset();
    loadSubscriptionSelects();
    showModal('event-modal');
}

async function ingestEvent(e) {
    e.preventDefault();
    const formData = new FormData(e.target);
    const eventTypeId = formData.get('eventTypeId');
    let payload = formData.get('payload');
    
    // Try to parse and re-stringify to ensure valid JSON
    try {
        const parsed = JSON.parse(payload);
        payload = parsed;
    } catch (error) {
        showToast('Invalid JSON payload', 'error');
        return;
    }
    
    const data = {
        eventTypeId: eventTypeId,
        payload: payload
    };
    
    try {
        await apiCall('/events', 'POST', data);
        showToast('Event ingested successfully', 'success');
        closeModal('event-modal');
        loadEvents();
        loadDashboard();
    } catch (error) {
        showToast(error.message || 'Error ingesting event', 'error');
    }
}

// Webhook Deliveries
async function loadWebhookDeliveries() {
    try {
        const deliveries = await apiCall('/admin/webhook-deliveries');
        const tbody = document.getElementById('webhook-deliveries-table');
        
        if (deliveries.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="loading">No webhook deliveries found</td></tr>';
            return;
        }
        
        tbody.innerHTML = deliveries.map(delivery => {
            const status = delivery.status || 'UNKNOWN';
            const statusClass = status === 'SUCCESS' ? 'active' : 
                              status === 'FAILED' ? 'inactive' : '';
            const nextAttempt = delivery.nextAttemptAt ? 
                new Date(delivery.nextAttemptAt).toLocaleString() : 'N/A';
            const createdAt = delivery.createdAt ? 
                new Date(delivery.createdAt).toLocaleString() : 'N/A';
            const lastAttempt = delivery.lastAttemptAt ? 
                new Date(delivery.lastAttemptAt).toLocaleString() : 'N/A';
            const lastAttemptStatus = delivery.lastAttemptStatus || 'N/A';
            const lastAttemptError = delivery.lastAttemptError ? 
                `<span class="json-payload" title="${delivery.lastAttemptError}">${delivery.lastAttemptError.substring(0, 30)}${delivery.lastAttemptError.length > 30 ? '...' : ''}</span>` : 
                'N/A';
            
            return `
                <tr>
                    <td>${delivery.id}</td>
                    <td>${delivery.eventId}</td>
                    <td>${delivery.subscriptionId}</td>
                    <td><span class="status-badge ${statusClass}">${status}</span></td>
                    <td>${delivery.attemptCount || 0}</td>
                    <td>
                        ${lastAttempt !== 'N/A' ? `${lastAttempt}<br><small>Status: ${lastAttemptStatus}</small>` : 'N/A'}
                        ${delivery.lastAttemptError ? `<br><small style="color: var(--danger-color);">${lastAttemptError}</small>` : ''}
                    </td>
                    <td>${nextAttempt}</td>
                    <td>${createdAt}</td>
                </tr>
            `;
        }).join('');
    } catch (error) {
        document.getElementById('webhook-deliveries-table').innerHTML = 
            '<tr><td colspan="8" class="loading">Error loading webhook deliveries</td></tr>';
        showToast('Error loading webhook deliveries', 'error');
    }
}

// DLQ
async function loadDLQ() {
    try {
        const dlqEntries = await apiCall('/admin/dlq');
        const tbody = document.getElementById('dlq-table');
        
        if (dlqEntries.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="loading">No DLQ entries found</td></tr>';
            return;
        }
        
        tbody.innerHTML = dlqEntries.map(entry => `
            <tr>
                <td>${entry.dlqId || entry.id}</td>
                <td>${entry.eventId}</td>
                <td>${entry.subscriptionId}</td>
                <td><span class="json-payload" title="${entry.failureReason || 'N/A'}">${(entry.failureReason || 'N/A').substring(0, 50)}${(entry.failureReason || '').length > 50 ? '...' : ''}</span></td>
                <td>${entry.addedAt ? new Date(entry.addedAt).toLocaleString() : 'N/A'}</td>
                <td>
                    <button class="btn btn-sm btn-danger" onclick="deleteDLQEntry('${entry.dlqId || entry.id}')">
                        <i class="fas fa-trash"></i> Delete
                    </button>
                </td>
            </tr>
        `).join('');
    } catch (error) {
        document.getElementById('dlq-table').innerHTML = 
            '<tr><td colspan="6" class="loading">Error loading DLQ entries</td></tr>';
        showToast('Error loading DLQ entries', 'error');
    }
}

async function deleteDLQEntry(id) {
    if (!confirm('Are you sure you want to delete this DLQ entry?')) {
        return;
    }
    
    try {
        await apiCall(`/admin/dlq/${id}`, 'DELETE');
        showToast('DLQ entry deleted successfully', 'success');
        loadDLQ();
    } catch (error) {
        showToast(error.message || 'Error deleting DLQ entry', 'error');
    }
}

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    loadDashboard();
    loadEventTypes();
    loadSubscriptions();
    loadEvents();
    loadDLQ();
    loadSubscriptionSelects();
    
    // Auto-refresh every 30 seconds
    setInterval(() => {
        const activeSection = document.querySelector('.content-section.active');
        if (activeSection) {
            const sectionId = activeSection.id;
            switch(sectionId) {
                case 'dashboard':
                    loadDashboard();
                    break;
                case 'event-types':
                    loadEventTypes();
                    break;
                case 'clients':
                    loadClients();
                    break;
                case 'subscriptions':
                    loadSubscriptions();
                    break;
                case 'events':
                    loadEvents();
                    break;
                case 'webhook-deliveries':
                    loadWebhookDeliveries();
                    break;
                case 'dlq':
                    loadDLQ();
                    break;
            }
        }
    }, 30000);
});
