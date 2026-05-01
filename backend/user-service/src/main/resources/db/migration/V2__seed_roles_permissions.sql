-- Seed permissions
INSERT INTO permissions (id, name, description) VALUES
    (gen_random_uuid(), 'TASK_CREATE',          'Create new tasks'),
    (gen_random_uuid(), 'TASK_READ',            'View tasks'),
    (gen_random_uuid(), 'TASK_UPDATE',          'Update task details and status'),
    (gen_random_uuid(), 'TASK_DELETE',          'Delete tasks'),
    (gen_random_uuid(), 'TASK_ASSIGN',          'Assign tasks to other users'),
    (gen_random_uuid(), 'USER_VIEW',            'View user profiles'),
    (gen_random_uuid(), 'USER_MANAGE',          'Create, update, and deactivate users'),
    (gen_random_uuid(), 'TENANT_VIEW',          'View tenant information'),
    (gen_random_uuid(), 'TENANT_MANAGE',        'Manage tenant settings'),
    (gen_random_uuid(), 'REPORT_VIEW',          'View reports and statistics'),
    (gen_random_uuid(), 'NOTIFICATION_MANAGE',  'Manage notification settings'),
    (gen_random_uuid(), 'SCHEDULER_MANAGE',     'Schedule and manage task schedules')
ON CONFLICT (name) DO NOTHING;

-- System roles (no tenant_id)
INSERT INTO roles (id, name, tenant_id, description, system_role) VALUES
    ('00000000-0000-0000-0000-000000000001', 'MASTER_ADMIN', NULL,
     'Master administrator with full cross-tenant access', true),
    ('00000000-0000-0000-0000-000000000002', 'TENANT_ADMIN', NULL,
     'Tenant administrator with full access within their tenant', true),
    ('00000000-0000-0000-0000-000000000003', 'USER', NULL,
     'Standard user with task management access', true),
    ('00000000-0000-0000-0000-000000000004', 'READONLY', NULL,
     'Read-only access to tasks and reports', true)
ON CONFLICT (id) DO NOTHING;

-- TENANT_ADMIN: all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000002', id FROM permissions
ON CONFLICT DO NOTHING;

-- USER: task and notification permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000003', id
FROM permissions
WHERE name IN ('TASK_CREATE', 'TASK_READ', 'TASK_UPDATE', 'TASK_DELETE',
               'USER_VIEW', 'REPORT_VIEW', 'NOTIFICATION_MANAGE')
ON CONFLICT DO NOTHING;

-- READONLY: view only
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000004', id
FROM permissions
WHERE name IN ('TASK_READ', 'USER_VIEW', 'REPORT_VIEW')
ON CONFLICT DO NOTHING;
