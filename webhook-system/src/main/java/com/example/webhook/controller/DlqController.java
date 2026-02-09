package com.example.webhook.controller;

import com.example.webhook.model.DeadLetterQueue;
import com.example.webhook.repository.DeadLetterQueueRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dlq")
public class DlqController {

    private final DeadLetterQueueRepository dlqRepository;

    public DlqController(DeadLetterQueueRepository dlqRepository) {
        this.dlqRepository = dlqRepository;
    }

    @GetMapping
    public List<DeadLetterQueue> list() {
        return dlqRepository.findAll();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (dlqRepository.existsById(id)) {
            dlqRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
