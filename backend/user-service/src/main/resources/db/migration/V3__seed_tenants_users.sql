-- Seed tenants
-- IDs are stable and match the tenant_id attributes set on Keycloak users.
INSERT INTO tenants (id, name, domain, active) VALUES
    ('aaaaaaaa-0000-0000-0000-000000000001', 'Acme Corp',  'acme.com',      true),
    ('aaaaaaaa-0000-0000-0000-000000000002', 'TechStart',  'techstart.io',  true)
ON CONFLICT (id) DO NOTHING;

-- Seed users
-- keycloak_id matches the "id" field on the corresponding user in the Keycloak realm JSON files.
-- alice and carol are in taskmaster-admin realm (TENANT_ADMIN) → use admin-frontend (port 4201).
-- bob and dave are in taskmaster-app realm (USER/READONLY)    → use user-frontend (port 4200).
INSERT INTO users (id, keycloak_id, email, username, first_name, last_name, tenant_id, active) VALUES
    ('cccccccc-0000-0000-0000-000000000001', 'bbbbbbbb-0000-0000-0000-000000000001',
     'alice@acme.com',    'alice.johnson', 'Alice', 'Johnson',
     'aaaaaaaa-0000-0000-0000-000000000001', true),
    ('cccccccc-0000-0000-0000-000000000002', 'bbbbbbbb-0000-0000-0000-000000000002',
     'bob@acme.com',      'bob.smith',     'Bob',   'Smith',
     'aaaaaaaa-0000-0000-0000-000000000001', true),
    ('cccccccc-0000-0000-0000-000000000003', 'bbbbbbbb-0000-0000-0000-000000000003',
     'carol@techstart.io', 'carol.white',  'Carol', 'White',
     'aaaaaaaa-0000-0000-0000-000000000002', true),
    ('cccccccc-0000-0000-0000-000000000004', 'bbbbbbbb-0000-0000-0000-000000000004',
     'dave@techstart.io',  'dave.brown',   'Dave',  'Brown',
     'aaaaaaaa-0000-0000-0000-000000000002', true)
ON CONFLICT (id) DO NOTHING;

-- Assign system roles (role IDs from V2__seed_roles_permissions.sql)
INSERT INTO user_roles (user_id, role_id) VALUES
    ('cccccccc-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002'), -- alice  → TENANT_ADMIN
    ('cccccccc-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000003'), -- bob    → USER
    ('cccccccc-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000002'), -- carol  → TENANT_ADMIN
    ('cccccccc-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000004')  -- dave   → READONLY
ON CONFLICT DO NOTHING;
