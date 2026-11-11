CREATE TABLE external_identities
(
    id                UUID         NOT NULL PRIMARY KEY,
    user_id           UUID         NOT NULL REFERENCES users (id),
    provider          VARCHAR(32)  NOT NULL,
    provider_subject  VARCHAR(255) NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_external_identities_provider_subject UNIQUE (provider, provider_subject),
    CONSTRAINT uk_external_identities_user_provider UNIQUE (user_id, provider),
    CONSTRAINT ck_external_identities_provider CHECK (provider IN ('GOOGLE'))
);
