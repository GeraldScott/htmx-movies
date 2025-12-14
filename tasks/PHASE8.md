# Phase 8: Infinite Scroll and URL Push State

## Overview

Add infinite scroll (lazy loading) for the film list and URL push state for better navigation experience. When users scroll to the bottom of their film list, more films load automatically. Browser URLs update when navigating between list and detail views, enabling proper back/forward navigation.

## Reference Implementation

Django project at `/home/geraldo/htmx/django-htmx/Video #8/` provides the pattern.

**New Features (compared to Video #7):**
- Infinite scroll using `hx-trigger="revealed"` to load more films
- Pagination support with configurable page size
- URL push state with `hx-push-url` for browser history integration
- HTMX request detection to return appropriate template fragments

---

## New Functionality

### Infinite Scroll (Lazy Loading)
The last film element triggers loading of the next page when it becomes visible:

```html
{#if isLastItem}
<div hx-get="/films?page={nextPage}" hx-trigger="revealed" hx-swap="afterend" hx-target="this">
{#else}
<div>
{/if}
    <!-- film item content -->
</div>
```

### URL Push State
Film detail links update the browser URL without full page reload:

```html
<!-- Film list item -->
<a hx-get="/films/{film.id}/detail"
   hx-target="#film-list"
   hx-push-url="/films/{film.name}">
    #{film.displayOrder} {film.name}
</a>

<!-- Return to list button -->
<button hx-get="/films/list-partial"
        hx-target="#film-list"
        hx-push-url="/films">Your List</button>
```

### HTMX Request Detection
Return different templates based on whether request is from HTMX or regular page load:
- HTMX request: Return only the film elements (for infinite scroll append)
- Regular request: Return full page with layout

---

## Files to Create

### 1. Film List Elements Template (for infinite scroll)
**File:** `src/main/resources/templates/FilmResource/filmListElements.html`

```html
{#for film in films}
{#if film_isLast && hasMorePages}
<div hx-get="/films?page={nextPage}" hx-trigger="revealed" hx-swap="afterend" hx-target="this">
{#else}
<div>
{/if}
    <input type="hidden" name="film_order" value="{film.id}" />
    <li class="uk-flex uk-flex-between uk-flex-middle">
        <span class="drag-handle" style="cursor: grab; margin-right: 10px;" uk-icon="icon: menu"></span>
        <a class="uk-flex-1" style="cursor: pointer;"
           hx-get="/films/{film.id}/detail"
           hx-target="#film-list"
           hx-push-url="/films/{film.name}">
            #{film.displayOrder} {film.name}
        </a>
        <span class="uk-badge"
              style="cursor: pointer; background-color: #f0506e;"
              hx-delete="/films/{film.id}"
              hx-target="#film-list"
              hx-confirm="Are you sure you wish to delete?">X</span>
    </li>
</div>
{/for}
```

---

## Files to Modify

### 1. FilmResource.java
**File:** `src/main/java/io/archton/scaffold/router/FilmResource.java`

Add pagination support and HTMX detection:

```java
// Add to Templates class
public static native TemplateInstance filmListElements(List<Film> films, int nextPage, boolean hasMorePages);

// Configuration
private static final int PAGE_SIZE = 100;

// Modify list endpoint to support pagination
@GET
@Produces(MediaType.TEXT_HTML)
public TemplateInstance list(@QueryParam("page") @DefaultValue("1") int page,
                             @HeaderParam("HX-Request") String hxRequest) {
    String userName = securityIdentity.getPrincipal().getName();
    User user = User.findByUsername(userName);

    long totalFilms = Film.count("user = ?1", user);
    int totalPages = (int) Math.ceil((double) totalFilms / PAGE_SIZE);
    boolean hasMorePages = page < totalPages;

    List<Film> films = Film.find("user = ?1 order by displayOrder", user)
        .page(page - 1, PAGE_SIZE)
        .list();

    // Return elements only for HTMX infinite scroll requests
    if (hxRequest != null && page > 1) {
        return Templates.filmListElements(films, page + 1, hasMorePages);
    }

    return Templates.films("My Films", userName, films);
}
```

### 2. filmList.html
**File:** `src/main/resources/templates/FilmResource/filmList.html`

Update to include the elements template and support pagination:

```html
<div class="uk-grid uk-child-width-1-2@m" uk-grid>
    <div>
        {#if films && films.size > 0}
        <form class="sortable" hx-post="/films/reorder" hx-trigger="end" hx-target="#film-list">
            <ul class="uk-list uk-list-striped">
                {#include FilmResource/filmListElements films=films nextPage=2 hasMorePages=hasMorePages /}
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

### 3. filmDetail.html
**File:** `src/main/resources/templates/FilmResource/filmDetail.html`

Add `hx-push-url` to the "Your List" button:

```html
<button class="uk-button uk-button-primary"
        hx-get="/films/list-partial"
        hx-target="#film-list"
        hx-push-url="/films">Your List</button>
```

### 4. application.properties
**File:** `src/main/resources/application.properties`

Add pagination configuration (optional):

```properties
# Pagination configuration
app.films.page-size=100
```

---

## HTMX Integration

| Feature | Attributes | Purpose |
|---------|------------|---------|
| Infinite scroll | `hx-get="/films?page=N"` `hx-trigger="revealed"` `hx-swap="afterend"` | Load more films when last item visible |
| URL push (detail) | `hx-push-url="/films/{name}"` | Update URL when viewing film detail |
| URL push (list) | `hx-push-url="/films"` | Update URL when returning to list |

**Flow:**
1. User loads `/films` - full page with first batch of films
2. User scrolls to bottom - `revealed` trigger fires on last element
3. HTMX loads next page, appends after last element
4. User clicks film name - detail loads, URL updates to `/films/{name}`
5. User clicks "Your List" - list loads, URL updates to `/films`
6. Browser back/forward works with pushed URLs

---

## Implementation Order

1. Create `filmListElements.html` template for film items
2. Update `FilmResource.list()` to support pagination and HTMX detection
3. Update `filmList.html` to use include for elements
4. Add `hx-push-url` to film detail links in `filmListElements.html`
5. Add `hx-push-url` to "Your List" button in `filmDetail.html`
6. Test infinite scroll with many films
7. Test URL push state with browser back/forward

---

## UIKit Styling

No new styling required. Existing UIKit classes continue to work.

---

## Technical Notes

### HTMX Request Detection in Quarkus
Check for `HX-Request` header to detect HTMX requests:
```java
@HeaderParam("HX-Request") String hxRequest
// hxRequest will be "true" for HTMX requests, null otherwise
```

### Pagination with Panache
Use Panache's built-in pagination:
```java
Film.find("user = ?1 order by displayOrder", user)
    .page(pageIndex, pageSize)  // pageIndex is 0-based
    .list();
```

### hx-trigger="revealed"
- Fires when element enters viewport
- Combined with `hx-swap="afterend"` to append new content
- Only the last element in the list has this trigger

### hx-push-url
- Updates browser URL without page reload
- Enables browser back/forward navigation
- URL should be a valid route that can be loaded directly

### URL Encoding
Film names in URLs need encoding for special characters:
```html
hx-push-url="/films/{film.name.urlEncode()}"
```

---

## Edge Cases

1. **Empty film list**: No infinite scroll trigger needed
2. **Last page**: Don't include `hx-trigger="revealed"` on last page
3. **Direct URL access**: `/films/{name}` should work as a deep link (optional enhancement)
4. **Special characters in names**: URL encode film names for push-url
