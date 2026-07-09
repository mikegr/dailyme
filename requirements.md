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
13. Add an **edit button** to each item in the journal list.
14. **No commit-message prompt**: pressing Save commits automatically with a generated message, instead of requiring the user to enter one first.
15. Add a **Settings screen**, accessible from the Start screen, that lets the user change the auto-commit message (used in place of the fixed default from #14).
16. Tapping the rendered file content switches to edit mode, same as tapping the edit icon.
17. Swap the order of the save and view (preview) icons, and add a confirmation dialog before switching to view mode while editing, since that discards unsaved changes.
18. Apply the design system defined in **DESIGN.md** ("Cyanic Studio" — dark, turquoise/cyan glassmorphism, Montserrat + Inter) across the app.
19. Toolbar titles should be tinted in the **primary** color, not the default on-surface color.
20. Add an option on the **Settings** screen to switch between **light and dark mode**.
21. Serve reads from the **offline cache** whenever a cached copy exists (cache-first, not just an offline fallback), and on startup check the branch's latest commit — clearing the entire cache if it has moved on since last time.
22. Support **hyperlinks** in rendered markdown: `#tag` and `[[wiki link]]` syntax. Make them clickable, opening the matching file (by name) from the `journals` or `pages` folder.
23. Disambiguate `#` usages: a `#tag` link has no space between the `#` and the text, while a markdown **header** requires a space after the `#`(s).
24. The hardware back button should behave like the toolbar back arrow, dismissing the current screen rather than closing the app.
25. Integrate **Compose Multiplatform Navigation 3** to manage the app's backstack, replacing the custom `Screen`/`AppState` backstack switching.
26. Add offline support for commits: queue every commit instead of calling the API directly, process the queue asynchronously with a retry back-off strategy on failure, persist queued changes so they retry after the next app start, and add a log screen showing the last calls and the state of the queue.
27. Add a **+ button** in the Journal screen's toolbar that creates a new file named after the current day in the `journals` directory and opens it in edit mode.
