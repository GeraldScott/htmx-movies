# Phase 1: Authentication Implementation Plan

## Overview

Implement Login and Register functionality for the htmx-movies application, mirroring the Django + HTMX reference project patterns but using Quarkus, Qute templates, and UIKit.

## Reference Implementation (Django + HTMX)

The Django project at `/home/geraldo/htmx/django-htmx/Video #1` provides:

- **Custom User model** extending `AbstractUser`
- **Login** using Django's built-in `LoginView`
- **Register** using `FormView` with `UserCreationForm`
- **Real-time username validation** via HTMX (`hx-post`, `hx-trigger="keyup"`, `hx-target`)
- **Navbar** that shows Login/Register when unauthenticated, Logout when authenticated

## Target Architecture

### Technology Stack
- **Quarkus 3.30.3** with `quarkus-security-jpa` for form-based authentication
- **PostgreSQL** via Quarkus Dev Services (Testcontainers)
- **Flyway** for database migrations
- **Qute templates** for server-side rendering
- **HTMX 2.0.8** for real-time validation without page reloads
- **UIKit 3.24.2** for form styling and layout

---

## Quarkus Dev Services for PostgreSQL

Quarkus Dev Services automatically starts a PostgreSQL container when:
1. `quarkus-jdbc-postgresql` dependency is present
2. `quarkus.datasource.db-kind=postgresql` is configured
3. No explicit JDBC URL is provided

**How it works:**
- Uses **Testcontainers** under the hood
- Container starts automatically with `./mvnw compile quarkus:dev` or during tests
- Zero configuration required for development
- Database is ephemeral (data lost on restart unless volumes configured)

**Requirements:**
- Docker or Podman running on the development machine

**To enable container reuse across restarts:**
Add to `~/.testcontainers.properties`:
```properties
testcontainers.reuse.enable=true
```

---

## Implementation Steps

### Step 1: Add Dependencies

Add to `pom.xml`:
```xml
<!-- Security -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-security-jpa</artifactId>
</dependency>

<!-- Database -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-hibernate-orm-panache</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-jdbc-postgresql</artifactId>
</dependency>

<!-- Migrations -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-flyway</artifactId>
</dependency>
```

### Step 2: Configure Application Properties

**File:** `src/main/resources/application.properties`

```properties
# Database - Dev Services auto-starts PostgreSQL container
quarkus.datasource.db-kind=postgresql
quarkus.hibernate-orm.schema-management.strategy=none
quarkus.flyway.migrate-at-start=true

# Form Authentication
quarkus.http.auth.form.enabled=true
quarkus.http.auth.form.login-page=/login
quarkus.http.auth.form.landing-page=/
quarkus.http.auth.form.error-page=/login?error=true
quarkus.http.auth.form.username-parameter=j_username
quarkus.http.auth.form.password-parameter=j_password

# Production database (optional - for deployment)
%prod.quarkus.datasource.username=movies
%prod.quarkus.datasource.password=movies
%prod.quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/movies
```

### Step 3: Create Flyway Migrations

**File:** `src/main/resources/db/migration/V1.0.0__Create_users_table.sql`

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'user'
);

CREATE INDEX idx_users_username ON users(username);
```

**File:** `src/main/resources/db/migration/V1.0.1__Insert_admin_user.sql`

```sql
-- Password: admin (BCrypt hashed)
INSERT INTO users (username, password, role)
VALUES ('admin', '$2a$10$PrI5Gk9L.tSZiW9FXhTS8O8Mz9E6f5.Xeuv.3mXKqL5wT5v5r5v5r', 'admin');
```

### Step 4: Create User Entity

**File:** `src/main/java/io/archton/scaffold/entity/User.java`

```java
package io.archton.scaffold.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.security.jpa.Password;
import io.quarkus.security.jpa.Roles;
import io.quarkus.security.jpa.UserDefinition;
import io.quarkus.security.jpa.Username;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
@UserDefinition
public class User extends PanacheEntity {

    @Username
    @Column(unique = true, nullable = false, length = 50)
    public String username;

    @Password
    @Column(nullable = false)
    public String password;

    @Roles
    @Column(nullable = false, length = 50)
    public String role = "user";

    public static User findByUsername(String username) {
        return find("username", username).firstResult();
    }

    public static boolean existsByUsername(String username) {
        return count("username", username) > 0;
    }
}
```

### Step 5: Create Authentication Resource

**File:** `src/main/java/io/archton/scaffold/router/AuthResource.java`

```java
package io.archton.scaffold.router;

import io.archton.scaffold.entity.User;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;

@Path("/")
public class AuthResource {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance login(String title, boolean error);
        public static native TemplateInstance register(String title, String error);
    }

    @GET
    @Path("/login")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance loginPage(@QueryParam("error") boolean error) {
        return Templates.login("Login", error);
    }

    @GET
    @Path("/register")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance registerPage() {
        return Templates.register("Register", null);
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response register(
            @FormParam("username") String username,
            @FormParam("password") String password,
            @FormParam("confirmPassword") String confirmPassword) {

        // Validation
        if (username == null || username.isBlank() || username.length() < 3) {
            return Response.ok(Templates.register("Register", "Username must be at least 3 characters")).build();
        }
        if (password == null || password.length() < 6) {
            return Response.ok(Templates.register("Register", "Password must be at least 6 characters")).build();
        }
        if (!password.equals(confirmPassword)) {
            return Response.ok(Templates.register("Register", "Passwords do not match")).build();
        }
        if (User.existsByUsername(username)) {
            return Response.ok(Templates.register("Register", "Username already exists")).build();
        }

        // Create user
        User user = new User();
        user.username = username;
        user.password = BcryptUtil.bcryptHash(password);
        user.role = "user";
        user.persist();

        return Response.seeOther(URI.create("/login")).build();
    }

    @POST
    @Path("/check-username")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public String checkUsername(@FormParam("username") String username) {
        if (username == null || username.isBlank()) {
            return "";
        }
        if (username.length() < 3) {
            return "<span class=\"uk-text-warning\">Username must be at least 3 characters</span>";
        }
        if (User.existsByUsername(username)) {
            return "<span class=\"uk-text-danger\">This username already exists</span>";
        }
        return "<span class=\"uk-text-success\">Username is available</span>";
    }
}
```

### Step 6: Create Login Template

**File:** `src/main/resources/templates/AuthResource/login.html`

```html
{#include base}
<div class="uk-width-2-3@m uk-align-center">
    <h1 class="uk-heading-small">Login</h1>
    <hr class="uk-divider-small">

    {#if error}
    <div class="uk-alert-danger" uk-alert>
        <a class="uk-alert-close" uk-close></a>
        <p>Invalid username or password</p>
    </div>
    {/if}

    <form method="POST" action="/j_security_check" class="uk-form-stacked" autocomplete="off">
        <div class="uk-margin">
            <label class="uk-form-label" for="username">Username</label>
            <div class="uk-form-controls">
                <input class="uk-input" type="text" id="username" name="j_username"
                       placeholder="Username" required>
            </div>
        </div>
        <div class="uk-margin">
            <label class="uk-form-label" for="password">Password</label>
            <div class="uk-form-controls">
                <input class="uk-input" type="password" id="password" name="j_password"
                       placeholder="Password" required>
            </div>
        </div>
        <button type="submit" class="uk-button uk-button-primary">Login</button>
    </form>
</div>
{/include}
```

### Step 7: Create Register Template with HTMX Validation

**File:** `src/main/resources/templates/AuthResource/register.html`

```html
{#include base}
<div class="uk-width-2-3@m uk-align-center">
    <h1 class="uk-heading-small">Register</h1>
    <hr class="uk-divider-small">

    {#if error}
    <div class="uk-alert-danger" uk-alert>
        <a class="uk-alert-close" uk-close></a>
        <p>{error}</p>
    </div>
    {/if}

    <form method="POST" action="/register" class="uk-form-stacked" autocomplete="off">
        <div class="uk-margin">
            <label class="uk-form-label" for="username">Username</label>
            <div class="uk-form-controls">
                <input class="uk-input" type="text" id="username" name="username"
                       placeholder="Username" required
                       hx-post="/check-username"
                       hx-trigger="keyup changed delay:500ms"
                       hx-target="#username-feedback">
                <div id="username-feedback" class="uk-margin-small-top"></div>
            </div>
        </div>
        <div class="uk-margin">
            <label class="uk-form-label" for="password">Password</label>
            <div class="uk-form-controls">
                <input class="uk-input" type="password" id="password" name="password"
                       placeholder="Password (min 6 characters)" required>
            </div>
        </div>
        <div class="uk-margin">
            <label class="uk-form-label" for="confirmPassword">Confirm Password</label>
            <div class="uk-form-controls">
                <input class="uk-input" type="password" id="confirmPassword" name="confirmPassword"
                       placeholder="Confirm Password" required>
            </div>
        </div>
        <button type="submit" class="uk-button uk-button-primary">Register</button>
    </form>
</div>
{/include}
```

### Step 8: Update Navbar for Auth State

**File:** `src/main/resources/templates/base.html`

Update navbar to show different links based on authentication. Inject `SecurityIdentity` in resources and pass `userName` to templates:

```html
<nav class="uk-navbar-container" uk-navbar>
    <div class="uk-navbar-left">
        <a class="uk-navbar-item uk-logo" href="/">Movie List</a>
        <ul class="uk-navbar-nav">
            {#if userName}
            <li><a href="/logout">Logout ({userName})</a></li>
            {#else}
            <li><a href="/login">Login</a></li>
            <li><a href="/register">Register</a></li>
            {/if}
        </ul>
    </div>
</nav>
```

---

## File Structure After Implementation

```
src/main/java/io/archton/scaffold/
├── entity/
│   └── User.java
└── router/
    ├── IndexResource.java
    └── AuthResource.java

src/main/resources/
├── db/migration/
│   ├── V1.0.0__Create_users_table.sql
│   └── V1.0.1__Insert_admin_user.sql
├── templates/
│   ├── base.html (updated with auth-aware navbar)
│   ├── IndexResource/
│   │   └── index.html
│   └── AuthResource/
│       ├── login.html
│       └── register.html
└── application.properties
```

---

## HTMX Integration Points

| Feature | HTMX Attributes | Target |
|---------|-----------------|--------|
| Username validation | `hx-post="/check-username"` `hx-trigger="keyup changed delay:500ms"` | `#username-feedback` |
| Form submission feedback | `hx-indicator` (optional) | Loading spinner |

---

## UIKit Components Used

- `uk-form-stacked` - Vertical form layout
- `uk-input` - Text input styling
- `uk-button uk-button-primary` - Submit button
- `uk-alert-danger` - Error messages
- `uk-text-danger`, `uk-text-success`, `uk-text-warning` - Validation feedback
- `uk-margin` - Spacing between form elements
- `uk-width-2-3@m uk-align-center` - Centered form container
- `uk-divider-small` - Subtle horizontal rule

---

## Security Considerations

1. **Password Hashing:** Use BCrypt via `BcryptUtil.bcryptHash()`
2. **CSRF Protection:** Quarkus handles this automatically with form auth
3. **Input Validation:** Validate username/password length and format
4. **Error Messages:** Don't reveal whether username exists on login failure

---

## Development Workflow

1. Ensure Docker is running
2. Run `./mvnw compile quarkus:dev`
3. Quarkus automatically starts PostgreSQL container via Testcontainers
4. Flyway runs migrations on startup
5. Access app at http://localhost:8080
6. Dev UI available at http://localhost:8080/q/dev/

---

## Testing Checklist

- [ ] Dev Services starts PostgreSQL container automatically
- [ ] Flyway migrations run on startup
- [ ] Register new user successfully
- [ ] Username validation shows error for existing username
- [ ] Username validation shows success for available username
- [ ] Login with valid credentials redirects to home
- [ ] Login with invalid credentials shows error
- [ ] Navbar shows correct links based on auth state
- [ ] Logout clears session and redirects to home
- [ ] Protected routes redirect to login when unauthenticated
