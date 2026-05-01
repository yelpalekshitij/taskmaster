CREATE TYPE task_status AS ENUM ('TODO', 'IN_PROGRESS', 'ON_HOLD', 'DONE', 'SCHEDULED');
CREATE TYPE task_priority AS ENUM ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL');

CREATE TABLE tasks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(500) NOT NULL,
    description     TEXT,
    status          task_status NOT NULL DEFAULT 'TODO',
    priority        task_priority NOT NULL DEFAULT 'MEDIUM',
    due_date        TIMESTAMP WITH TIME ZONE,
    scheduled_date  TIMESTAMP WITH TIME ZONE,
    tenant_id       UUID NOT NULL,
    assigned_to     UUID,
    created_by      UUID NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE task_tags (
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    tag     VARCHAR(100) NOT NULL,
    PRIMARY KEY (task_id, tag)
);

CREATE TABLE task_comments (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id    UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL,
    content    TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE task_history (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id    UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    changed_by UUID NOT NULL,
    old_status task_status,
    new_status task_status NOT NULL,
    comment    TEXT,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE outbox_events (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type   VARCHAR(100) NOT NULL DEFAULT 'TASK',
    aggregate_id     UUID NOT NULL,
    event_type       VARCHAR(100) NOT NULL,
    payload          JSONB NOT NULL,
    idempotency_key  VARCHAR(255) UNIQUE NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at     TIMESTAMP WITH TIME ZONE,
    publish_attempts INT NOT NULL DEFAULT 0
);

-- Indexes for common query patterns
CREATE INDEX idx_tasks_tenant_id    ON tasks(tenant_id);
CREATE INDEX idx_tasks_assigned_to  ON tasks(assigned_to);
CREATE INDEX idx_tasks_status       ON tasks(status);
CREATE INDEX idx_tasks_tenant_status ON tasks(tenant_id, status);
CREATE INDEX idx_task_comments_task_id ON task_comments(task_id);
CREATE INDEX idx_task_history_task_id  ON task_history(task_id);
CREATE INDEX idx_outbox_unpublished ON outbox_events(created_at) WHERE published_at IS NULL;
