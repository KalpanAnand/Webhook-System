package com.example.webhook.service;

import com.example.webhook.exception.ResourceNotFoundException;
import com.example.webhook.model.EventType;
import com.example.webhook.model.dto.EventTypeDto;
import com.example.webhook.repository.EventTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final EventTypeRepository eventTypeRepository;

    public AdminService(EventTypeRepository eventTypeRepository) {
        this.eventTypeRepository = eventTypeRepository;
    }

    @Transactional
    public EventTypeDto.Response createEventType(EventTypeDto.CreateRequest request) {
        Optional<EventType> existing = eventTypeRepository.findByEventName(request.getEventName());

        if (existing.isPresent()) {
            throw new com.example.webhook.exception.DuplicateResourceException(
                    "EventType already exists: " + request.getEventName());
        }

        EventType eventType = new EventType();
        eventType.setEventName(request.getEventName());
        eventType.setDescription(request.getDescription());

        EventType saved = eventTypeRepository.save(eventType);
        return new EventTypeDto.Response(saved, "EventType created successfully");
    }

    @Transactional
    public EventTypeDto.Response updateEventType(String id, EventTypeDto.UpdateRequest request) {
        EventType eventType = eventTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EventType not found: " + id));

        if (request.getDescription() != null) {
            eventType.setDescription(request.getDescription());
        }

        EventType updated = eventTypeRepository.save(eventType);
        return new EventTypeDto.Response(updated, "EventType updated successfully");
    }

    @Transactional
    public EventTypeDto.Response deleteEventType(String id) {
        EventType eventType = eventTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EventType not found: " + id));

        eventTypeRepository.delete(eventType);
        return new EventTypeDto.Response(eventType, "EventType deleted successfully");
    }

    public List<EventTypeDto.Response> listEventTypes() {
        return eventTypeRepository.findAll().stream()
                .map(EventTypeDto.Response::new)
                .collect(Collectors.toList());
    }
}
