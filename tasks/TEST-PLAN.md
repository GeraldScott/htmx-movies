# Authentication Test Plan

Manual testing procedures for Phase 1 authentication features using curl.

## Prerequisites

1. Docker running (for Quarkus Dev Services PostgreSQL)
2. Dev server running: `./mvnw compile quarkus:dev`
3. Application accessible at http://localhost:8080

---

## 1. Page Rendering Tests

### 1.1 Home Page
```bash
curl -s http://localhost:8080/
```
**Expected:** HTML page with navbar showing "Login" and "Register" links.

### 1.2 Login Page
```bash
curl -s http://localhost:8080/login
```
**Expected:** HTML form with `j_username` and `j_password` fields, posting to `/j_security_check`.

### 1.3 Register Page
```bash
curl -s http://localhost:8080/register
```
**Expected:** HTML form with username, password, confirmPassword fields. Username field has HTMX attributes for real-time validation.

---

## 2. HTMX Username Validation Tests

### 2.1 Existing Username
```bash
curl -s -X POST -d "username=admin" http://localhost:8080/check-username
```
**Expected:**
```html
<span class="uk-text-danger">This username already exists</span>
```

### 2.2 Available Username
```bash
curl -s -X POST -d "username=newuser123" http://localhost:8080/check-username
```
**Expected:**
```html
<span class="uk-text-success">Username is available</span>
```

### 2.3 Username Too Short
```bash
curl -s -X POST -d "username=ab" http://localhost:8080/check-username
```
**Expected:**
```html
<span class="uk-text-warning">Username must be at least 3 characters</span>
```

### 2.4 Empty Username
```bash
curl -s -X POST -d "username=" http://localhost:8080/check-username
```
**Expected:** Empty response (no validation message).

---

## 3. Registration Tests

### 3.1 Successful Registration
```bash
curl -s -X POST \
  -d "username=testuser&password=testpass123&confirmPassword=testpass123" \
  http://localhost:8080/register \
  -D - -o /dev/null
```
**Expected:** `HTTP/1.1 303 See Other` with `Location: http://localhost:8080/login`

### 3.2 Password Mismatch
```bash
curl -s -X POST \
  -d "username=testuser2&password=pass123&confirmPassword=different" \
  http://localhost:8080/register
```
**Expected:** HTML page with error alert "Passwords do not match".

### 3.3 Username Too Short
```bash
curl -s -X POST \
  -d "username=ab&password=testpass123&confirmPassword=testpass123" \
  http://localhost:8080/register
```
**Expected:** HTML page with error alert "Username must be at least 3 characters".

### 3.4 Password Too Short
```bash
curl -s -X POST \
  -d "username=testuser3&password=short&confirmPassword=short" \
  http://localhost:8080/register
```
**Expected:** HTML page with error alert "Password must be at least 6 characters".

### 3.5 Duplicate Username
```bash
curl -s -X POST \
  -d "username=admin&password=testpass123&confirmPassword=testpass123" \
  http://localhost:8080/register
```
**Expected:** HTML page with error alert "Username already exists".

---

## 4. Login Tests

### 4.1 Successful Login
```bash
curl -s -X POST \
  -d "j_username=testuser&j_password=testpass123" \
  http://localhost:8080/j_security_check \
  -c /tmp/cookies.txt \
  -D - -o /dev/null
```
**Expected:**
- `HTTP/1.1 302 Found`
- `location: http://localhost:8080/`
- `set-cookie: quarkus-credential=...`

### 4.2 Invalid Credentials
```bash
curl -s -X POST \
  -d "j_username=wrong&j_password=wrong" \
  http://localhost:8080/j_security_check \
  -D - -o /dev/null
```
**Expected:**
- `HTTP/1.1 302 Found`
- `Location: http://localhost:8080/login?error=true`

### 4.3 Login Error Page
```bash
curl -s "http://localhost:8080/login?error=true"
```
**Expected:** HTML page with error alert "Invalid username or password".

---

## 5. Authenticated Session Tests

### 5.1 Navbar Shows Username
```bash
# First login to get session cookie
curl -s -X POST \
  -d "j_username=testuser&j_password=testpass123" \
  http://localhost:8080/j_security_check \
  -c /tmp/cookies.txt \
  -o /dev/null

# Then check home page with cookie
curl -s -b /tmp/cookies.txt http://localhost:8080/ | grep -E "(Logout|testuser)"
```
**Expected:** Output contains `Logout (testuser)`

### 5.3 Logout
```bash
curl -s -X POST -b /tmp/cookies.txt -c /tmp/cookies.txt \
  http://localhost:8080/logout \
  -w "HTTP: %{http_code}\n" -o /dev/null
```
**Expected:** `HTTP: 303` (redirect to home, session cookie cleared)

### 5.2 Authenticated User on Login Page
```bash
curl -s -b /tmp/cookies.txt http://localhost:8080/login | grep -A5 "uk-navbar-nav"
```
**Expected:** Navbar shows "Logout (testuser)" instead of Login/Register links.

---

## 6. Full Flow Integration Test

Complete user journey from registration to authenticated session:

```bash
# 1. Check username availability
curl -s -X POST -d "username=flowtest" http://localhost:8080/check-username
# Expected: Username is available

# 2. Register new user
curl -s -X POST \
  -d "username=flowtest&password=flowpass123&confirmPassword=flowpass123" \
  http://localhost:8080/register \
  -D - -o /dev/null
# Expected: 303 redirect to /login

# 3. Login with new user
curl -s -X POST \
  -d "j_username=flowtest&j_password=flowpass123" \
  http://localhost:8080/j_security_check \
  -c /tmp/flow-cookies.txt \
  -D - -o /dev/null
# Expected: 302 redirect to / with session cookie

# 4. Verify authenticated state
curl -s -b /tmp/flow-cookies.txt http://localhost:8080/ | grep "Logout (flowtest)"
# Expected: Match found

# 5. Cleanup
rm /tmp/flow-cookies.txt
```

---

## HTTP Response Code Reference

| Code | Meaning |
|------|---------|
| 200 | Success - page rendered |
| 302 | Redirect after login attempt |
| 303 | Redirect after successful registration |
| 500 | Server error (check logs) |

---

## Troubleshooting

### Database Issues
If you get "relation does not exist" errors, the PostgreSQL container may need to be recreated:
```bash
# Stop dev server, then:
docker ps -a | grep postgres | awk '{print $1}' | xargs docker rm -f
# Restart dev server
```

### Cookie Issues
Clear test cookies between test runs:
```bash
rm /tmp/cookies.txt /tmp/admin-cookies.txt /tmp/flow-cookies.txt 2>/dev/null
```

### Check Flyway Migrations
Access Dev UI at http://localhost:8080/q/dev-ui/io.quarkus.quarkus-flyway/migrations to verify migrations ran successfully.
