package com.eventflow.workspace;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workspaces")
public class Workspace {

    @Id
    private UUID id;

    @Column(name = "business_name", nullable = false, length = 160)
    private String businessName;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Workspace() {
    }

    public Workspace(String businessName) {
        this.id = UUID.randomUUID();
        this.businessName = businessName;
        this.currencyCode = "NGN";
        this.timeZone = "Africa/Lagos";
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getBusinessName() {
        return businessName;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
