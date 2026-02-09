package com.example.webhook.service;

import com.example.webhook.model.Client;
import com.example.webhook.model.dto.ClientDto;
import com.example.webhook.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    @Transactional
    public ClientDto.Response registerClient(ClientDto.CreateRequest request) {
        // Check for duplicates
        if (clientRepository.findFirstByOrganizationName(request.getOrganizationName()).isPresent()) {
            throw new com.example.webhook.exception.DuplicateResourceException(
                    "Client already registered: " + request.getOrganizationName());
        }

        Client client = new Client();
        client.setOrganizationName(request.getOrganizationName());
        client.setContactEmail(request.getContactEmail());

        Client savedClient = clientRepository.save(client);

        return new ClientDto.Response(savedClient, "Client registered successfully");
    }

    public List<ClientDto.Response> listClients() {
        return clientRepository.findAll().stream()
                .map(ClientDto.Response::new)
                .collect(Collectors.toList());
    }
}
