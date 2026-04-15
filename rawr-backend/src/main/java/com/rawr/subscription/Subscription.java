package com.rawr.subscription;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String unsubscribeToken = UUID.randomUUID().toString().replace("-", "");

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Subscription() {}

    public Subscription(String email) { this.email = email; }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getUnsubscribeToken() { return unsubscribeToken; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
