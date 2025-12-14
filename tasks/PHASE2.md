# Phase 2: Film List Feature Implementation Plan

## Overview

Implement a dynamic film list feature allowing authenticated users to add films to their personal list using HTMX for real-time updates without page reloads.

## Reference Implementation

Django project at `/home/geraldo/htmx/django-htmx/Video #3/` provides the pattern:
- **Model**: Film with `name` field + relationship to User
- **HTMX**: Form `hx-post` to add endpoint, `hx-target` swaps film list
- **Partials**: Separate `film-list.html` for reusable fragment

---

## Files to Create/Modify

### 1. Database Migration
**File:** `src/main/resources/db/migration/V1.1.0__Create_films_table.sql`

```sql
CREATE TABLE films (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_films_user_id ON films(user_id);
```

### 2. Film Entity
**File:** `src/main/java/io/archton/scaffold/entity/Film.java`

```java
@Entity
@Table(name = "films")
public class Film extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 128)
    public String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    public static List<Film> findByUser(User user) {
        return list("user", user);
    }
}
```

### 3. FilmResource
**File:** `src/main/java/io/archton/scaffold/router/FilmResource.java`

| Method | Path | Purpose | Returns |
|--------|------|---------|---------|
| GET | `/films` | Show films page | Full HTML page |
| POST | `/films/add` | Add new film | HTML fragment (updated list) |

Key implementation:
- Inject `SecurityIdentity` to get current user
- `add()` creates Film, associates with user, returns partial template
- Use `@Transactional` for database operations

### 4. Templates
- `src/main/resources/templates/FilmResource/films.html` - Main page
- `src/main/resources/templates/FilmResource/filmList.html` - Partial for HTMX swap

### 5. Update Navbar
**File:** `src/main/resources/templates/base.html`
- Add "Films" link for authenticated users

### 6. Security Configuration
**File:** `src/main/resources/application.properties`
- Add `/films/**` to authenticated routes via policy

---

## Implementation Order

1. Create Flyway migration for films table
2. Create Film entity with Panache
3. Create FilmResource with CheckedTemplate
4. Create films.html template (main page)
5. Create filmList.html partial template
6. Update base.html navbar with Films link
7. Configure security for /films/** routes

---

## HTMX Integration

The add film form uses:
```html
<form hx-post="/films/add" hx-target="#film-list" hx-swap="innerHTML">
    <input type="text" name="filmname" placeholder="Enter a film" />
    <button type="submit">Add Film</button>
</form>
```

The endpoint returns only the updated film list HTML fragment, which HTMX swaps into the `#film-list` div without a full page reload.

---

## Security Considerations

1. **Authentication Required**: `/films/**` routes require login
2. **Data Isolation**: Films filtered by current user only
3. **CSRF**: Handled automatically by Quarkus form auth
