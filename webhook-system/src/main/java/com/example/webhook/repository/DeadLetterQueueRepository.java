package com.example.webhook.repository;

import com.example.webhook.model.DeadLetterQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeadLetterQueueRepository extends JpaRepository<DeadLetterQueue, String> {
}
