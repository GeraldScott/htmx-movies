# Phase 5: Film Search Feature Implementation Plan

## Overview

Add a dynamic search component that allows users to search a film catalog and add films to their personal list. Search results update in real-time as the user types (debounced), and films already in the user's list are excluded from results.

## Reference Implementation

Django project at `/home/geraldo/htmx/django-htmx/Video #5/` provides the pattern.

**Key Differences:**
- Django uses ManyToMany (films shared between users)
- Our Quarkus model uses ManyToOne (films owned by users)
- Solution: Create a separate `FilmCatalog` entity for searchable films

---

## New Functionality

### Search Input
```html
<input type="text"
    hx-post="/films/search"
    hx-target="#search-results"
    hx-trigger="keyup changed delay:500ms"
    name="search"
    placeholder="Search films..." />
```

### Search Results
- Shows films matching search text (case-insensitive)
- Excludes films already in user's list
- Each result has an "add" button to add film to user's list
- "add" button uses `hx-post` with `hx-vals` to pass film name

---

## Files to Create

### 1. FilmCatalog Entity
**File:** `src/main/java/io/archton/scaffold/entity/FilmCatalog.java`

```java
@Entity
@Table(name = "film_catalog")
public class FilmCatalog extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 128)
    public String name;

    public static List<FilmCatalog> searchByName(String search, List<String> excludeNames) {
        if (excludeNames.isEmpty()) {
            return list("lower(name) like lower(?1)", "%" + search + "%");
        }
        return list("lower(name) like lower(?1) and name not in ?2",
                    "%" + search + "%", excludeNames);
    }
}
```

### 2. Database Migration
**File:** `src/main/resources/db/migration/V1.2.0__Create_film_catalog.sql`

```sql
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
```

### 3. Search Partial Template
**File:** `src/main/resources/templates/FilmResource/search.html`

```html
<div class="uk-margin">
    <input type="text"
        class="uk-input uk-form-small"
        hx-post="/films/search"
        hx-target="#search-results"
        hx-trigger="keyup changed delay:500ms"
        name="search"
        placeholder="Search films..." />
</div>
<div id="search-results"></div>
```

### 4. Search Results Template
**File:** `src/main/resources/templates/FilmResource/searchResults.html`

```html
{#if results && results.size > 0}
<ul class="uk-list uk-list-striped">
    {#for film in results}
    <li class="uk-flex uk-flex-between uk-flex-middle">
        <span>{film.name}</span>
        <span class="uk-badge"
              style="cursor: pointer; background-color: #32d296;"
              hx-post="/films/add"
              hx-vals='{"filmname": "{film.name}"}'
              hx-target="#film-list">add</span>
    </li>
    {/for}
</ul>
{#else if search}
<p class="uk-text-muted">No films found matching "{search}"</p>
{/if}
```

---

## Files to Modify

### 1. FilmResource.java
**File:** `src/main/java/io/archton/scaffold/router/FilmResource.java`

Add search endpoint and update Templates class:

```java
@CheckedTemplate
public static class Templates {
    public static native TemplateInstance films(String title, String userName, List<Film> films);
    public static native TemplateInstance filmList(List<Film> films);
    public static native TemplateInstance searchResults(List<FilmCatalog> results, String search);
}

@POST
@Path("/search")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_HTML)
public TemplateInstance search(@FormParam("search") String search) {
    String userName = securityIdentity.getPrincipal().getName();
    User user = User.findByUsername(userName);

    if (search == null || search.isBlank()) {
        return Templates.searchResults(List.of(), null);
    }

    // Get names of films user already has
    List<String> userFilmNames = Film.findByUser(user)
        .stream()
        .map(f -> f.name)
        .toList();

    List<FilmCatalog> results = FilmCatalog.searchByName(search.trim(), userFilmNames);
    return Templates.searchResults(results, search);
}
```

### 2. filmList.html Template
**File:** `src/main/resources/templates/FilmResource/filmList.html`

Update layout to include search panel:

```html
<div class="uk-grid uk-child-width-1-2@m" uk-grid>
    <div>
        {#if films && films.size > 0}
        <ul class="uk-list uk-list-striped">
            {#for film in films}
            <li class="uk-flex uk-flex-between uk-flex-middle">
                <span>{film.name}</span>
                <span class="uk-badge"
                      style="cursor: pointer; background-color: #f0506e;"
                      hx-delete="/films/{film.id}"
                      hx-target="#film-list"
                      hx-confirm="Are you sure you wish to delete?">X</span>
            </li>
            {/for}
        </ul>
        {#else}
        <p class="uk-text-muted">You do not have any films in your list</p>
        {/if}
    </div>
    <div>
        {#include FilmResource/search /}
    </div>
</div>
```

### 3. films.html Template
**File:** `src/main/resources/templates/FilmResource/films.html`

Update to use the new layout structure (film list div wraps both sections).

---

## HTMX Integration

| Feature | Attributes | Purpose |
|---------|------------|---------|
| Search input | `hx-post="/films/search"` `hx-trigger="keyup changed delay:500ms"` `hx-target="#search-results"` | Real-time search with debounce |
| Add from search | `hx-post="/films/add"` `hx-vals='{"filmname": "..."}` `hx-target="#film-list"` | Add film to user's list |

**Flow:**
1. User types in search box
2. After 500ms pause, HTMX posts to `/films/search`
3. Server returns matching films (excluding user's films)
4. User clicks "add" on a result
5. Film added to user's list, film-list div updated

---

## Implementation Order

1. Create Flyway migration with film_catalog table and seed data
2. Create FilmCatalog entity
3. Add search endpoint to FilmResource
4. Create search.html partial template
5. Create searchResults.html template
6. Update filmList.html to include search panel
7. Update films.html layout if needed

---

## UIKit Styling

- Search on right side of film list (two-column grid)
- `uk-input uk-form-small` - Search input styling
- `uk-badge` with green background (`#32d296`) - Add button
- `uk-list uk-list-striped` - Results list
