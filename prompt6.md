I'll check your past chats for the sorting discussion first so the prompt can point to the right details.

I couldn't find the sorting discussion or the spreadsheet in your past chats here, so it was probably in Claude Code on your other computer. I did find the memory setup we built on Oct 29 for `entity-service` and `entity-ui` (the `CLAUDE.md` files plus `docs/claude-memory/`), so this prompt rebuilds context from that memory and the spreadsheet.

**Before you paste it:** copy the spreadsheet into the project folder, replace `<ISSUE-NUMBER>` and `<PATH-TO-SPREADSHEET>`, and start `claude --model fable` in the folder that holds both `entity-service` and `entity-ui`.

```
I'm working on issue <ISSUE-NUMBER> in the Entity application (entity-service = backend, entity-ui = frontend). It adds sorting to the views and fixes dates in the CSV export. We will implement it ONE VIEW AT A TIME. Don't write or change any code until I approve the plan for a view.

FIRST: save these instructions verbatim to ./issue-<ISSUE-NUMBER>/RULES.md so later sessions can reuse them. That folder is the only place you may write.

STEP 1 - RECOVER CONTEXT (read only)
1. Read ./CLAUDE.md, entity-service/CLAUDE.md and entity-ui/CLAUDE.md, then the docs/claude-memory/ files they point to (backend-reference.md, frontend-reference.md and api-contract.md first). Use the memory instead of re-exploring the code; open source files only to confirm specifics. If the commit hash in the memory headers differs from HEAD, list the changed files that matter for this issue.
2. Read the requirements spreadsheet at <PATH-TO-SPREADSHEET>. It lists the fields to add for sorting and the backend and frontend changes needed. If you can't read it with the tools on this machine, stop and ask me to save it as CSV. Don't install anything.
3. Search the code for existing sorting and CSV export (sort, sortBy, orderBy, ORDER BY, Pageable, csv, export, Blob, text/csv) so you know what's already there.

STEP 2 - CONFIRM REQUIREMENTS (stop and wait)
Restate the requirements as tables, citing the spreadsheet sheet and row for each item:
- A. Sorting: each field to make sortable, per view, plus the backend and frontend changes the spreadsheet calls for.
- B. CSV export: any date equal to 1/1/1900 is written as an empty cell (not the text "null") in the CSV the UI produces. Make the change where the CSV is actually built. This changes the CSV output only; database values and what the grid shows on screen stay as they are.
- Both apply to every view.
Then list anything missing, ambiguous or conflicting as numbered questions, and wait for my answers.

STEP 3 - VIEW INVENTORY, THEN ONE VIEW'S PLAN (stop and wait)
Make a table of every view: view | UI component file(s) | backend endpoint(s) | query/repository method | sorting today (none, client-side, server-side) | where the CSV is built (file:function, UI or backend) | how dates arrive in the UI (type and format).
Save it to ./issue-<ISSUE-NUMBER>/PROGRESS.md and ask me which view to start with.
For the view I pick, list every file you'll change, what changes in each and why, and anything you need from me. Wait for my OK.

STEP 4 - IMPLEMENT THE APPROVED VIEW
How to deliver changes:
- Never edit an existing file. For each file that needs changes, write a complete copy with the changes applied and the issue number added before the extension: OrderGrid.jsx -> OrderGrid_<ISSUE-NUMBER>.jsx.
- Put the copies in ./issue-<ISSUE-NUMBER>/<view>/, mirroring the original path (for example ./issue-<ISSUE-NUMBER>/<view>/entity-ui/src/.../OrderGrid_<ISSUE-NUMBER>.jsx), so the build, tests and Java class names are not affected.
- Every copy is the whole file, ready to paste over the original. No "..." or "rest unchanged".
- Write ./issue-<ISSUE-NUMBER>/<view>/CHANGES.md: for each file, the original path, a one-line summary, and a unified diff against the original (diff -u).
- New SQL (index, view or query changes) goes in <purpose>_<ISSUE-NUMBER>.sql in the same folder. Do not run it.

Don't break existing functionality:
- Change only what this issue needs. No refactoring, renaming, reformatting, import reordering, dependency or version changes, or unrelated fixes. Note bugs you spot in CHANGES.md instead of fixing them.
- Keep everything backward compatible. New request parameters are optional; without them the endpoint returns exactly what it returns today (same default order, paging and response shape). Other views and other callers of a changed endpoint must behave as before.
- Never put raw user input into ORDER BY. Map each allowed sort field to its column through a whitelist, fall back to today's default order for unknown values, and add a unique tie-breaker (such as the primary key) so paging stays stable.
- If the grid is paged on the server, sort on the server before paging. Don't sort only the current page in the browser.
- If a change belongs in a shared file (a shared grid, CSV utility, base repository, common DTO or mapper), STOP and ask me first, because changing it changes every view at once. Give me a view-only option and a shared option with the trade-offs.

Requirement B details:
- Match 1/1/1900 by calendar date, with or without a time part, in every form the value arrives in (Date object, ISO string, "01/01/1900", epoch number).
- Watch for timezone conversion: in a US browser, 1900-01-01 UTC becomes Dec 31, 1899 locally, so a naive check misses it. Make sure the check sees the value as stored.
- Leave the CSV formatting of every other date exactly as it is today.
- Tell me where the 1900 values come from (real data or a default, such as SQL Server storing an empty string as 1900-01-01), but don't change that.
- Tell me how 1900 dates will sort under the new sorting and ask whether they should sort as empty. Don't decide that yourself.

STEP 5 - TEST
- Database: never connect to or run anything against a database. Write the queries I need into ./issue-<ISSUE-NUMBER>/<view>/test-queries_<ISSUE-NUMBER>.sql, SELECT statements only. Tell me which database and schema to run them in, what each one checks and what result to expect, then wait for me to paste the results. Include queries that find rows in this view with 1/1/1900 dates, and the new ORDER BY for each sortable field with the view's filters so I can compare against the UI. If test data needs to be created, describe it and let me decide; never write INSERT, UPDATE, DELETE or DDL for testing.
- Build: after I tell you I've copied the files in, you may run the existing build and unit tests (nothing that touches a database). Ask before running anything else.
- Give me a manual test checklist for this view: each sortable field ascending and descending, across pages and with filters; the default load with no sort chosen (must match today); CSV export with 1900 dates, other dates and empty dates; and a quick check that one other view is unchanged.

STEP 6 - CLOSE OUT THE VIEW (stop)
Update PROGRESS.md with views done, views left, decisions I made and open questions. Give me a short list of the files to copy and where each one goes. Then stop. Don't start the next view until I name it.

GENERAL
- If you're unsure, ask me. Don't guess. Every claim about the code cites file:line.
- Keep replies short; the details go in the files.
```

**For each next view**, start a fresh session (it keeps Fable's context small) and paste:

```
Continue issue <ISSUE-NUMBER>. Read ./issue-<ISSUE-NUMBER>/RULES.md, ./issue-<ISSUE-NUMBER>/PROGRESS.md and the CLAUDE.md memory files. Then do Steps 3 (plan for this view only), 4, 5 and 6 for the view: <VIEW-NAME>. Follow the patterns and decisions from the views already done (see their CHANGES.md).
```

The prompt keeps the renamed copies in an `issue-<ISSUE-NUMBER>/` folder instead of next to the originals on purpose. In Java, a file named `OrderService_1234.java` that contains `public class OrderService` won't compile. A second copy of a React component can also get picked up by tests or lint. You still get the issue-numbered files; you just paste each one's contents over its original.
