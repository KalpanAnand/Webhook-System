package com.example.webhook.controller;

import com.example.webhook.model.dto.ClientDto;
import com.example.webhook.service.ClientService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@Slf4j
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    public ResponseEntity<ClientDto.Response> registerClient(
            @RequestBody ClientDto.CreateRequest request) {

        log.info("Received Client Registration Request: {}", request);

        ClientDto.Response response = clientService.registerClient(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ClientDto.Response>> listClients() {
        log.info("Listing all clients");
        List<ClientDto.Response> clients = clientService.listClients();
        return ResponseEntity.ok(clients);
    }
}