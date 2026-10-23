Files first, then the prompt.

**Upload to the other Claude** (the import block at the top of `CaseViewTable.tsx` tells you exactly where the first three live):

- Must: `src/app/components/Views/CaseView/CaseViewTable.tsx`; the file(s) defining `SortRule`, `SortObject`, `CaseViewDataType`, `LayoutDialogField`; the file defining `multisortTable`; `ViewController` (the API client with `getCaseViewDataViewsWithRole`, `getCaseViewDataViews`, `getCaseViewDataPaged`).
- Should: `caseViewTableData.json` (sample rows — shows the real formats of dates, TIN, indicators, priority alpha); the CaseView stylesheet/CSS module (header highlight classes); `package.json` (PrimeReact version matters for multi-sort props); any separate sort dropdown/dialog component and the constants file if `TRUNCATION_LIMIT` / `LAZY_THRESHOLD` / `CHUNK` / `MAX_CHUNKS` live outside the component.
- Not needed: the Redux slices, `National.tsx`, `EntityHeader.tsx`, `eComboStaff.tsx`.

Plus your MD exports. Edit the two bracketed lines in the prompt to match what you attach.

```
# Task: Replicate legacy ENTITY sort behaviour in the modern Case View grid (JIRA EN-1772 — Case View Sorting)

You are working on `entity-ui` (Next.js with `output: export`, React + TypeScript, PrimeReact DataTable, Redux). The Case View grid is `src/app/components/Views/CaseView/CaseViewTable.tsx`. Legacy ENTITY let a user pick sort fields with a checkbox (enabled/disabled) and a sequence number (1 = primary, 2 = secondary, 3 = tertiary). The modern grid must reproduce that behaviour for every view without regressing anything else in the component.

## Inputs

1. **Field mapping (MD)** exported from the "SP 443 Views Testing" workbook, sheet "Inventory-Case Fields". Columns: legacy field name, new/changed name, Notes, SP 444 (column title), SP 443 (data display), **SP 445 Sort order** (✓ / X), and **Test Notes**, which contain a `Sort:` line per field describing how legacy sorts it. Treat ✓ in SP 445 as "sortable in modern Case View" and X as "not sortable"; state this assumption at the top of your output. [Other MD files attached: ___]
2. **Code**: `CaseViewTable.tsx`; the types file(s) defining `SortRule`, `SortObject`, `CaseViewDataType`, `LayoutDialogField`; the file containing `multisortTable`; `ViewController`; `caseViewTableData.json` (sample rows); the CaseView stylesheet; `package.json`. [Edit to match what is actually attached: ___]

## What the component does today (verified — do not re-derive)

- Views are selected by `optionsValue`: 0 open, 1 closed, 3 queue, 4 open+closed, 5 open+queue, and a shared branch for 2/6/7/…; plus an `openFromDisplay && changeLayoutToPresetInNewWindow` branch that builds `columns` from `layoutDialogSelectedFields`. Each branch sets `columns`, `hiddenColumns`, `defaultSortColumns: SortRule<CaseViewDataType>[]` (`{ key, direction }`) and `defaultSortColumnNames: string[]`.
- Current defaults: 0 → caseAssignedRODate desc; 1 → caseClosedDate desc; 2/6/7 → caseAssignedRODate desc; 3 → priorityAlpha asc; 4 → tin desc, caseStatus desc, caseClosedDate desc; 5 → priority asc, tin desc; openFromDisplay → caseAssignedRODate desc.
- `sortStrings: SortObject[]` (`{ field, dir }`) is a 20-field allowlist, all `asc`. `viewFieldMaster` is the master list of `{ field, header }`.
- State: `sortColumnsObject` (ordered `SortRule[]`, the user's active sort), `sortColumnSelectedClassValues` (header highlight), `sortedColumn` ("default"), `tablekey` (forced re-render). A `useEffect` keyed on `defaultSortColumnNames.join("|")` resets these via a `__dummy__` SortRule hack.
- `fetchData` loads the full result set through `ViewController.getCaseViewDataViewsWithRole` / `getCaseViewDataViews`, then `multisortTable(response.data, sortColumnsObject.length ? sortColumnsObject : defaultSortColumns)`, adds `__index` (1-based, used for keyboard navigation), and calls `setTableData`. Display is truncated at `TRUNCATION_LIMIT`.
- Above `LAZY_THRESHOLD` rows, lazy mode fetches chunks with `ViewController.getCaseViewDataPaged(org, optionsValue, elevel, dataScope, offset, CHUNK)` into `chunksRef` with LRU eviction (`lruRef`, `MAX_CHUNKS`), rebuilt into `lazyRows` by index. **No sort specification is passed to this call.** The user's sort cannot be honoured in lazy mode without a backend change, and sorting chunks client-side would sort each page independently — a known defect pattern in this program that must not be reintroduced.
- Type hints already exist: `dateStrings`, `currencyStrings`, `tinStrings`, `booleanStrings`.

## What to build

1. **Sortable-field registry** derived from the MD (✓ rows only). Per entry: legacy name, `viewFieldMaster.field` key, data type (date / currency / number / tin / indicator / code / text / priorityAlpha), legacy default direction and any special ordering from the Test Notes `Sort:` line, with the MD row cited. Map only to keys that exist in `viewFieldMaster`; a ✓ field with no key in the code is a listed gap — never invent one. Replace `sortStrings` with this registry (keep a shim if `sortStrings` is referenced elsewhere).
2. **Legacy sort configuration**: checkbox enables a field, a sequence number orders it, each field has a direction. It must produce the ordered `SortRule[]` that `multisortTable` already consumes and keep `sortColumnSelectedClassValues` / `defaultSortColumnNames` consistent. Per-view defaults stay exactly as listed above unless the MD shows legacy differs — then show both and recommend. Provide a "reset to default sort" path. Non-sortable columns must not sort on header click.
3. **Comparators** (in or beside `multisortTable`): stable; type-aware; null/blank last regardless of direction unless the Test Notes say otherwise; dates parsed (confirm the format from the sample JSON), currency and counts numeric, TIN on digits only, indicators with an explicitly stated order, **Priority Alpha parsed as (numeric band, letter) — never a plain string compare, since `101b` sorts above `99a` lexically**; take the meaning of asc/desc from the Test Notes and flag it if they are silent. Add a final deterministic tiebreaker (tin asc) and say so.
4. **Header indicators**: rank number plus direction arrow on every sorted column, accessible to keyboard and assistive technology.
5. **Re-sort without refetch** when the sort changes in non-lazy mode; renumber `__index` after sorting.
6. **Lazy mode**: pass the ordered sort spec to `getCaseViewDataPaged` (propose the exact parameter shape and the backend contract: sort before paging); clear `chunksRef`/`lruRef` and reload from chunk 0 whenever the sort changes. Mark the backend change as a dependency. If the backend cannot change this release, implement the fallback you recommend (e.g. user sort disabled in lazy mode with a visible message) and explain the trade-off.

## Constraints

- Minimal, surgical diffs. Do not reorder or edit any `columns` / `hiddenColumns` arrays. Do not remove the lazy path, the `openFromDisplay` branch, keyboard navigation, or `__index`.
- No new npm dependencies. Respect existing ESLint disables and TypeScript strictness. Nothing server-only (static export).
- Where the MD and the code disagree, say so explicitly; do not silently choose.
- If something blocks a correct answer, ask up to 5 questions — but still deliver the analysis document.

## Output — produce `case-view-sorting-solution.md` with exactly these sections

1. **Summary** (≤10 lines), including the ✓/X assumption.
2. **Sortable field registry** — table: legacy name | field key | type | default dir | sort rule (MD row) | views where visible.
3. **Per-view analysis** — one subsection per view (optionsValue 0, 1, 2/6/7, 3, 4, 5, openFromDisplay): current default sort, legacy expectation, sortable columns in that view, problems found, fix.
4. **Changes by file** — for every hunk: file, exact line range in the file as uploaded (before any edits), 2 lines of anchor context, current code, replacement code, reason. Number the hunks so later ones can reference line shifts from earlier ones.
5. **Lazy mode / backend dependency** — proposed API contract and client changes.
6. **Test plan** — manual checks per view plus unit cases for the comparators (priority alpha order, nulls, dates, ties, stability).
7. **Open questions**.

Then, if the TS/TSX files were uploaded: apply the changes and return each modified file complete, not as snippets. Self-check before returning: every identifier resolves, all existing exports and props are unchanged, no behaviour removed. If you have a shell, run `npx tsc --noEmit` and `npx eslint <changed files>` and report the results; if not, say which checks you could not run.
```
