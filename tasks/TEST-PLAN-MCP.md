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
| `handle_dialog` | Accept or dismiss browser dialogs (alert, confirm, prompt) |

### Snapshot Element Identification

Elements are identified by `uid` in the snapshot. Example:
```
uid=1_4 link "LOGIN" url="http://localhost:8080/login"
uid=1_10 textbox "Username" required
uid=1_13 button "LOGIN"
```

Use the `uid` value with `click` and `fill` tools.

### Handling Browser Dialogs (hx-confirm)

When clicking elements with `hx-confirm` attribute, the browser shows a confirmation dialog that **blocks execution**. The `click` tool will timeout waiting for the dialog.

**Pattern for confirmation dialogs:**
1. Call `click` on the element with `hx-confirm` - expect timeout error
2. Call `handle_dialog` with `action: "accept"` to confirm, or `action: "dismiss"` to cancel
3. Call `take_snapshot` to verify the result

**Example:**
```
click(uid="delete-button")  → Timeout (dialog blocking)
handle_dialog(action="accept")  → Dialog dismissed, action proceeds
take_snapshot()  → Verify item was deleted
```

**Note:** The timeout on click is expected behavior, not an error.

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

---

## 8. Film Feature Tests

Tests for the dynamic film list feature (Phase 2).

### 8.1 Films Page - Unauthenticated Access
**Action:** Navigate to `http://localhost:8080/films` without logging in

**Expected:** Redirects to `/login` page

### 8.2 Films Page - Authenticated Access
**Action:**
1. Login with valid credentials
2. Navigate to `/films`

**Expected:**
- Heading "My Films" displayed
- Input field with placeholder "Enter a film"
- "Add Film" button
- Empty film list message or existing films

### 8.3 Add Film - HTMX Dynamic Update
**Action:**
1. Navigate to `/films` (logged in)
2. Enter "The Godfather" in the input field
3. Click "Add Film" button

**Expected:**
- Film "The Godfather" appears in list without page reload
- No page flash/reload visible
- Film appears in striped list format

### 8.4 Add Multiple Films
**Action:**
1. Add "Taxi Driver"
2. Add "Fargo"
3. Add "Big Lebowski"

**Expected:** All three films appear in list in order added

### 8.5 Films Persist After Logout/Login
**Action:**
1. Add some films
2. Logout
3. Login again
4. Navigate to `/films`

**Expected:** Previously added films still visible

### 8.6 Films Are User-Specific
**Action:**
1. Login as user1, add "Film A"
2. Logout
3. Register/login as user2
4. Navigate to `/films`

**Expected:** Film list is empty (user2 has no films)

### 8.7 Navbar Shows Films Link When Authenticated
**Action:** Login and check navbar

**Expected:** "Films" link visible in navbar between logo and Logout

### 8.8 Navbar Hides Films Link When Not Authenticated
**Action:** Logout and check navbar

**Expected:** "Films" link NOT visible in navbar (only Login/Register shown)

---

## Film Test Results Template

| Category | Test | Status |
|----------|------|--------|
| Film Access | 8.1 Unauthenticated Access | |
| Film Access | 8.2 Authenticated Access | |
| Film HTMX | 8.3 Add Film Dynamic Update | |
| Film HTMX | 8.4 Add Multiple Films | |
| Film Data | 8.5 Films Persist | |
| Film Data | 8.6 User-Specific Films | |
| Film Navbar | 8.7 Films Link Visible | |
| Film Navbar | 8.8 Films Link Hidden | |

**Film Tests: ___ / 8 tests**

---

## 9. Delete Film Feature Tests

Tests for the delete film functionality (Phase 3).

**Important:** Delete buttons use `hx-confirm` which triggers browser dialogs. See "Handling Browser Dialogs" section above for the MCP tool pattern.

### 9.1 Delete Button Visible
**Action:**
1. Login and navigate to `/films`
2. Add a film if none exist
3. Take snapshot and observe the film list

**Expected:**
- Each film has an "X" delete button/badge next to it
- Delete button appears as StaticText "X" in snapshot

### 9.2 Delete Confirmation Dialog
**Action:**
1. Click the "X" delete button on a film
2. Observe that click times out (dialog is blocking)

**Expected:**
- Click tool returns timeout error (this is expected)
- Dialog is blocking - use `handle_dialog` to proceed

### 9.3 Delete Cancelled
**Action:**
1. Click "X" on a film (will timeout)
2. Call `handle_dialog` with `action: "dismiss"`
3. Take snapshot

**Expected:**
- Film remains in the list
- No changes to the film list

### 9.4 Delete Confirmed - HTMX Update
**Action:**
1. Add a film "Test Film to Delete"
2. Click "X" on that film (will timeout)
3. Call `handle_dialog` with `action: "accept"`
4. Take snapshot

**Expected:**
- Film is removed from the list without page reload
- URL remains `/films` (no navigation)
- Other films remain unchanged

### 9.5 Delete Last Film
**Action:**
1. Ensure only one film exists in list
2. Click "X" on the film (will timeout)
3. Call `handle_dialog` with `action: "accept"`
4. Take snapshot

**Expected:**
- Film list shows empty state message "You do not have any films in your list"

### 9.6 Delete Multiple Films
**Action:**
1. Add 3 films: "Film A", "Film B", "Film C"
2. Delete "Film B": click X → handle_dialog(accept) → snapshot
3. Delete "Film A": click X → handle_dialog(accept) → snapshot

**Expected:**
- After step 2: "Film A" and "Film C" remain
- After step 3: Only "Film C" remains

### 9.7 Delete Only Affects Current User
**Action:**
1. Login as user1, add "Shared Film Name"
2. Logout, login as user2, add "Shared Film Name"
3. Delete the film as user2 (click X → handle_dialog(accept))
4. Logout, login as user1, check films

**Expected:**
- user1's "Shared Film Name" still exists
- Delete only affected user2's copy

---

## Delete Film Test Results Template

| Category | Test | Status |
|----------|------|--------|
| Delete UI | 9.1 Delete Button Visible | |
| Delete UI | 9.2 Confirmation Dialog | |
| Delete Flow | 9.3 Delete Cancelled | |
| Delete HTMX | 9.4 Delete Confirmed | |
| Delete Edge | 9.5 Delete Last Film | |
| Delete HTMX | 9.6 Delete Multiple Films | |
| Delete Security | 9.7 User Isolation | |

**Delete Tests: ___ / 7 tests**

---

## 10. Film Search Feature Tests

Tests for the dynamic film search functionality (Phase 5).

**Note:** Search uses `hx-trigger="keyup changed delay:500ms"` - wait ~600ms after typing for results to appear.

### 10.1 Search Input Visible
**Action:**
1. Login and navigate to `/films`
2. Take snapshot

**Expected:**
- Search input field visible with placeholder "Search films..."
- Search input appears alongside the film list (two-column layout)

### 10.2 Search Returns Results
**Action:**
1. Navigate to `/films` (logged in)
2. Fill search input with "god" (partial match for "The Godfather")
3. Wait 600ms for debounce
4. Take snapshot

**Expected:**
- Search results appear in `#search-results` div
- "The Godfather" appears in results (if not already in user's list)
- Each result has an "add" button

### 10.3 Search No Results
**Action:**
1. Fill search input with "xyznonexistent"
2. Wait 600ms
3. Take snapshot

**Expected:**
- Message "No films found matching..." displayed
- No film items in results

### 10.4 Search Excludes User Films
**Action:**
1. Add "The Godfather" to user's film list manually
2. Search for "god"
3. Wait 600ms
4. Take snapshot

**Expected:**
- "The Godfather" does NOT appear in search results (already in user's list)

### 10.5 Add Film from Search Results
**Action:**
1. Search for "matrix" (assuming "The Matrix" exists in catalog)
2. Wait for results
3. Click "add" button on "The Matrix"
4. Take snapshot

**Expected:**
- "The Matrix" appears in user's film list (left column)
- Search results update (The Matrix no longer shown if same search)

### 10.6 Search Case Insensitive
**Action:**
1. Search for "PULP" (uppercase)
2. Wait 600ms
3. Take snapshot

**Expected:**
- "Pulp Fiction" appears in results (case-insensitive match)

### 10.7 Search Debounce Works
**Action:**
1. Type "the" quickly in search input
2. Immediately check network requests (within 500ms)

**Expected:**
- No immediate request sent
- Request only sent after ~500ms pause in typing

### 10.8 Empty Search Shows No Results
**Action:**
1. Clear search input (empty)
2. Wait 600ms
3. Take snapshot

**Expected:**
- No results displayed
- No error message (empty search is valid)

---

## Search Film Test Results Template

| Category | Test | Status |
|----------|------|--------|
| Search UI | 10.1 Search Input Visible | |
| Search HTMX | 10.2 Search Returns Results | |
| Search Edge | 10.3 Search No Results | |
| Search Logic | 10.4 Excludes User Films | |
| Search HTMX | 10.5 Add from Search | |
| Search Logic | 10.6 Case Insensitive | |
| Search HTMX | 10.7 Debounce Works | |
| Search Edge | 10.8 Empty Search | |

**Search Tests: ___ / 8 tests**

---

## 11. Sortable Drag-and-Drop Feature Tests

Tests for the drag-and-drop film reordering functionality (Phase 6.1).

**Note:** Drag-and-drop uses Sortable.js with `hx-trigger="end"`. Use the `drag` tool to simulate drag operations.

### 11.1 Drag Handle Visible
**Action:**
1. Login and navigate to `/films`
2. Add at least one film if none exist
3. Take snapshot

**Expected:**
- Each film has a drag handle icon (menu/hamburger icon) on the left
- Drag handle appears before the film name

### 11.2 Films Display in Order
**Action:**
1. Navigate to `/films` (logged in)
2. Add films in this order: "Film A", "Film B", "Film C"
3. Refresh page
4. Take snapshot

**Expected:**
- Films appear in the order they were added
- Order persists after page refresh

### 11.3 Drag Film to New Position
**Action:**
1. Ensure you have 3+ films: "Film A", "Film B", "Film C"
2. Use `drag` tool to drag "Film C" to the top (before "Film A")
3. Wait for HTMX response
4. Take snapshot

**Expected:**
- Films now display as: "Film C", "Film A", "Film B"
- No page reload occurred
- List updates via HTMX swap

### 11.4 Reorder Persists After Refresh
**Action:**
1. After reordering (test 11.3), reload the page
2. Take snapshot

**Expected:**
- New order persists: "Film C", "Film A", "Film B"
- Order was saved to database

### 11.5 Reorder Persists After Logout/Login
**Action:**
1. After reordering, logout
2. Login again
3. Navigate to `/films`
4. Take snapshot

**Expected:**
- Film order remains as last set
- Order survives session changes

### 11.6 New Film Added at End
**Action:**
1. With existing films in custom order
2. Add a new film "Film D" via the add form
3. Take snapshot

**Expected:**
- "Film D" appears at the bottom of the list
- Existing order is preserved
- New film gets highest order value

### 11.7 Delete Maintains Order
**Action:**
1. With films in order: "Film A", "Film B", "Film C"
2. Delete "Film B" (middle film)
3. Take snapshot

**Expected:**
- Films remain as: "Film A", "Film C"
- Order is maintained (no gaps visible to user)

### 11.8 Drag Multiple Times
**Action:**
1. Start with: "Film A", "Film B", "Film C", "Film D"
2. Drag "Film D" to position 2 (after A, before B)
3. Wait for response
4. Drag "Film A" to position 4 (end)
5. Wait for response
6. Take snapshot

**Expected:**
- Final order: "Film D", "Film B", "Film C", "Film A"
- Each drag operation saves correctly

### 11.9 Sortable Reinitializes After HTMX Swap
**Action:**
1. Reorder films via drag
2. Wait for HTMX swap
3. Attempt another drag operation
4. Take snapshot

**Expected:**
- Second drag works correctly
- Sortable.js reinitializes after HTMX swaps content

### 11.10 Order Is User-Specific
**Action:**
1. Login as user1, add "Film X", "Film Y", "Film Z"
2. Reorder to: "Film Z", "Film X", "Film Y"
3. Logout
4. Login as user2, add same films: "Film X", "Film Y", "Film Z"
5. Take snapshot for user2

**Expected:**
- user2's films are in default order (order added)
- user1's custom order doesn't affect user2

---

## Sortable Drag-and-Drop Test Results Template

| Category | Test | Status |
|----------|------|--------|
| Sortable UI | 11.1 Drag Handle Visible | |
| Sortable UI | 11.2 Films Display in Order | |
| Sortable HTMX | 11.3 Drag to New Position | |
| Sortable Data | 11.4 Order Persists Refresh | |
| Sortable Data | 11.5 Order Persists Login | |
| Sortable Logic | 11.6 New Film at End | |
| Sortable Logic | 11.7 Delete Maintains Order | |
| Sortable HTMX | 11.8 Drag Multiple Times | |
| Sortable HTMX | 11.9 Reinitializes After Swap | |
| Sortable Security | 11.10 User-Specific Order | |

**Sortable Tests: ___ / 10 tests**

---

## 12. Film Detail and Photo Upload Tests

Tests for the film detail view and photo upload functionality (Phase 6.2).

**Note:** Photo upload uses `hx-encoding="multipart/form-data"`. Use `upload_file` tool for file uploads.

### 12.1 Film Name is Clickable
**Action:**
1. Login and navigate to `/films`
2. Add a film if none exist
3. Take snapshot

**Expected:**
- Film names are displayed as clickable links
- Format shows order number: "#1 Film Name"

### 12.2 Click Film Opens Detail View
**Action:**
1. Click on a film name link
2. Wait for HTMX response
3. Take snapshot

**Expected:**
- Detail view loads in `#film-list` area
- Shows film name as heading
- Shows "This film is #X in {username}'s list"
- Shows "Your List" button
- Shows "No photo :(" message (if no photo)
- Shows file upload form

### 12.3 Your List Button Returns to List
**Action:**
1. From detail view, click "Your List" button
2. Wait for response
3. Take snapshot

**Expected:**
- Returns to film list view
- All films displayed with drag handles
- Search panel visible on right

### 12.4 Photo Upload Form Visible
**Action:**
1. Click on a film to open detail view
2. Take snapshot

**Expected:**
- File input field visible
- "Upload File" button visible
- Form has `hx-encoding="multipart/form-data"` attribute

### 12.5 Upload Photo Success
**Action:**
1. Open detail view for a film
2. Use `upload_file` tool to upload an image
3. Click "Upload File" button
4. Wait for response
5. Take snapshot

**Expected:**
- Photo appears in detail view
- Image displayed with max dimensions (200x200)
- "No photo :(" message no longer shown

### 12.6 Photo Persists After Return to List
**Action:**
1. After uploading photo, click "Your List"
2. Click same film to return to detail
3. Take snapshot

**Expected:**
- Photo still displayed
- Photo was saved to database

### 12.7 Flash Message on Add Film
**Action:**
1. Return to film list view
2. Add a new film via the add form
3. Immediately take snapshot

**Expected:**
- Flash message appears: "Added {filmname} to list of films"
- Message has success styling (green alert)

### 12.8 Flash Message Auto-Clears
**Action:**
1. After adding film, wait 4 seconds
2. Take snapshot

**Expected:**
- Flash message has disappeared
- Auto-cleared via `hx-trigger="load delay:3s"`

### 12.9 Detail View Shows Correct Order
**Action:**
1. Add multiple films and reorder them
2. Click on a film in the middle of the list
3. Take snapshot

**Expected:**
- Detail shows correct order number matching list position
- "This film is #X" matches the film's position

### 12.10 Different Films Have Separate Photos
**Action:**
1. Upload photo to Film A
2. Click "Your List" to return
3. Click on Film B (different film)
4. Take snapshot

**Expected:**
- Film B shows "No photo :(" (not Film A's photo)
- Photos are film-specific, not shared

---

## Film Detail Test Results Template

| Category | Test | Status |
|----------|------|--------|
| Detail UI | 12.1 Film Name Clickable | |
| Detail HTMX | 12.2 Click Opens Detail | |
| Detail HTMX | 12.3 Your List Returns | |
| Upload UI | 12.4 Upload Form Visible | |
| Upload HTMX | 12.5 Upload Photo Success | |
| Upload Data | 12.6 Photo Persists | |
| Message UI | 12.7 Flash Message Shows | |
| Message HTMX | 12.8 Message Auto-Clears | |
| Detail Logic | 12.9 Correct Order Shown | |
| Upload Logic | 12.10 Separate Photos | |

**Film Detail Tests: ___ / 10 tests**
