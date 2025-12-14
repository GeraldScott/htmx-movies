-- Film table for user's movie lists
CREATE TABLE films (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_films_user_id ON films(user_id);
