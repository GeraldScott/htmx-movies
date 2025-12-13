# Authentication Test Plan (Chrome DevTools MCP)

Browser-based testing procedures for Phase 1 authentication features using Chrome DevTools MCP.

## Prerequisites

1. Docker running (for Quarkus Dev Services PostgreSQL)
2. Dev server running: `./mvnw compile quarkus:dev`
3. Application accessible at http://localhost:8080
4. Chrome browser with DevTools MCP server connected

---

## 1. Page Rendering Tests

### 1.1 Home Page
**Action:** Navigate to `http://localhost:8080/`

**Expected:**
- Page title: "Movie List"
- Navbar contains "LOGIN" link to `/login`
- Navbar contains "REGISTER" link to `/register`
- Heading "Welcome!" displayed

### 1.2 Login Page
**Action:** Click "LOGIN" link or navigate to `http://localhost:8080/login`

**Expected:**
- Heading "Login" displayed
- Username textbox (required)
- Password textbox (required)
- "LOGIN" button

### 1.3 Register Page
**Action:** Click "REGISTER" link or navigate to `http://localhost:8080/register`

**Expected:**
- Heading "Register" displayed
- Username textbox (required)
- Password textbox (required)
- Confirm Password textbox (required)
- "REGISTER" button

---

## 2. HTMX Username Validation Tests

All tests performed on the Register page (`/register`).

### 2.1 Existing Username
**Action:**
1. Fill username field with `admin`
2. Click/tab to another field (trigger blur)

**Expected:** Text "This username already exists" appears below username field.

### 2.2 Available Username
**Action:**
1. Fill username field with `newuser123` (or any unused name)
2. Click/tab to another field (trigger blur)

**Expected:** Text "Username is available" appears below username field.

### 2.3 Username Too Short
**Action:**
1. Fill username field with `ab` (less than 3 characters)
2. Click/tab to another field (trigger blur)

**Expected:** Text "Username must be at least 3 characters" appears below username field.

### 2.4 Empty Username
**Action:**
1. Clear username field
2. Click/tab to another field (trigger blur)

**Expected:** Text "Username must be at least 3 characters" appears (validation message for empty/short input).

---

## 3. Browser-Level Validation Tests

All tests performed on the Register page (`/register`). These test HTML5 validation attributes.

### 3.1 Username Pattern - Invalid Characters
**Action:**
1. Fill username field with `user@name!` (contains invalid characters)
2. Click "REGISTER" button

**Expected:** Browser shows validation error "Only letters, numbers, and underscores allowed" (or similar pattern mismatch message). Form does not submit.

### 3.2 Username Minlength
**Action:**
1. Fill username field with `ab` (2 characters)
2. Fill password fields with valid values
3. Click "REGISTER" button

**Expected:** Browser shows validation error about minimum length. Form does not submit.

### 3.3 Password Minlength
**Action:**
1. Fill username field with valid value
2. Fill password field with `12345` (5 characters, less than 6)
3. Fill confirm password with `12345`
4. Click "REGISTER" button

**Expected:** Browser shows validation error about minimum length. Form does not submit.

### 3.4 Password Mismatch (Client-Side)
**Action:**
1. Fill username field with valid value
2. Fill password field with `password123`
3. Fill confirm password with `different123`
4. Click "REGISTER" button

**Expected:** Browser shows validation error "Passwords do not match". Form does not submit.

### 3.5 Password Match Updates on Password Change
**Action:**
1. Fill password field with `password123`
2. Fill confirm password with `password123`
3. Go back and change password field to `changed123`
4. Click "REGISTER" button

**Expected:** Browser shows validation error "Passwords do not match" (validation updates when password changes).

---

## 4. Registration Tests

### 4.1 Successful Registration
**Action:**
1. Navigate to `/register`
2. Fill form:
   - Username: `testuser` (unique name)
   - Password: `testpass123`
   - Confirm Password: `testpass123`
3. Click "REGISTER" button

**Expected:**
- Page redirects to `/login`
- No error messages displayed

### 4.2 Password Mismatch (Server-Side)
**Action:**
1. Navigate to `/register`
2. Fill form:
   - Username: `testuser2`
   - Password: `pass123`
   - Confirm Password: `different`
3. Click "REGISTER" button

**Expected:**
- Stays on `/register`
- Alert with "Passwords do not match" displayed

### 4.3 Username Too Short (Server-Side)
**Action:**
1. Navigate to `/register`
2. Fill form:
   - Username: `ab`
   - Password: `testpass123`
   - Confirm Password: `testpass123`
3. Click "REGISTER" button

**Expected:**
- Stays on `/register`
- Alert with "Username must be at least 3 characters" displayed

### 4.4 Password Too Short (Server-Side)
**Action:**
1. Navigate to `/register`
2. Fill form:
   - Username: `testuser3`
   - Password: `short`
   - Confirm Password: `short`
3. Click "REGISTER" button

**Expected:**
- Stays on `/register`
- Alert with "Password must be at least 6 characters" displayed

### 4.5 Duplicate Username
**Action:**
1. Navigate to `/register`
2. Fill form:
   - Username: `admin` (existing user)
   - Password: `testpass123`
   - Confirm Password: `testpass123`
3. Click "REGISTER" button

**Expected:**
- Stays on `/register`
- Alert with "Username already exists" displayed

---

## 5. Login Tests

### 5.1 Successful Login
**Action:**
1. Navigate to `/login`
2. Fill form:
   - Username: `testuser` (valid user)
   - Password: `testpass123`
3. Click "LOGIN" button

**Expected:**
- Redirects to `/` (home page)
- Navbar shows "LOGOUT (USERNAME)" button instead of LOGIN/REGISTER links

### 5.2 Invalid Credentials
**Action:**
1. Navigate to `/login`
2. Fill form:
   - Username: `wrong`
   - Password: `wrong`
3. Click "LOGIN" button

**Expected:**
- Redirects to `/login?error=true`
- Alert with "Invalid username or password" displayed

### 5.3 Login Error Page
**Action:** Navigate directly to `http://localhost:8080/login?error=true`

**Expected:**
- Login form displayed
- Alert with "Invalid username or password" visible

---

## 6. Authenticated Session Tests

### 6.1 Navbar Shows Username
**Action:**
1. Login with valid credentials
2. Observe navbar

**Expected:**
- Navbar displays "LOGOUT (USERNAME)" button
- LOGIN and REGISTER links are not visible

### 6.2 Authenticated User on Login Page
**Action:**
1. Login with valid credentials
2. Navigate to `/login`

**Expected:**
- Login form displayed
- Navbar still shows "LOGOUT (USERNAME)" button (session persists)

### 6.3 Logout
**Action:**
1. While logged in, click "LOGOUT (USERNAME)" button

**Expected:**
- Redirects to `/` (home page)
- Navbar shows "LOGIN" and "REGISTER" links again
- Session cookie cleared

---

## 7. Full Flow Integration Test

Complete user journey from registration to authenticated session.

### Steps:

1. **Check username availability**
   - Navigate to `/register`
   - Enter unique username (e.g., `flowtest`)
   - Tab to next field
   - Verify "Username is available" message

2. **Register new user**
   - Fill password: `flowpass123`
   - Fill confirm password: `flowpass123`
   - Click "REGISTER"
   - Verify redirect to `/login`

3. **Login with new user**
   - Fill username: `flowtest`
   - Fill password: `flowpass123`
   - Click "LOGIN"
   - Verify redirect to `/`

4. **Verify authenticated state**
   - Confirm navbar shows "LOGOUT (FLOWTEST)"

5. **Logout**
   - Click logout button
   - Verify redirect to `/`
   - Confirm navbar shows LOGIN/REGISTER links

---

## MCP Tool Reference

| Tool | Purpose |
|------|---------|
| `navigate_page` | Navigate to URL or reload |
| `take_snapshot` | Get page accessibility tree |
| `click` | Click element by uid |
| `fill` | Fill single input field |
| `fill_form` | Fill multiple form fields |
| `list_pages` | List open browser tabs |

### Snapshot Element Identification

Elements are identified by `uid` in the snapshot. Example:
```
uid=1_4 link "LOGIN" url="http://localhost:8080/login"
uid=1_10 textbox "Username" required
uid=1_13 button "LOGIN"
```

Use the `uid` value with `click` and `fill` tools.

---

## Test Results Template

| Category | Test | Status |
|----------|------|--------|
| Page Rendering | 1.1 Home Page | |
| Page Rendering | 1.2 Login Page | |
| Page Rendering | 1.3 Register Page | |
| HTMX Validation | 2.1 Existing Username | |
| HTMX Validation | 2.2 Available Username | |
| HTMX Validation | 2.3 Username Too Short | |
| HTMX Validation | 2.4 Empty Username | |
| Browser Validation | 3.1 Username Pattern | |
| Browser Validation | 3.2 Username Minlength | |
| Browser Validation | 3.3 Password Minlength | |
| Browser Validation | 3.4 Password Mismatch | |
| Browser Validation | 3.5 Password Change Update | |
| Registration | 4.1 Successful Registration | |
| Registration | 4.2 Password Mismatch | |
| Registration | 4.3 Username Too Short | |
| Registration | 4.4 Password Too Short | |
| Registration | 4.5 Duplicate Username | |
| Login | 5.1 Successful Login | |
| Login | 5.2 Invalid Credentials | |
| Login | 5.3 Login Error Page | |
| Session | 6.1 Navbar Shows Username | |
| Session | 6.2 Auth User on Login Page | |
| Session | 6.3 Logout | |
| Integration | 7. Full Flow | |

**Total: ___ / 24 tests**
