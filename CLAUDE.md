# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Quarkus + HTMX prototype for building a movies application with server-side rendering and dynamic HTML updates.

**Stack:** Quarkus 3.30.3, Java 17, Qute templating, HTMX

## References

- https://getuikit.com/docs/introduction
- https://htmx.org/
- https://quarkus.io/guides/qute-reference
- https://quarkus.io/guides/security-authentication-mechanisms
- https://github.com/quarkusio/quarkus/issues/27389


## Build & Development Commands

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

Dev UI available at http://localhost:8080/q/dev/ during development.

## Architecture

- **REST endpoints** in `src/main/java/` using JAX-RS annotations
- **Qute templates** in `src/main/resources/templates/` for HTML rendering
- **HTMX** for partial page updates without full reloads - endpoints return HTML fragments
- **Configuration** in `src/main/resources/application.properties`

### Quarkus-Qute Pattern

REST endpoints use `@CheckedTemplate` for type-safe template rendering:

```java
@Path("/movies")
public class MovieResource {
    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance list(List<Movie> movies);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.list(movieService.findAll());
    }
}
```

Templates use the naming convention: `templates/MovieResource/list.html`

### HTMX Integration

Endpoints can return partial HTML for HTMX requests:
- Check `HX-Request` header to detect HTMX calls
- Return full page for regular requests, fragments for HTMX
- Use `hx-get`, `hx-post`, `hx-target`, `hx-swap` attributes in templates
