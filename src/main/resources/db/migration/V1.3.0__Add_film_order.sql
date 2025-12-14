-- Add order column to films table
ALTER TABLE films ADD COLUMN display_order INTEGER;

-- Initialize order based on existing IDs (preserves current implicit order)
UPDATE films SET display_order = id;

-- Make column not null after initialization
ALTER TABLE films ALTER COLUMN display_order SET NOT NULL;

-- Add index for efficient ordering queries
CREATE INDEX idx_films_user_order ON films(user_id, display_order);
