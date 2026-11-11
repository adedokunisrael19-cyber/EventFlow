package com.eventflow.auth;

import com.eventflow.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "external_identities", uniqueConstraints = {
        @UniqueConstraint(name = "uk_external_identities_provider_subject", columnNames = {"provider", "provider_subject"}),
        @UniqueConstraint(name = "uk_external_identities_user_provider", columnNames = {"user_id", "provider"})
})
public class ExternalIdentity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(name = "provider_subject", nullable = false, length = 255)
    private String providerSubject;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ExternalIdentity() {
    }

    public ExternalIdentity(User user, String provider, String providerSubject) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.provider = provider;
        this.providerSubject = providerSubject;
        this.createdAt = Instant.now();
    }

    public User getUser() { return user; }
    public String getProvider() { return provider; }
    public String getProviderSubject() { return providerSubject; }
}
