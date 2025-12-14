# Phase 3: Delete Film Feature Implementation Plan

## Overview

Add delete functionality to the film list, allowing users to remove films from their list using HTMX without page reload. Includes confirmation dialog before deletion.

## Reference Implementation

Django project at `/home/geraldo/htmx/django-htmx/Video #4/` provides the pattern:

**URL:** `delete-film/<int:pk>/` (DELETE request)

**View:**
```python
def delete_film(request, pk):
    request.user.films.remove(pk)
    films = request.user.films.all()
    return render(request, 'partials/film-list.html', {'films': films})
```

**Template (film-list.html):**
```html
<li class="list-group-item d-flex justify-content-between align-items-center">
    {{ film.name }}
    <span class="badge badge-danger badge-pill"
        style="cursor: pointer;"
        hx-delete="{% url 'delete-film' film.pk %}"
        hx-target="#film-list"
        hx-confirm="Are you sure you wish to delete?">X</span>
</li>
```

---

## Files to Modify

### 1. FilmResource.java
**File:** `src/main/java/io/archton/scaffold/router/FilmResource.java`

Add DELETE endpoint:

```java
@DELETE
@Path("/{id}")
@Produces(MediaType.TEXT_HTML)
@Transactional
public TemplateInstance delete(@PathParam("id") Long id) {
    String userName = securityIdentity.getPrincipal().getName();
    User user = User.findByUsername(userName);

    // Only delete if film belongs to current user
    Film.delete("id = ?1 and user = ?2", id, user);

    List<Film> films = Film.findByUser(user);
    return Templates.filmList(films);
}
```

| Method | Path | Purpose | Returns |
|--------|------|---------|---------|
| DELETE | `/films/{id}` | Delete film by ID | HTML fragment (updated list) |

### 2. filmList.html Template
**File:** `src/main/resources/templates/FilmResource/filmList.html`

Update to include delete button:

```html
{#if films && films.size > 0}
<ul class="uk-list uk-list-striped">
    {#for film in films}
    <li class="uk-flex uk-flex-between uk-flex-middle">
        <span>{film.name}</span>
        <span class="uk-badge uk-background-danger"
              style="cursor: pointer;"
              hx-delete="/films/{film.id}"
              hx-target="#film-list"
              hx-confirm="Are you sure you wish to delete?">X</span>
    </li>
    {/for}
</ul>
{#else}
<p class="uk-text-muted">You do not have any films in your list</p>
{/if}
```

---

## HTMX Integration

| Attribute | Value | Purpose |
|-----------|-------|---------|
| `hx-delete` | `/films/{id}` | Send DELETE request to server |
| `hx-target` | `#film-list` | Replace film list div content |
| `hx-confirm` | "Are you sure..." | Show browser confirmation dialog |

**Flow:**
1. User clicks "X" badge on a film
2. Browser shows confirmation dialog
3. If confirmed, HTMX sends DELETE request to `/films/{id}`
4. Server deletes film (if owned by user), returns updated list
5. HTMX swaps new list into `#film-list` div

---

## Security Considerations

1. **Ownership Validation**: Only delete films belonging to the current user
2. **Authentication Required**: `/films/**` already protected by security policy
3. **No Cascade Issues**: Films are user-specific, no shared data concerns

---

## Implementation Order

1. Add DELETE endpoint to FilmResource.java
2. Update filmList.html template with delete button
3. Test delete functionality

---

## UIKit Styling

- `uk-badge` - Badge styling for delete button
- `uk-background-danger` - Red background (danger color)
- `uk-flex uk-flex-between uk-flex-middle` - Flexbox layout for list items
- `cursor: pointer` - Visual feedback that element is clickable
