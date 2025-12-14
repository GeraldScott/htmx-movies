# Phase 6.1: Sortable Drag-and-Drop Film List

## Overview

Add drag-and-drop reordering functionality to the film list using Sortable.js integrated with HTMX. Users can drag films to reorder them, and the new order persists to the database.

## Reference Implementation

Django project at `/home/geraldo/htmx/django-htmx/Video #6.1/` provides the pattern.

**Key Differences:**
- Django uses ManyToMany with intermediate `UserFilms` table
- Our Quarkus model uses ManyToOne (films owned by users)
- Solution: Add `order` field directly to existing `Film` entity (simpler)

---

## New Functionality

### Drag-and-Drop Behavior
- User drags a film to a new position in the list
- Sortable.js fires "end" event when drag completes
- HTMX posts ordered film IDs to `/films/reorder`
- Server updates order values and returns updated list
- List refreshes without page reload

### HTMX + Sortable.js Integration
```html
<form class="sortable" hx-post="/films/reorder" hx-trigger="end" hx-target="#film-list">
    {#for film in films}
    <div>
        <input type="hidden" name="film_order" value="{film.id}" />
        <li>...</li>
    </div>
    {/for}
</form>
```

---

## Files to Create

### 1. Database Migration
**File:** `src/main/resources/db/migration/V1.3.0__Add_film_order.sql`

```sql
-- Add order column to films table
ALTER TABLE films ADD COLUMN display_order INTEGER;

-- Initialize order based on existing IDs (preserves current implicit order)
UPDATE films SET display_order = id;

-- Make column not null after initialization
ALTER TABLE films ALTER COLUMN display_order SET NOT NULL;

-- Add index for efficient ordering queries
CREATE INDEX idx_films_user_order ON films(user_id, display_order);
```

---

## Files to Modify

### 1. Film Entity
**File:** `src/main/java/io/archton/scaffold/entity/Film.java`

Add order field and update query method:

```java
@Column(name = "display_order", nullable = false)
public Integer displayOrder;

public static List<Film> findByUserOrdered(User user) {
    return list("user = ?1 order by displayOrder", user);
}

public static Integer getMaxOrder(User user) {
    Film film = find("user = ?1 order by displayOrder desc", user).firstResult();
    return film != null ? film.displayOrder : 0;
}

public static void reorderFilms(User user) {
    List<Film> films = findByUserOrdered(user);
    int order = 1;
    for (Film film : films) {
        film.displayOrder = order++;
    }
}
```

### 2. FilmResource.java
**File:** `src/main/java/io/archton/scaffold/router/FilmResource.java`

**Changes needed:**

1. Update `list()` to use ordered query
2. Update `add()` to set order (max + 1)
3. Update `delete()` to reorder after deletion
4. Add new `reorder()` endpoint

```java
@POST
@Path("/reorder")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_HTML)
@Transactional
public TemplateInstance reorder(@FormParam("film_order") List<Long> filmIds) {
    String userName = securityIdentity.getPrincipal().getName();
    User user = User.findByUsername(userName);

    // Update order based on position in list
    int order = 1;
    for (Long filmId : filmIds) {
        Film.update("displayOrder = ?1 where id = ?2 and user = ?3", order++, filmId, user);
    }

    List<Film> films = Film.findByUserOrdered(user);
    return Templates.filmList(films);
}
```

### 3. base.html
**File:** `src/main/resources/templates/base.html`

Add Sortable.js library and initialization:

```html
<!-- After HTMX script -->
<script src="https://cdn.jsdelivr.net/npm/sortablejs@latest/Sortable.min.js"></script>

<script>
    // Initialize Sortable on HTMX content load
    document.body.addEventListener('htmx:load', function(evt) {
        var sortables = evt.detail.elt.querySelectorAll('.sortable');
        sortables.forEach(function(sortable) {
            new Sortable(sortable, {
                animation: 150,
                ghostClass: 'uk-background-muted',
                handle: '.drag-handle'
            });
        });
    });
</script>
```

### 4. filmList.html
**File:** `src/main/resources/templates/FilmResource/filmList.html`

Wrap film list in sortable form:

```html
<div class="uk-grid uk-child-width-1-2@m" uk-grid>
    <div>
        {#if films && films.size > 0}
        <form class="sortable" hx-post="/films/reorder" hx-trigger="end" hx-target="#film-list">
            <ul class="uk-list uk-list-striped">
                {#for film in films}
                <div>
                    <input type="hidden" name="film_order" value="{film.id}" />
                    <li class="uk-flex uk-flex-between uk-flex-middle">
                        <span class="drag-handle" style="cursor: grab; margin-right: 10px;" uk-icon="icon: menu"></span>
                        <span class="uk-flex-1">{film.name}</span>
                        <span class="uk-badge"
                              style="cursor: pointer; background-color: #f0506e;"
                              hx-delete="/films/{film.id}"
                              hx-target="#film-list"
                              hx-confirm="Are you sure you wish to delete?">X</span>
                    </li>
                </div>
                {/for}
            </ul>
        </form>
        {#else}
        <p class="uk-text-muted">You do not have any films in your list</p>
        {/if}
    </div>
    <div>
        {#include FilmResource/search /}
    </div>
</div>
```

---

## HTMX Integration

| Feature | Attributes | Purpose |
|---------|------------|---------|
| Sortable form | `class="sortable"` | Sortable.js selector |
| Reorder trigger | `hx-trigger="end"` | Fires when drag ends |
| Reorder endpoint | `hx-post="/films/reorder"` | POST ordered IDs |
| Update target | `hx-target="#film-list"` | Swap updated list |

**Flow:**
1. User drags film to new position
2. Sortable.js reorders DOM elements
3. On drop, Sortable.js fires "end" event
4. HTMX collects all `film_order` hidden inputs (now in new order)
5. HTMX posts to `/films/reorder` with ordered IDs
6. Server updates database order values
7. Server returns updated filmList template
8. HTMX swaps new list into `#film-list`
9. Sortable.js reinitializes via `htmx:load` event

---

## Implementation Order

1. Create Flyway migration to add `display_order` column
2. Update Film entity with order field and helper methods
3. Update FilmResource:
   - Modify `list()` to use ordered query
   - Modify `add()` to assign order
   - Modify `delete()` to call reorder
   - Add `reorder()` endpoint
4. Update base.html with Sortable.js
5. Update filmList.html with sortable form structure
6. Test drag-and-drop functionality

---

## UIKit Styling

- `uk-icon="icon: menu"` - Drag handle icon (hamburger menu)
- `uk-background-muted` - Ghost class during drag
- `uk-flex-1` - Film name takes remaining space
- Existing striped list styling preserved

---

## Technical Notes

### Sortable.js Configuration
```javascript
new Sortable(element, {
    animation: 150,        // Smooth animation duration (ms)
    ghostClass: 'uk-background-muted',  // Class applied during drag
    handle: '.drag-handle' // Only drag via handle element
});
```

### Form Parameter Handling
Quarkus JAX-RS automatically deserializes multiple form params with same name into `List<Long>`:
```java
@FormParam("film_order") List<Long> filmIds
```

### Security Considerations
- Validate all film IDs belong to authenticated user
- Use `where user = ?` in update queries
- Reorder only affects current user's films
