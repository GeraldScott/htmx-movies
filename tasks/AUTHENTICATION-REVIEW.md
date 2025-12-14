# Authentication Implementation Review

> Review of login/register/logout functionality for the htmx-movies Quarkus application, considering production use and scale.

Don't implement local authentication for production use. Rather use OIDC with KeyCloak or third-party identity providers like Google, Facebook, or GitHub.

## Current Implementation Summary

The application uses:
- `quarkus-security-jpa` with BCrypt password hashing
- Form-based authentication with encrypted session cookies
- HTMX-based real-time username validation
- Server-side and client-side validation for registration

---

## 🔴 Critical Issues for Production

### 1. CSRF Protection Gap on Login Form

The `quarkus-rest-csrf` extension cannot protect the login endpoint because `/j_security_check` is not served through JAX-RS. This is a known limitation documented in [GitHub issue #29924](https://github.com/quarkusio/quarkus/issues/29924).

**Current Risk:** Login forms are vulnerable to CSRF attacks where an attacker could trick users into logging into attacker-controlled accounts.

**Recommendation:** Add the `quarkus-rest-csrf` extension and protect the registration form. For login, the SameSite=Strict cookie setting (already configured) provides partial mitigation.

Add dependency to `pom.xml`:
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-csrf</artifactId>
</dependency>
```

Add to `application.properties`:
```properties
quarkus.rest-csrf.form-field-name=csrf-token
quarkus.rest-csrf.token-signature-key=${CSRF_SIGNATURE_KEY:your-32-char-minimum-secret-key-here}
```

Update `register.html` template:
```html
<form method="POST" action="/register" class="uk-form-stacked" autocomplete="off">
    <input type="hidden" name="{inject:csrf.parameterName}" value="{inject:csrf.token}" />
    <!-- ... rest of form -->
</form>
```

---

### 2. Session Encryption Key Length

**Current configuration:**
```properties
%dev.quarkus.http.auth.session.encryption-key=dev-only-key-min-16-chars
```

**Issue:** This key is only 25 characters. Per Quarkus documentation, the encryption key must be at least 16 characters long and is hashed using SHA-256, with the resulting digest used as a key for AES-256 encryption. While technically valid, a longer key provides better entropy.

**Recommendation:** Use a longer, more random key:
```properties
%dev.quarkus.http.auth.session.encryption-key=dev-only-insecure-key-min-32-characters-long
%prod.quarkus.http.auth.session.encryption-key=${SESSION_ENCRYPTION_KEY}
```

For production, generate a secure key:
```bash
openssl rand -base64 32
```

---

### 3. Logout Implementation is Incomplete

**Current implementation:**
```java
NewCookie removeCookie = new NewCookie.Builder("quarkus-credential")
        .path("/")
        .maxAge(0)
        .build();
```

**Issue:** The cookie removal doesn't include all the security attributes that were set when the cookie was created. The authentication information is stored in an encrypted cookie which can be read by all cluster members who share the same encryption key.

**Recommendation:** Match cookie attributes to ensure proper removal:

```java
@POST
@Path("/logout")
public Response logout() {
    NewCookie removeCookie = new NewCookie.Builder("quarkus-credential")
            .path("/")
            .maxAge(0)
            .sameSite(NewCookie.SameSite.STRICT)
            .httpOnly(true)
            .secure(true) // Required for production with HTTPS
            .build();
    return Response.seeOther(URI.create("/"))
            .cookie(removeCookie)
            .build();
}
```

---

## 🟡 Important Improvements

### 4. Add Rate Limiting on Authentication Endpoints

**Current State:** The `/check-username`, `/register`, and login endpoints have no rate limiting.

**Risk:** Vulnerable to brute-force attacks and credential stuffing.

**Recommendation:** Add rate limiting using `quarkus-bucket4j` or implement custom throttling:

```xml
<dependency>
    <groupId>io.quarkiverse.bucket4j</groupId>
    <artifactId>quarkus-bucket4j</artifactId>
    <version>1.2.0</version>
</dependency>
```

Example configuration in `application.properties`:
```properties
quarkus.bucket4j.filters.auth.path=/login.*|/register.*|/check-username.*
quarkus.bucket4j.filters.auth.rate-limits[0].limit=10
quarkus.bucket4j.filters.auth.rate-limits[0].period=1M
```

---

### 5. Username Enumeration Vulnerability

**Current implementation in `/check-username`:**
```java
if (User.existsByUsername(username)) {
    return Templates.usernameFeedback("This username already exists", "danger");
}
```

**Risk:** Attackers can enumerate valid usernames before attempting password attacks.

**Recommendation Options:**

1. **For high-security applications:** Return generic messages or require CAPTCHA after multiple checks
2. **For typical applications:** The UX benefit may outweigh the risk - document as accepted risk
3. **Middle ground:** Add rate limiting specifically to this endpoint and log enumeration attempts

---

### 6. Missing Secure Cookie Flag for Production

**Issue:** The `secure` flag ensures cookies are only sent over HTTPS.

**Recommendation:** Add to `application.properties`:
```properties
%prod.quarkus.http.auth.form.cookie-secure=true
```

---

### 7. Password Policy Enforcement

**Current validation:** Only checks minimum length (6 characters).

**Recommendation:** Implement stronger password policy:

```java
private static final Pattern PASSWORD_PATTERN = Pattern.compile(
    "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$"
);

private boolean isPasswordStrong(String password) {
    if (password == null) return false;
    return PASSWORD_PATTERN.matcher(password).matches();
}

// In register method:
if (!isPasswordStrong(password)) {
    return Response.ok(Templates.register("Register", 
        "Password must be at least 8 characters with uppercase, lowercase, and a number", 
        userName)).build();
}
```

Update client-side validation in `register.html`:
```html
<input class="uk-input" type="password" id="password" name="password"
       placeholder="Password (min 8 chars, uppercase, lowercase, number)" 
       required minlength="8"
       pattern="(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}"
       title="At least 8 characters with uppercase, lowercase, and a number">
```

---

### 8. User Entity - Add Account Status Fields

**Recommendation:** Add fields for account management and security:

```java
@Entity
@Table(name = "users")
@UserDefinition
public class User extends PanacheEntityBase {
    // ... existing fields ...
    
    @Column(nullable = false)
    public boolean enabled = true;
    
    @Column(name = "failed_login_attempts")
    public int failedLoginAttempts = 0;
    
    @Column(name = "locked_until")
    public Instant lockedUntil;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt = Instant.now();
    
    @Column(name = "last_login")
    public Instant lastLogin;
    
    public boolean isAccountLocked() {
        return lockedUntil != null && Instant.now().isBefore(lockedUntil);
    }
}
```

Add corresponding migration `V1.0.2__Add_user_account_fields.sql`:
```sql
ALTER TABLE users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE users ADD COLUMN failed_login_attempts INTEGER DEFAULT 0;
ALTER TABLE users ADD COLUMN locked_until TIMESTAMP;
ALTER TABLE users ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ADD COLUMN last_login TIMESTAMP;

CREATE INDEX idx_users_enabled ON users(enabled);
```

---

## 🟢 Good Practices Already Implemented

| Practice | Status | Notes |
|----------|--------|-------|
| BCrypt password hashing | ✅ | Using `BcryptUtil.bcryptHash()` |
| SameSite=Strict cookie | ✅ | Configured in `application.properties` |
| HttpOnly cookie | ✅ | Prevents XSS from accessing session |
| Session timeout | ✅ | 30 minutes with 1-minute refresh |
| Server-side validation | ✅ | Backs all client-side checks |
| Generic login errors | ✅ | Doesn't reveal username vs password failure |
| Flyway migrations | ✅ | Proper schema management |
| Input pattern validation | ✅ | Username restricted to `[a-zA-Z0-9_]+` |

---

## 🏗️ Production Architecture Recommendations

### Consider OIDC for Scale

For production applications requiring Authorization Code flow or multi-tenant support, `quarkus-oidc` with an OpenID Connect provider like Keycloak provides:

- Bearer token verification
- End user authentication
- Automatic JsonWebKey rotation support
- Social login integration (Google, GitHub, etc.)
- Multi-factor authentication
- Centralized identity management

**When to migrate:**
- Multiple applications sharing authentication
- Need for social login providers
- Regulatory compliance requirements (SOC2, HIPAA)
- User self-service (password reset, profile management)

### Add Security Headers

Create a JAX-RS filter for security headers:

```java
package io.archton.scaffold.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class SecurityHeadersFilter implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        response.getHeaders().add("X-Content-Type-Options", "nosniff");
        response.getHeaders().add("X-Frame-Options", "DENY");
        response.getHeaders().add("X-XSS-Protection", "1; mode=block");
        response.getHeaders().add("Referrer-Policy", "strict-origin-when-cross-origin");
        response.getHeaders().add("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
    }
}
```

### Content Security Policy

Add CSP header for XSS mitigation (adjust based on your CDN usage):

```java
response.getHeaders().add("Content-Security-Policy", 
    "default-src 'self'; " +
    "script-src 'self' https://cdn.jsdelivr.net; " +
    "style-src 'self' https://cdn.jsdelivr.net 'unsafe-inline'; " +
    "img-src 'self' data:; " +
    "font-src 'self' https://cdn.jsdelivr.net; " +
    "connect-src 'self'; " +
    "frame-ancestors 'none';"
);
```

---

## Summary Priority List

| Priority | Issue | Effort | Impact |
|----------|-------|--------|--------|
| 🔴 Critical | Add CSRF protection to registration form | Low | High |
| 🔴 Critical | Fix logout cookie attributes | Low | Medium |
| 🔴 Critical | Add secure cookie flag for production | Low | High |
| 🟡 Medium | Add rate limiting | Medium | High |
| 🟡 Medium | Stronger password policy | Low | Medium |
| 🟡 Medium | Add security headers filter | Low | Medium |
| 🟢 Optional | Account status fields | Medium | Medium |
| 🟢 Optional | Consider OIDC migration | High | High |

---

## Implementation Checklist

- [ ] Add `quarkus-rest-csrf` dependency
- [ ] Configure CSRF token signature key
- [ ] Update registration template with CSRF token
- [ ] Fix logout cookie attributes
- [ ] Add `cookie-secure=true` for production
- [ ] Implement rate limiting on auth endpoints
- [ ] Strengthen password validation
- [ ] Add security headers filter
- [ ] Create migration for user account fields
- [ ] Update User entity with account status
- [ ] Add logging for authentication events
- [ ] Configure production encryption key via environment variable

---

## References

- [Quarkus Security Authentication Mechanisms](https://quarkus.io/guides/security-authentication-mechanisms)
- [Quarkus CSRF Prevention Guide](https://quarkus.io/guides/security-csrf-prevention)
- [Quarkus Security JPA](https://quarkus.io/guides/security-jpa)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [GitHub Issue #27389 - Form Auth Logout](https://github.com/quarkusio/quarkus/issues/27389)
- [GitHub Issue #29924 - Form Auth CSRF](https://github.com/quarkusio/quarkus/issues/29924)
