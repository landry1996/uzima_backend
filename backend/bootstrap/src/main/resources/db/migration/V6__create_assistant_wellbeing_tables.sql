-- ============================================================
-- V6 : Assistant IA (reminders) + Bien-être numérique
-- ============================================================

-- ------------------------------------------------------------
-- reminders
-- ------------------------------------------------------------
CREATE TABLE reminders (
    id            UUID        NOT NULL PRIMARY KEY,
    user_id       UUID        NOT NULL REFERENCES users(id),
    content       VARCHAR(500) NOT NULL,
    trigger       VARCHAR(30) NOT NULL
        CHECK (trigger IN ('TIME_BASED', 'LOCATION_BASED', 'CONTEXT_BASED')),
    scheduled_at  TIMESTAMPTZ NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    status        VARCHAR(20) NOT NULL
        CHECK (status IN ('PENDING', 'TRIGGERED', 'SNOOZED', 'DISMISSED')),
    triggered_at  TIMESTAMPTZ,
    dismissed_at  TIMESTAMPTZ,
    snoozed_until TIMESTAMPTZ
);

CREATE INDEX idx_reminders_user_id        ON reminders (user_id);
CREATE INDEX idx_reminders_user_status    ON reminders (user_id, status);
CREATE INDEX idx_reminders_scheduled_at  ON reminders (scheduled_at)
    WHERE status IN ('PENDING', 'SNOOZED');

-- ------------------------------------------------------------
-- focus_sessions
-- ------------------------------------------------------------
CREATE TABLE focus_sessions (
    id                  UUID        NOT NULL PRIMARY KEY,
    user_id             UUID        NOT NULL REFERENCES users(id),
    started_at          TIMESTAMPTZ NOT NULL,
    status              VARCHAR(20) NOT NULL
        CHECK (status IN ('ACTIVE', 'COMPLETED', 'INTERRUPTED')),
    ended_at            TIMESTAMPTZ,
    interruption_reason VARCHAR(30)
        CHECK (interruption_reason IN (
            'NOTIFICATION', 'USER_CHOICE', 'EMERGENCY', 'TIMEOUT', 'INCOMING_CALL'
        ))
);

CREATE INDEX idx_focus_sessions_user_id     ON focus_sessions (user_id);
CREATE INDEX idx_focus_sessions_user_status ON focus_sessions (user_id, status);
CREATE INDEX idx_focus_sessions_started_at  ON focus_sessions (started_at);

-- ------------------------------------------------------------
-- usage_sessions
-- ------------------------------------------------------------
CREATE TABLE usage_sessions (
    id          UUID        NOT NULL PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users(id),
    app_name    VARCHAR(100) NOT NULL,
    app_type    VARCHAR(20) NOT NULL
        CHECK (app_type IN (
            'SOCIAL', 'MESSAGING', 'ENTERTAINMENT', 'PRODUCTIVITY', 'WORK', 'HEALTH', 'OTHER'
        )),
    started_at  TIMESTAMPTZ NOT NULL,
    ended_at    TIMESTAMPTZ
);

CREATE INDEX idx_usage_sessions_user_id    ON usage_sessions (user_id);
CREATE INDEX idx_usage_sessions_user_type  ON usage_sessions (user_id, app_type);
CREATE INDEX idx_usage_sessions_started_at ON usage_sessions (started_at);
