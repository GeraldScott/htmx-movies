# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Quarkus + HTMX prototype for building a movies application with server-side rendering and dynamic HTML updates.

**Stack:** Quarkus 3.30.3, Java 17, Qute templating, HTMX

## References

When you are making a plan to implement a feature, first refer to these resources for guidance and best practices:

- https://htmx.org/
- https://htmx.org/examples/
- https://htmx.org/reference/
- https://htmx.org/docs/
- https://quarkus.io/guides/qute-reference
- https://quarkus.io/guides/getting-started-dev-services
- https://quarkus.io/guides/security-getting-started-tutorial
- https://quarkus.io/guides/security-jpa
- https://quarkus.io/guides/hibernate-orm-panache
- https://getuikit.com/docs/introduction
- https://quarkus.io/guides/security-authentication-mechanisms
- https://github.com/quarkusio/quarkus/issues/27389 (guidance on Logout)

Use Context7 MCP for additional information if the references above are not helpful.

## Build & Development Commands

Don't compile the application after making changes. We are using the Quarkus development mode (`quarkus:dev`) for hot reloading. Instead, ask the user to restart the application after making changes.

The application is available at http://localhost:8080 during development.

Use the Chrome Devtool MCP `chrome-devtools` to test the application. Execute the full test pack in `tasks/TEST-PLAN-MCP.md` after implementing any new user-facing feature. 

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
- For future commits use:
  git status
  git diff
  git add <files>
  git commit -m "message"
