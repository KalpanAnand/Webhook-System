package com.example.webhook.repository;

import com.example.webhook.model.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventTypeRepository extends JpaRepository<EventType, String> {
    Optional<EventType> findByEventName(String eventName);
}
