-- Migration V3 : Création des tables circles et circle_memberships
-- Domaine Social — Sprint 3-4
-- Date : 2026-03-13

-- -------------------------------------------------------------------------
-- Table : circles
-- -------------------------------------------------------------------------
CREATE TABLE circles
(
    id                    UUID         NOT NULL,
    name                  VARCHAR(100) NOT NULL,
    type                  VARCHAR(20)  NOT NULL,
    owner_id              UUID         NOT NULL,

    -- CircleRule (value object embarqué)
    notification_policy   VARCHAR(20)  NOT NULL,
    visibility            VARCHAR(15)  NOT NULL,
    allows_voice_messages BOOLEAN      NOT NULL DEFAULT TRUE,
    allows_payments       BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at            TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_circles PRIMARY KEY (id),
    CONSTRAINT chk_circles_type   CHECK (type IN ('FAMILY', 'WORK', 'CLOSE_FRIENDS', 'PROJECT', 'COMMUNITY')),
    CONSTRAINT chk_circles_notif  CHECK (notification_policy IN ('IMMEDIATE', 'DEFERRED', 'URGENT_ONLY', 'BLOCKED')),
    CONSTRAINT chk_circles_visibility CHECK (visibility IN ('PUBLIC', 'CIRCLE_ONLY', 'PRIVATE'))
);

-- Index pour les requêtes courantes
CREATE INDEX idx_circles_owner_id   ON circles (owner_id);
CREATE INDEX idx_circles_type       ON circles (type);
CREATE INDEX idx_circles_created_at ON circles (created_at DESC);

-- -------------------------------------------------------------------------
-- Table : circle_memberships
-- -------------------------------------------------------------------------
CREATE TABLE circle_memberships
(
    id         UUID        NOT NULL,
    circle_id  UUID        NOT NULL,
    member_id  UUID        NOT NULL,
    role       VARCHAR(10) NOT NULL,
    joined_at  TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_circle_memberships            PRIMARY KEY (id),
    CONSTRAINT fk_memberships_circle            FOREIGN KEY (circle_id) REFERENCES circles (id) ON DELETE CASCADE,
    CONSTRAINT uq_memberships_circle_member     UNIQUE (circle_id, member_id),
    CONSTRAINT chk_memberships_role             CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'GUEST'))
);

-- Index pour les requêtes courantes
CREATE INDEX idx_memberships_circle_id ON circle_memberships (circle_id);
CREATE INDEX idx_memberships_member_id ON circle_memberships (member_id);

-- -------------------------------------------------------------------------
-- Commentaires
-- -------------------------------------------------------------------------
COMMENT ON TABLE circles IS 'Cercles de Vie Uzima (famille, travail, amis proches, projets, communautés)';
COMMENT ON COLUMN circles.notification_policy IS 'Politique de notification : IMMEDIATE, DEFERRED, URGENT_ONLY, BLOCKED';
COMMENT ON COLUMN circles.visibility IS 'Visibilité du contenu : PUBLIC, CIRCLE_ONLY, PRIVATE';

COMMENT ON TABLE circle_memberships IS 'Appartenances des utilisateurs aux Cercles de Vie';
COMMENT ON COLUMN circle_memberships.role IS 'Rôle dans le cercle : OWNER (unique), ADMIN, MEMBER, GUEST';
