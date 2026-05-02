-- Seed tasks
-- tenant_id, created_by, assigned_to reference the UUIDs from user-service V3 migration.
-- No FK constraints exist here (cross-service), so values must stay in sync manually.
--
-- Acme Corp (tenant aaaaaaaa-...-001):
--   alice.johnson DB-id = cccccccc-...-001  (TENANT_ADMIN, admin-frontend)
--   bob.smith     DB-id = cccccccc-...-002  (USER,         user-frontend)
--
-- TechStart (tenant aaaaaaaa-...-002):
--   carol.white   DB-id = cccccccc-...-003  (TENANT_ADMIN, admin-frontend)
--   dave.brown    DB-id = cccccccc-...-004  (READONLY,     user-frontend)

-- ── Acme Corp tasks ───────────────────────────────────────────────────────────

INSERT INTO tasks (id, title, description, status, priority, due_date, tenant_id, assigned_to, created_by) VALUES
(
    'dddddddd-0000-0000-0000-000000000001',
    'Set up development environment',
    'Install Docker, JDK 21, Node 20. Clone the repo and run docker compose up -d. Verify all services are healthy.',
    'TODO', 'HIGH',
    CURRENT_TIMESTAMP + INTERVAL '3 days',
    'aaaaaaaa-0000-0000-0000-000000000001',
    'cccccccc-0000-0000-0000-000000000002',  -- assigned to bob
    'cccccccc-0000-0000-0000-000000000001'   -- created by alice
),
(
    'dddddddd-0000-0000-0000-000000000002',
    'Implement user authentication flow',
    'Integrate Keycloak PKCE flow into the Angular frontend. Handle token refresh and silent renew. Cover both admin and app realms.',
    'IN_PROGRESS', 'HIGH',
    CURRENT_TIMESTAMP + INTERVAL '7 days',
    'aaaaaaaa-0000-0000-0000-000000000001',
    'cccccccc-0000-0000-0000-000000000001',  -- assigned to alice
    'cccccccc-0000-0000-0000-000000000001'   -- created by alice
),
(
    'dddddddd-0000-0000-0000-000000000003',
    'Write unit tests for task service',
    'Achieve at least 80% branch coverage on TaskService and TaskRepository. Use Testcontainers for DB tests.',
    'TODO', 'MEDIUM',
    CURRENT_TIMESTAMP + INTERVAL '14 days',
    'aaaaaaaa-0000-0000-0000-000000000001',
    'cccccccc-0000-0000-0000-000000000002',  -- assigned to bob
    'cccccccc-0000-0000-0000-000000000002'   -- created by bob
),
(
    'dddddddd-0000-0000-0000-000000000004',
    'Review authentication PR',
    'Review the Keycloak integration PR. Check token validation logic, error handling, and security best practices.',
    'ON_HOLD', 'LOW',
    NULL,
    'aaaaaaaa-0000-0000-0000-000000000001',
    NULL,                                    -- unassigned
    'cccccccc-0000-0000-0000-000000000001'   -- created by alice
);

INSERT INTO task_tags (task_id, tag) VALUES
    ('dddddddd-0000-0000-0000-000000000001', 'onboarding'),
    ('dddddddd-0000-0000-0000-000000000001', 'setup'),
    ('dddddddd-0000-0000-0000-000000000002', 'auth'),
    ('dddddddd-0000-0000-0000-000000000002', 'keycloak'),
    ('dddddddd-0000-0000-0000-000000000002', 'frontend'),
    ('dddddddd-0000-0000-0000-000000000003', 'testing'),
    ('dddddddd-0000-0000-0000-000000000003', 'backend'),
    ('dddddddd-0000-0000-0000-000000000004', 'review');

-- ── TechStart tasks ───────────────────────────────────────────────────────────

INSERT INTO tasks (id, title, description, status, priority, due_date, tenant_id, assigned_to, created_by) VALUES
(
    'dddddddd-0000-0000-0000-000000000005',
    'Design multi-tenant database schema',
    'Define the tenant isolation strategy: shared schema with tenant_id column. Document FK constraints and index strategy.',
    'DONE', 'CRITICAL',
    CURRENT_TIMESTAMP - INTERVAL '5 days',
    'aaaaaaaa-0000-0000-0000-000000000002',
    'cccccccc-0000-0000-0000-000000000003',  -- assigned to carol
    'cccccccc-0000-0000-0000-000000000003'   -- created by carol
),
(
    'dddddddd-0000-0000-0000-000000000006',
    'Write API documentation',
    'Document all REST endpoints and the GraphQL schema in Swagger and README. Include request/response examples for each endpoint.',
    'IN_PROGRESS', 'MEDIUM',
    CURRENT_TIMESTAMP + INTERVAL '5 days',
    'aaaaaaaa-0000-0000-0000-000000000002',
    'cccccccc-0000-0000-0000-000000000004',  -- assigned to dave (read-only user can still be assigned)
    'cccccccc-0000-0000-0000-000000000003'   -- created by carol
),
(
    'dddddddd-0000-0000-0000-000000000007',
    'Deploy to staging environment',
    'Set up Docker Compose staging config. Configure environment variables. Verify all health checks pass before go-live.',
    'TODO', 'HIGH',
    CURRENT_TIMESTAMP + INTERVAL '10 days',
    'aaaaaaaa-0000-0000-0000-000000000002',
    NULL,                                    -- unassigned
    'cccccccc-0000-0000-0000-000000000003'   -- created by carol
),
(
    'dddddddd-0000-0000-0000-000000000008',
    'Set up monitoring and alerting',
    'Configure Grafana dashboards for JVM metrics, HTTP latency, and Kafka lag. Set up alert rules for error rate thresholds.',
    'TODO', 'MEDIUM',
    CURRENT_TIMESTAMP + INTERVAL '21 days',
    'aaaaaaaa-0000-0000-0000-000000000002',
    'cccccccc-0000-0000-0000-000000000004',  -- assigned to dave
    'cccccccc-0000-0000-0000-000000000003'   -- created by carol
);

INSERT INTO task_tags (task_id, tag) VALUES
    ('dddddddd-0000-0000-0000-000000000005', 'database'),
    ('dddddddd-0000-0000-0000-000000000005', 'architecture'),
    ('dddddddd-0000-0000-0000-000000000006', 'documentation'),
    ('dddddddd-0000-0000-0000-000000000006', 'api'),
    ('dddddddd-0000-0000-0000-000000000007', 'devops'),
    ('dddddddd-0000-0000-0000-000000000007', 'deployment'),
    ('dddddddd-0000-0000-0000-000000000008', 'monitoring'),
    ('dddddddd-0000-0000-0000-000000000008', 'observability');
