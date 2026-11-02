This prompt implements the Case View default sorts only. All changes go to the new folder, and your existing code is never modified. Claude will stop and show you a plan before it writes any files.

```text
Implement the Case View default sorts. Work only on Case View in this session; the other views come in later sessions.

# Inputs
- Existing code (read-only): <EXISTING_CODE_PATH>
- Spec: <SPEC_PATH>/case-view-layout-spec.md
- Output folder: <OUTPUT_PARENT_PATH>/issue-1766-7-8-9-V2
- Issues: 1766, 1767, 1768, 1769

# Scope
Implement the default SORT for each Case View status/layout. Do NOT change column sets or column order in this session. List any column gaps from the spec under "Deferred" in CHANGES.md.

# Hard rules
1. Do NOT modify anything under <EXISTING_CODE_PATH>. Every change goes into the output folder.
2. Do not commit, push or deploy. No test deployment.
3. Prefer a frontend-only change. Touch the backend only when the sort cannot be correct on the frontend alone. For example, if the grid uses server-side paging or sorting, a frontend-only sort would reorder only the current page. If you change the backend, explain why in CHANGES.md.
4. Change only the default behavior:
   - Users must still be able to re-sort by clicking a column header.
   - Don't break user-saved custom views. If saved views or preferences store the old default sort, flag it instead of overwriting it.
5. Make the smallest safe change set, and match the existing code style and patterns.

# Step 1 — Recover context
- Check your memory and any CLAUDE.md files for previous work on these views (issues 1766–1769, item 443, view layouts, default sorts). Treat what you find as hints; verify it against the current code.
- Read the spec fully. Its file:line references may be stale, so re-locate them.
- Search git history in the existing code for the last version where the Case View sorts were correct (Open defaulted to Date – Assigned RO). Reuse that logic where sensible.

# Step 2 — Required default sorts
If the spec differs from this table, the spec wins; flag the difference.

| Status / layout        | Default sort |
|------------------------|--------------|
| Open (master default)  | Date – Assigned RO (Case) DESC |
| Closed                 | Date – Closed (Case) DESC |
| Queue                  | Priority Alpha (direction per spec/legacy) |
| Open + Closed          | TIN, then case status (Open first); closed rows by Date – Closed (Case) DESC |
| Open + Queue           | Priority (confirm: Priority Alpha), then TIN |
| Return to Queue        | As defined in the spec; if it is TBD, leave the current sort unchanged and flag it |

Behavior:
- Apply the default sort on initial load, when the status filter changes, and when the layout changes.
- Sort dates as dates, not strings (watch for MM/DD/YYYY strings).
- Put empty dates last in DESC sorts, unless legacy behavior differs.
- Priority Alpha must sort in the same order as legacy. Check the expected order.
- Ties: keep the existing tie-breaker. If there is none, list it as an open question.

# Step 3 — Plan, then STOP for approval
Before writing any files, show me:
- the files you will change (frontend/backend) and why;
- the current vs. new default sort for each status/layout;
- anything in the spec you could not find, or that conflicts with the code.
Wait for my OK.

# Step 4 — Implement in the output folder
- For every file you change or add, copy it into the output folder at the same relative path, keeping frontend and backend in separate subfolders:
    issue-1766-7-8-9-V2/frontend/<relative path>
    issue-1766-7-8-9-V2/backend/<relative path>
  Edit only the copies.
- Also write to the output folder:
  - case-view-default-sort.patch: a unified diff against <EXISTING_CODE_PATH> that applies cleanly with git apply;
  - CHANGES.md (contents below).

# Step 5 — Verify without touching the original
1. Create a temporary copy to test in: a git worktree if the existing code is a git repo, otherwise a temporary folder copy.
2. Apply the patch there.
3. Run the build, lint, and any existing tests for the changed areas.
4. If the project has tests for this area, add or update unit tests covering the default sort for each status. Put them in the output folder too.
5. Remove the temporary worktree or copy, and report the results.

# CHANGES.md contents
1. Summary: issues 1766–1769, Case View default sorts.
2. Files changed: path, frontend or backend, and a one-line reason.
3. Before vs. after default sort for each status/layout.
4. Frontend vs. backend: whether backend changes were needed, and why.
5. How to apply: copy the files, or run git apply with the patch.
6. Dev test checklist:
   - each status/layout loads with the correct default sort;
   - switching status re-applies the default sort;
   - switching layout re-applies the default sort;
   - clicking a column header still re-sorts;
   - date columns sort chronologically, with empty dates placed correctly.
7. Build and test results.
8. Deferred: column gaps and other spec items outside this scope.
9. Open questions.

# Finish
Save a short note to your memory (not the codebase) covering what you changed, where, and the approach, so the Module View session can reuse it. Then reply with a short summary, the output folder path, and the open questions.
```

Before running it, replace the three placeholders: `<EXISTING_CODE_PATH>`, `<SPEC_PATH>`, and `<OUTPUT_PARENT_PATH>`.

The scope is sorting only, as you asked. If you also want the Case View column layouts fixed in the same pass, change the Scope section to "default sort and default columns".
