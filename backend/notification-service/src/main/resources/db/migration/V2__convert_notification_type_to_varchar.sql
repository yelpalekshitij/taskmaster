ALTER TABLE notifications
    ALTER COLUMN type TYPE VARCHAR(50) USING type::text;

DROP TYPE IF EXISTS notification_type;