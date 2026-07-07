# DailyMe — Requirements

Running log of requirements as requested by the user, in the order they were given.

1. Build a Kotlin Multiplatform app using Compose Multiplatform, targeting **Android** and **Wasm** (web).
2. The app connects to a **GitHub repository** and shows markdown files in it.
3. The app is later able to **edit** those markdown files (implemented from the first version, not deferred).
4. Project name: **DailyMe**.
5. GitHub authentication via a **personal access token** entered by the user.
6. First working version scope: browse the repo, render markdown, edit, and commit changes back to GitHub — all included from day one.
7. **Store the credentials and details** (token, repo owner, repo name, branch) once the user has logged in, so the session persists across app restarts without re-entering them.
8. Add a **Start screen** that shows:
   - a link/button to open the file browser,
   - a **logout** button if the user is already logged in,
   - a **login** button if the user is not logged in.
9. **Ask for confirmation** before logging out (confirmation dialog on the logout action).
10. Add an option in the file browser to **reverse the sort order** of the file/folder listing.
11. Add **offline capabilities**:
    - View and edit files while offline (not just read-only).
    - Automatically cache every folder/file the user has visited, so it's available offline with no manual step.
    - Show an explicit online/offline status indicator in the UI.
    - Edits made offline are queued and synced automatically once back online.
12. Add a **Journal** screen: each file in the `journals` folder is shown as a list item entry in a paged list, with each entry showing its rendered markdown content.
