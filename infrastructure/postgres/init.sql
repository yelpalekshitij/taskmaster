-- TaskMaster PostgreSQL initialization
-- Creates separate databases for each microservice

CREATE USER taskmaster WITH PASSWORD 'taskmaster_secret';

CREATE DATABASE taskmanager_users;
CREATE DATABASE taskmanager_tasks;
CREATE DATABASE taskmanager_notifications;
CREATE DATABASE taskmanager_scheduler;

GRANT ALL PRIVILEGES ON DATABASE taskmanager_users TO taskmaster;
GRANT ALL PRIVILEGES ON DATABASE taskmanager_tasks TO taskmaster;
GRANT ALL PRIVILEGES ON DATABASE taskmanager_notifications TO taskmaster;
GRANT ALL PRIVILEGES ON DATABASE taskmanager_scheduler TO taskmaster;

-- Grant schema privileges (Flyway needs this)
\c taskmanager_users
GRANT ALL ON SCHEMA public TO taskmaster;

\c taskmanager_tasks
GRANT ALL ON SCHEMA public TO taskmaster;

\c taskmanager_notifications
GRANT ALL ON SCHEMA public TO taskmaster;

\c taskmanager_scheduler
GRANT ALL ON SCHEMA public TO taskmaster;

-- Keycloak database
CREATE USER keycloak WITH PASSWORD 'keycloak_secret';
CREATE DATABASE keycloak OWNER keycloak;
GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak;
\c keycloak
GRANT ALL ON SCHEMA public TO keycloak;
