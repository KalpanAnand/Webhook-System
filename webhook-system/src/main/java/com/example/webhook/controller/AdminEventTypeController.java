package com.example.webhook.controller;

import com.example.webhook.model.dto.EventTypeDto;
import com.example.webhook.service.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/event-types")
public class AdminEventTypeController {

    private final AdminService adminService;

    public AdminEventTypeController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping
    public ResponseEntity<EventTypeDto.Response> createEventType(@RequestBody EventTypeDto.CreateRequest request) {
        EventTypeDto.Response response = adminService.createEventType(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventTypeDto.Response> updateEventType(
            @PathVariable String id,
            @RequestBody EventTypeDto.UpdateRequest request) {
        EventTypeDto.Response response = adminService.updateEventType(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<EventTypeDto.Response> deleteEventType(@PathVariable String id) {
        EventTypeDto.Response response = adminService.deleteEventType(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<EventTypeDto.Response>> listEventTypes() {
        List<EventTypeDto.Response> all = adminService.listEventTypes();
        return ResponseEntity.ok(all);
    }
}