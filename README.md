# htmx-movies

> Quarkus+HTMX prototype

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

```bash
# Run in dev mode (hot reload enabled)
./mvnw compile quarkus:dev

# Run tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=TestClassName

# Run a single test method
./mvnw test -Dtest=TestClassName#methodName

# Package application
./mvnw package

# Build native executable (requires GraalVM)
./mvnw package -Dnative

# Build native using container (no GraalVM needed)
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

## HTMX and user functionality

This section documents how HTMX enables dynamic user interactions without writing custom JavaScript. Each use case demonstrates a specific HTMX pattern.

### 1. Username Availability Check (Registration)

**File:** `templates/AuthResource/register.html`

```html
<input class="uk-input" type="text" id="username" name="username"
       hx-post="/check-username"
       hx-trigger="keyup changed delay:500ms"
       hx-target="#username-feedback">
<div id="username-feedback"></div>
```

**How it works:**
- `hx-post="/check-username"` sends a POST request to validate the username
- `hx-trigger="keyup changed delay:500ms"` fires 500ms after the user stops typing (debounced)
- `hx-target="#username-feedback"` replaces the feedback div with the server response
- Server returns a small HTML fragment (`usernameFeedback.html`) showing "Available" or "Taken"

**Pattern:** Real-time form field validation with debouncing.

---

### 2. Add Film to List

**File:** `templates/FilmResource/films.html`

```html
<form hx-post="/films/add" hx-target="#film-list">
    <input type="text" name="filmname" placeholder="Enter a film" />
    <button type="submit">Add Film</button>
</form>
```

**How it works:**
- `hx-post="/films/add"` submits the form via AJAX
- `hx-target="#film-list"` replaces the entire film list with the updated list from the server
- No page reload - the new film appears instantly in the list

**Pattern:** Form submission with partial page update.

---

### 3. Add Film from Search Results

**File:** `templates/FilmResource/searchResults.html`

```html
<span class="uk-badge"
      hx-post="/films/add"
      hx-vals='{"filmname": "{film.name}"}'
      hx-target="#film-list">add</span>
```

**How it works:**
- `hx-post="/films/add"` sends a POST request when the badge is clicked
- `hx-vals` injects the film name as a form parameter without a form element
- `hx-target="#film-list"` updates the user's film list with the response

**Pattern:** Click-to-action with inline parameter injection.

---

### 4. Live Search with Debounce

**File:** `templates/FilmResource/search.html`

```html
<input type="text"
    hx-post="/films/search"
    hx-target="#search-results"
    hx-trigger="keyup changed delay:500ms"
    name="search"
    placeholder="Search films..." />
<div id="search-results"></div>
```

**How it works:**
- `hx-trigger="keyup changed delay:500ms"` waits until user stops typing
- `hx-post="/films/search"` queries the film catalog
- `hx-target="#search-results"` displays matching films below the search box
- Server returns `searchResults.html` fragment with clickable "add" badges

**Pattern:** Type-ahead search with debounced requests.

---

### 5. Delete Film with Confirmation

**File:** `templates/FilmResource/filmListElements.html`

```html
<span class="uk-badge"
      hx-delete="/films/{film.id}"
      hx-target="#film-list"
      hx-confirm="Are you sure you wish to delete?">X</span>
```

**How it works:**
- `hx-delete` sends a DELETE request (RESTful verb)
- `hx-confirm` shows a browser confirmation dialog before the request
- `hx-target="#film-list"` refreshes the list after deletion

**Pattern:** Destructive action with user confirmation.

---

### 6. View Film Detail with URL Push State

**File:** `templates/FilmResource/filmListElements.html`

```html
<a hx-get="/films/{film.id}/detail"
   hx-target="#film-list"
   hx-push-url="/films/{film.name}">
    {film.name}
</a>
```

**How it works:**
- `hx-get` fetches the detail view as an HTML fragment
- `hx-target="#film-list"` replaces the list with the detail view
- `hx-push-url` updates the browser URL for bookmarking and history navigation

**Pattern:** SPA-like navigation with browser history support.

---

### 7. Navigate Back to List

**File:** `templates/FilmResource/filmDetail.html`

```html
<button hx-get="/films/list-partial"
        hx-target="#film-list"
        hx-push-url="/films">Your List</button>
```

**How it works:**
- Returns to the film list view without a full page reload
- `hx-push-url="/films"` restores the list URL in the browser

**Pattern:** In-page navigation maintaining URL state.

---

### 8. File Upload

**File:** `templates/FilmResource/filmDetail.html`

```html
<form hx-encoding="multipart/form-data"
      hx-post="/films/{userfilm.id}/upload-photo"
      hx-target="#film-list">
    <input type="file" name="photo" />
    <button type="submit">Upload File</button>
</form>
```

**How it works:**
- `hx-encoding="multipart/form-data"` enables file uploads via HTMX
- `hx-post` sends the file to the server
- `hx-target="#film-list"` refreshes the detail view to show the uploaded image

**Pattern:** AJAX file upload with automatic UI refresh.

---

### 9. Drag-and-Drop Reordering

**File:** `templates/FilmResource/filmList.html`

```html
<form class="sortable" hx-post="/films/reorder" hx-trigger="end" hx-target="#film-list">
    <ul class="uk-list">
        {#for film in films}
        <input type="hidden" name="film_order" value="{film.id}" />
        <li><span class="drag-handle">...</span> {film.name}</li>
        {/for}
    </ul>
</form>
```

**How it works:**
- Sortable.js handles the drag-and-drop UI (initialized in `base.html`)
- `hx-trigger="end"` fires when the user finishes dragging (Sortable.js event)
- Hidden inputs with `name="film_order"` send the new order to the server
- Server receives film IDs in their new display order

**Pattern:** Integration with third-party JS library (Sortable.js) using custom event triggers.

---

### 10. Infinite Scroll

**File:** `templates/FilmResource/filmListElements.html`

```html
{#for film in films}
{#if film_isLast && hasMorePages}
<div hx-get="/films?page={nextPage}" hx-trigger="revealed" hx-swap="afterend">
{#else}
<div>
{/if}
    <!-- film content -->
</div>
{/for}
```

**How it works:**
- The last item in each page contains the infinite scroll trigger
- `hx-trigger="revealed"` fires when the element scrolls into view
- `hx-get="/films?page={nextPage}"` fetches the next page of films
- `hx-swap="afterend"` appends new items after the current last item (not replacing)
- Server returns more `filmListElements.html` with incremented `nextPage`

**Pattern:** Lazy loading with viewport intersection detection.

---

### 11. Auto-Dismissing Messages

**File:** `templates/FilmResource/filmListMessage.html`

```html
<div class="uk-alert uk-alert-success"
     hx-get="/films/clear"
     hx-trigger="load delay:3s"
     hx-swap="outerHTML">
    {message}
</div>
```

**How it works:**
- `hx-trigger="load delay:3s"` fires 3 seconds after the element loads
- `hx-get="/films/clear"` fetches an empty response
- `hx-swap="outerHTML"` removes the entire alert element

**Pattern:** Self-removing UI elements with timed triggers.

---

### Summary of HTMX Attributes Used

| Attribute | Purpose |
|-----------|---------|
| `hx-get`, `hx-post`, `hx-delete` | HTTP verbs for AJAX requests |
| `hx-target` | CSS selector for element to update |
| `hx-trigger` | Event that initiates the request |
| `hx-swap` | How to insert the response (`innerHTML`, `outerHTML`, `afterend`) |
| `hx-push-url` | Update browser URL for history/bookmarks |
| `hx-confirm` | Show confirmation dialog before request |
| `hx-vals` | Inject additional parameters into request |
| `hx-encoding` | Set form encoding (for file uploads) |

