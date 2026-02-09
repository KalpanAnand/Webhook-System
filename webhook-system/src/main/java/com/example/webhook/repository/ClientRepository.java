package com.example.webhook.repository;

import com.example.webhook.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, String> {

    Optional<Client> findFirstByOrganizationName(String organizationName);
}
