-- V2: Make the 'name' column in users table nullable with a default empty string.
-- This resolves the "Field 'name' doesn't have a default value" SQL error
-- that occurs when new users are inserted without an explicit 'name' value.

ALTER TABLE users
    MODIFY COLUMN name VARCHAR(255) NULL DEFAULT NULL;

-- Backfill any existing rows where name is NULL with display_name
UPDATE users SET name = display_name WHERE name IS NULL OR name = '';
