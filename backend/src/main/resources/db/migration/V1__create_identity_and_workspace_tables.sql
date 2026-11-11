CREATE TABLE users
(
    id             UUID         NOT NULL PRIMARY KEY,
    email          VARCHAR(254) NOT NULL,
    full_name      VARCHAR(160) NOT NULL,
    password_hash  VARCHAR(255),
    email_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_users_email_case_insensitive ON users (LOWER(email));

CREATE TABLE workspaces
(
    id            UUID        NOT NULL PRIMARY KEY,
    business_name VARCHAR(160) NOT NULL,
    currency_code VARCHAR(3)   NOT NULL DEFAULT 'NGN',
    time_zone     VARCHAR(64)  NOT NULL DEFAULT 'Africa/Lagos',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_workspaces_currency_code_uppercase CHECK (currency_code = UPPER(currency_code))
);

CREATE TABLE workspace_members
(
    id           UUID        NOT NULL PRIMARY KEY,
    workspace_id UUID        NOT NULL REFERENCES workspaces (id),
    user_id      UUID        NOT NULL UNIQUE REFERENCES users (id),
    role         VARCHAR(32) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_workspace_members_role CHECK (role IN ('BUSINESS_OWNER', 'EVENT_MANAGER', 'STAFF'))
);
