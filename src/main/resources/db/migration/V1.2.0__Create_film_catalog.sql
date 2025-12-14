CREATE TABLE film_catalog (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL UNIQUE
);

-- Seed with popular films
INSERT INTO film_catalog (name) VALUES
    ('The Godfather'),
    ('The Shawshank Redemption'),
    ('Pulp Fiction'),
    ('The Dark Knight'),
    ('Fight Club'),
    ('Forrest Gump'),
    ('Inception'),
    ('The Matrix'),
    ('Goodfellas'),
    ('Se7en'),
    ('The Silence of the Lambs'),
    ('Schindler''s List'),
    ('Casablanca'),
    ('City of God'),
    ('Taxi Driver'),
    ('Fargo'),
    ('The Big Lebowski'),
    ('No Country for Old Men'),
    ('There Will Be Blood'),
    ('Apocalypse Now');
