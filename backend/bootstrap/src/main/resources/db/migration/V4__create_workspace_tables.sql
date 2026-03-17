-- ============================================================
-- V4 : Workspace — Projets, Membres, Tâches, Entrées de temps
-- ============================================================

-- ------------------------------------------------------------
-- Table : projects
-- ------------------------------------------------------------
CREATE TABLE projects (
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    name       VARCHAR(150) NOT NULL,
    owner_id   UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_projects PRIMARY KEY (id),
    CONSTRAINT fk_projects_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE INDEX idx_projects_owner_id   ON projects (owner_id);
CREATE INDEX idx_projects_created_at ON projects (created_at DESC);

-- ------------------------------------------------------------
-- Table : project_members
-- ------------------------------------------------------------
CREATE TABLE project_members (
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    project_id UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    role       VARCHAR(20) NOT NULL CHECK (role IN ('VIEWER', 'MEMBER', 'MANAGER', 'OWNER')),
    joined_at  TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_project_members       PRIMARY KEY (id),
    CONSTRAINT fk_project_members_proj  FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_project_members_user  FOREIGN KEY (user_id)    REFERENCES users    (id) ON DELETE CASCADE,
    CONSTRAINT uk_project_members       UNIQUE (project_id, user_id)
);

CREATE INDEX idx_project_members_project_id ON project_members (project_id);
CREATE INDEX idx_project_members_user_id    ON project_members (user_id);

-- ------------------------------------------------------------
-- Table : tasks
-- ------------------------------------------------------------
CREATE TABLE tasks (
    id             UUID        NOT NULL DEFAULT gen_random_uuid(),
    title          VARCHAR(255) NOT NULL,
    project_id     UUID        NOT NULL,
    assignee_id    UUID,
    status         VARCHAR(20) NOT NULL CHECK (status IN ('BACKLOG', 'TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE')),
    priority       VARCHAR(20) NOT NULL CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    blocked_reason VARCHAR(500),
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    completed_at   TIMESTAMPTZ,

    CONSTRAINT pk_tasks         PRIMARY KEY (id),
    CONSTRAINT fk_tasks_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_tasks_assignee FOREIGN KEY (assignee_id) REFERENCES users   (id) ON DELETE SET NULL
);

CREATE INDEX idx_tasks_project_id  ON tasks (project_id);
CREATE INDEX idx_tasks_assignee_id ON tasks (assignee_id);
CREATE INDEX idx_tasks_status      ON tasks (status);
CREATE INDEX idx_tasks_updated_at  ON tasks (updated_at DESC);

-- ------------------------------------------------------------
-- Table : time_entries
-- ------------------------------------------------------------
CREATE TABLE time_entries (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL,
    project_id  UUID        NOT NULL,
    description VARCHAR(500),
    started_at  TIMESTAMPTZ NOT NULL,
    stopped_at  TIMESTAMPTZ,

    CONSTRAINT pk_time_entries         PRIMARY KEY (id),
    CONSTRAINT fk_time_entries_user    FOREIGN KEY (user_id)    REFERENCES users    (id) ON DELETE CASCADE,
    CONSTRAINT fk_time_entries_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE
);

CREATE INDEX idx_time_entries_project_id ON time_entries (project_id);
CREATE INDEX idx_time_entries_user_id    ON time_entries (user_id);
CREATE INDEX idx_time_entries_started_at ON time_entries (started_at DESC);
