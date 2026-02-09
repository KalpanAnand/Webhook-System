package com.example.webhook.controller;

import com.example.webhook.model.Subscription;
import com.example.webhook.model.dto.SubscriptionDto;
import com.example.webhook.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/subscribers")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody SubscriptionDto.CreateRequest request) {
        Subscription sub = subscriptionService.createSubscription(request);
        return ResponseEntity.ok(new SubscriptionDto.Response(sub));
    }

    @PutMapping("/{subscriptionId}")
    public ResponseEntity<?> update(@PathVariable String subscriptionId,
            @RequestBody SubscriptionDto.ModifyRequest request) {
        try {
            Subscription updated = subscriptionService.updateSubscription(subscriptionId, request);
            return ResponseEntity.ok(new SubscriptionDto.Response(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{subscriptionId}/unsubscribe")
    public ResponseEntity<?> unsubscribe(@PathVariable String subscriptionId) {
        Subscription sub = subscriptionService.unsubscribe(subscriptionId);
        return ResponseEntity.ok(new SubscriptionDto.Response(sub));
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionDto.Response>> list() {
        List<SubscriptionDto.Response> list = subscriptionService.listSubscribers()
                .stream()
                .map(SubscriptionDto.Response::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{clientId}/unsubscribed")
    public ResponseEntity<List<SubscriptionDto.Response>> listUnsubscribed(@PathVariable String clientId) {
        List<SubscriptionDto.Response> list = subscriptionService.listUnsubscribed(clientId)
                .stream()
                .map(SubscriptionDto.Response::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }
}
