# Phase 6.2: Film Detail View with Photo Upload

## Overview

Add a film detail view that displays when clicking on a film name. The detail view shows film information and allows users to upload a photo for the film. Also adds flash messages that auto-clear.

## Reference Implementation

Django project at `/home/geraldo/htmx/django-htmx/Video #6.2/` provides the pattern.

**New Features (compared to Video #6.1):**
- Clickable film names that load a detail view
- Film detail page showing name, order position, and photo
- Photo upload functionality with multipart form
- "Your List" button to return to film list
- Flash messages that auto-clear after 3 seconds

---

## New Functionality

### Clickable Film Names
```html
<a hx-get="/films/{film.id}/detail" hx-target="#film-list">
    #{film.displayOrder} {film.name}
</a>
```

### Film Detail View
- Shows film name as heading
- Shows order position: "This film is #X in {username}'s list"
- Displays photo if uploaded, or "No photo :(" message
- Photo upload form with file input
- "Your List" button to return to film list

### Photo Upload
```html
<form hx-encoding="multipart/form-data"
      hx-post="/films/{id}/upload-photo"
      hx-target="#film-list">
    <input type="file" name="photo" />
    <button type="submit">Upload File</button>
</form>
```

### Flash Messages (Auto-Clear)
```html
<div class="uk-alert" hx-get="/films/clear" hx-trigger="load delay:3s" hx-swap="outerHTML">
    Added {filmname} to list of films
</div>
```

---

## Files to Create

### 1. Database Migration
**File:** `src/main/resources/db/migration/V1.4.0__Add_film_photo.sql`

```sql
-- Add photo column to films table
ALTER TABLE films ADD COLUMN photo_path VARCHAR(255);
```

### 2. Film Detail Template
**File:** `src/main/resources/templates/FilmResource/filmDetail.html`

```html
<div class="uk-flex uk-flex-between">
    <div>
        <h2 class="uk-text-success uk-margin-bottom">{userfilm.name}</h2>
        <p>This film is #{userfilm.displayOrder} in {userName}'s list</p>
        <button class="uk-button uk-button-primary"
                hx-get="/films/list-partial"
                hx-target="#film-list">Your List</button>
    </div>
    <div>
        {#if userfilm.photoPath}
        <img src="/uploads/{userfilm.photoPath}" style="max-width: 200px; max-height: 200px;" />
        {#else}
        <p class="uk-text-muted">No photo :(</p>
        {/if}

        <form hx-encoding="multipart/form-data"
              hx-post="/films/{userfilm.id}/upload-photo"
              hx-target="#film-list"
              class="uk-margin-top">
            <div class="uk-margin">
                <input class="uk-input" type="file" name="photo" />
            </div>
            <button class="uk-button uk-button-success" type="submit">Upload File</button>
        </form>
    </div>
</div>
```

### 3. Upload Directory Configuration
**File:** Update `src/main/resources/application.properties`

```properties
# File upload configuration
quarkus.http.body.uploads-directory=uploads
quarkus.http.limits.max-body-size=10M
```

---

## Files to Modify

### 1. Film Entity
**File:** `src/main/java/io/archton/scaffold/entity/Film.java`

Add photo path field:

```java
@Column(name = "photo_path", length = 255)
public String photoPath;
```

### 2. FilmResource.java
**File:** `src/main/java/io/archton/scaffold/router/FilmResource.java`

Add new endpoints and update Templates:

```java
@CheckedTemplate
public static class Templates {
    // ... existing templates ...
    public static native TemplateInstance filmDetail(Film userfilm, String userName);
    public static native TemplateInstance filmListMessage(List<Film> films, String message);
}

// Film detail endpoint
@GET
@Path("/{id}/detail")
@Produces(MediaType.TEXT_HTML)
public TemplateInstance detail(@PathParam("id") Long id) {
    String userName = securityIdentity.getPrincipal().getName();
    User user = User.findByUsername(userName);

    Film film = Film.find("id = ?1 and user = ?2", id, user).firstResult();
    if (film == null) {
        throw new NotFoundException();
    }

    return Templates.filmDetail(film, userName);
}

// Return to list partial
@GET
@Path("/list-partial")
@Produces(MediaType.TEXT_HTML)
public TemplateInstance listPartial() {
    String userName = securityIdentity.getPrincipal().getName();
    User user = User.findByUsername(userName);
    List<Film> films = Film.findByUser(user);
    return Templates.filmList(films);
}

// Upload photo endpoint
@POST
@Path("/{id}/upload-photo")
@Consumes(MediaType.MULTIPART_FORM_DATA)
@Produces(MediaType.TEXT_HTML)
@Transactional
public TemplateInstance uploadPhoto(@PathParam("id") Long id,
                                    @MultipartForm FileUploadForm form) {
    String userName = securityIdentity.getPrincipal().getName();
    User user = User.findByUsername(userName);

    Film film = Film.find("id = ?1 and user = ?2", id, user).firstResult();
    if (film == null) {
        throw new NotFoundException();
    }

    // Save file and update film
    String filename = saveFile(form.photo, film.id);
    film.photoPath = filename;

    return Templates.filmDetail(film, userName);
}

// Clear message endpoint
@GET
@Path("/clear")
@Produces(MediaType.TEXT_HTML)
public String clear() {
    return "";
}
```

### 3. Create FileUploadForm Class
**File:** `src/main/java/io/archton/scaffold/router/FileUploadForm.java`

```java
package io.archton.scaffold.router;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.multipart.FileUpload;

public class FileUploadForm {
    @FormParam("photo")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    public FileUpload photo;
}
```

### 4. Update filmList.html
**File:** `src/main/resources/templates/FilmResource/filmList.html`

Make film names clickable and add flash message support:

```html
<div class="uk-grid uk-child-width-1-2@m" uk-grid>
    <div>
        {#if message}
        <div class="uk-alert uk-alert-success" hx-get="/films/clear" hx-trigger="load delay:3s" hx-swap="outerHTML">
            {message}
        </div>
        {/if}

        {#if films && films.size > 0}
        <form class="sortable" hx-post="/films/reorder" hx-trigger="end" hx-target="#film-list">
            <ul class="uk-list uk-list-striped">
                {#for film in films}
                <div>
                    <input type="hidden" name="film_order" value="{film.id}" />
                    <li class="uk-flex uk-flex-between uk-flex-middle">
                        <span class="drag-handle" style="cursor: grab; margin-right: 10px;" uk-icon="icon: menu"></span>
                        <a class="uk-flex-1" style="cursor: pointer;"
                           hx-get="/films/{film.id}/detail"
                           hx-target="#film-list">
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

### 5. Update add() Endpoint
**File:** `src/main/java/io/archton/scaffold/router/FilmResource.java`

Return filmListMessage template with success message:

```java
@POST
@Path("/add")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_HTML)
@Transactional
public TemplateInstance add(@FormParam("filmname") String filmname) {
    // ... existing code ...

    List<Film> films = Film.findByUser(user);
    return Templates.filmListMessage(films, "Added " + filmname + " to list of films");
}
```

### 6. Static File Serving for Uploads
**File:** Update `src/main/resources/application.properties`

```properties
# Serve uploaded files
quarkus.http.static-resources./uploads.path=/uploads
quarkus.http.static-resources./uploads.methods=GET
```

---

## HTMX Integration

| Feature | Attributes | Purpose |
|---------|------------|---------|
| Film detail link | `hx-get="/films/{id}/detail"` `hx-target="#film-list"` | Load detail view |
| Return to list | `hx-get="/films/list-partial"` `hx-target="#film-list"` | Return from detail |
| Photo upload | `hx-encoding="multipart/form-data"` `hx-post="/films/{id}/upload-photo"` | Upload file |
| Auto-clear message | `hx-get="/films/clear"` `hx-trigger="load delay:3s"` `hx-swap="outerHTML"` | Clear flash message |

**Flow:**
1. User clicks film name → loads detail view in `#film-list`
2. Detail shows film info and photo (or upload form)
3. User can upload photo → returns to detail with photo displayed
4. User clicks "Your List" → returns to film list
5. Flash messages auto-clear after 3 seconds

---

## Implementation Order

1. Create Flyway migration to add `photo_path` column
2. Update Film entity with `photoPath` field
3. Create FileUploadForm class for multipart handling
4. Create filmDetail.html template
5. Add detail, listPartial, uploadPhoto, and clear endpoints to FilmResource
6. Update filmList.html with clickable film names and message support
7. Update add() to return message template
8. Configure static file serving for uploads
9. Test the complete flow

---

## UIKit Styling

- `uk-text-success` - Green heading for film name
- `uk-button uk-button-primary` - "Your List" button
- `uk-button uk-button-success` - Upload button
- `uk-alert uk-alert-success` - Flash message styling
- `uk-input` - File input styling

---

## Technical Notes

### Multipart File Upload in Quarkus
- Use `@MultipartForm` annotation
- Create form class with `@FormParam` and `@PartType`
- Use `FileUpload` type from RESTEasy Reactive

### File Storage
- Store files in `uploads/` directory
- Generate unique filename using film ID + original extension
- Store relative path in database (`photo_path` column)

### Static File Serving
- Configure Quarkus to serve `/uploads` directory
- Files accessible at `/uploads/{filename}`

### Security Considerations
- Validate file belongs to current user before upload
- Limit file size (10MB default)
- Only allow image file types (validate extension/MIME type)
