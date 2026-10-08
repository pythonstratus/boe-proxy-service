# EN-1742 ELEVEL 7 AGM Access: Master Notes

Oct 8, 2026 · @Santosh Srinivasaiah

## Summary

An Acting Group Manager (ELEVEL 7) logging into ENTITY in UAT gets employee-level access only: no Group option, a staff ROID in the header, and a blank "Viewing as." This blocks testing of 8 of the 9 acceptance scenarios, all of which involve an AGM or Acting GM role.

Three causes combine. The tester's active assignment is a staff ROID (859062xx). ELEVEL 7 has no Group/Self mode in the code. The Change Access list treats every ENTEMP row, including the group row, as an employee.

- **Ticket:** EN-1742, Bug, High, In Progress, ENTITY UAT Sprint 4
- **Epic:** EA - Login, Change Level & role (Change Access)
- **Labels:** SP460, rbacuat
- **Tester profile:** Area 22, Group 22061700, personal ROID 22061787, header showing 85906221

## Observed vs expected

Every symptom traces to the app treating the AGM as an employee, not a group-level user. Expected behavior comes from the acceptance criteria table attached to EN-1742.

| # | Area | Observed in UAT | Expected |
| --- | --- | --- | --- |
| 1 | Header | "ACTING MANAGER 85906221": a staff ROID, not in Group 220617 | Shows the assignment for the group the user is acting for |
| 2 | Viewing as | Blank ("-") | Shows the current role and ROID, e.g. Employee 22061787 or Acting GM 22061700 |
| 3 | Change Access | Only Revenue Officer tiles. Group 22061700 "GROUP DESIGNATION" and hold files show as RO tiles | Employee and Group (Acting GM) role options, based on the user's profile |
| 4 | Group functions | No group-level options available (specific functions not yet listed) | Choosing Acting GM gives group data plus Case Assignment and Time Verification |
| 5 | Test data | ROID 22061787 appears twice: the tester and "R COLLINS AGM" | One person per ROID, or a documented reason for sharing it |

Only the "RO with normal RO assignment" scenario can be fully tested today.

## Root cause analysis

The wrong assignment is picked at login, ELEVEL 7 has no Group/Self mode, and Change Access lists the group as an employee. Code references come from the RBAC module built in the project chat [RBAC implementation for Java/React application](https://claude.ai/chat/ccc926e6-9d6f-4f0c-9112-00df77c91e95) (March 2026). Confirm the names against the current repo before changing anything.

&#91;embedded content: cause-to-symptom map · 4 causes, 5 symptoms\]

Building the Group/Self mode (RC2) clears three symptoms. Fixing the active assignment (RC1) and the Change Access list (RC3) clears the rest. The dashed link is a possible cause still to confirm.

### RC1: The active assignment is a staff ROID

- Header, ELEVEL, data scope and menus all read the current assignment from `getAssignmentForUser(seid)`.
- That method takes the row with EACTIVE='A' AND PRIMARY\_ROID='Y' (`findCurrentActiveAssignment`), then falls back to `findAssignmentsWithPriority`.
- The header shows 85906221. The code treats any ROID starting with 859062 as Staff (`isStaffAssignment()`, `findStaffAssignments`).
- So the tester's active/primary ENTEMP row is a staff assignment, not the Group 220617 assignment.
- Staff status turns on `canRealign`, which grants Realignment and Utilities regardless of ELEVEL.
- The code has two staff checks that can disagree. Menu permissions check only the current assignment; the assignment list and `isStaffUser(seid)` check any assignment.
- `getELevel()` takes `assignments.get(0)`. Rows that tie in the ORDER BY CASE come back in no guaranteed order.

### RC2: ELEVEL 7 has no Group/Self mode

- `ELevelService.ELEVEL_DEFINITIONS` defines ELEVEL 7 exactly like ELEVEL 6: same menus, scope "All employees within assigned Group/POD."
- The design says an ELEVEL 7 user picks Group or Self at login, which sets the session's effective assignment.
- That design was logged as an open question (modal at login, or a preference?). No code implementing it was found.
- Result: there is no role selector, the effective assignment is never set, "Viewing as" stays blank, and scope can't switch.

### RC3: Change Access lists every ENTEMP row as an employee

- The list appears to come from the ENTEMP hierarchy helpers (`findByAreacd`, POD lookups), filtered only by ELEVEL > -2.
- The group row (format XXXXXX00, here 22061700) and hold-file ROIDs (22061791, 22061799) render as Revenue Officer tiles.
- A GM or AGM drilling down to employees is expected. What's missing is the Group option beside it.

### Contributing: duplicate ROID 22061787

- Two ENTEMP rows share ROID 22061787 (the tester and "R COLLINS AGM"). Rows are keyed by ROID plus SEID, so ROID alone isn't unique.
- `findByRoid(Long)` expects one row. With two, it throws or returns either one.
- `updateOrg(roid, org)` updates by ROID only, so it changes both people's rows.

## Fix pathway

Fix the test data first, then build the ELEVEL 7 Group/Self mode, then clean up the header, Change Access and ROID lookups. Steps 1 and 2 may make some symptoms disappear, but step 3 is the real fix.

1. **Confirm the tester's assignments**
   - Run `SELECT ROID, NAME, TITLE, ELEVEL, EACTIVE, PRIMARY_ROID, AREACD, PODCD FROM ENTEMP WHERE TRIM(SEID) = '<tester SEID>';`
   - Expect an ELEVEL 7 assignment tied to Group 220617 and the personal RO row 22061787.
   - If ACTING MANAGER sits on the 85906221 row, fix the UAT profile. If the right row exists but isn't active/primary, debug `switchAssignment` and `activateAssignment`.
   - Use a test SEID with no staff (859062xx) row for AGM testing, so staff privileges don't mask results.
   - Done when: after re-login, the header shows a 220617xx assignment.
2. **Make assignment selection deterministic**
   - Add a final tiebreaker to the ORDER BY in `findAssignmentsWithPriority` (for example, by ROID).
   - Decide whether a staff row should ever win over an ELEVEL 7 group row at login, and encode the rule.
   - Use one staff rule everywhere: current assignment only, or any assignment.
   - Done when: the same user and data return the same assignment on every login.
3. **Build the ELEVEL 7 Group/Self mode (core fix)**
   - Add a session field for the selected mode and effective assignment (the design used EFFECTIVE\_ASSIGNMENT in USER\_SESSIONS).
   - Group sets the effective assignment to the group ROID (first six digits + "00", here 22061700). Self sets it to the personal ROID (22061787).
   - Add an endpoint to switch modes that rebuilds the access context from scratch: scope, filters, cached query results, permissions.
   - Scope: Group sees all employees in 220617xx; Self sees only the user's own ROID.
   - Menus: Case Assignment and Time Verification only in Group mode. Self mode gets the ELEVEL 8 menu.
   - Done when: an AGM can switch both ways, and each switch changes header, Viewing as, data and menus immediately.
4. **Fix the header and "Viewing as"**
   - Header shows the role plus the effective assignment.
   - "Viewing as" shows the mode and ROID (Group 22061700 or Self 22061787), or the employee when drilled down.
   - Done when: no logged-in ELEVEL 6 or 7 user ever sees "-".
5. **Update Change Access**
   - For ELEVEL 7, show a role section first: Employee (own ROID) and Acting GM (group ROID).
   - Remove group-level ROIDs (ending in 00) from the Revenue Officer tiles and present them as the Group option.
   - Done when: Change Access for an AGM matches the "Permanent/long-term AGM" scenario.
6. **Harden ROID lookups**
   - Replace `findByRoid(Long)` in user-facing paths with `findByRoidAndSeid`.
   - Scope `updateOrg` to ROID plus SEID.
   - Done when: two people sharing a ROID can't see or change each other's context.

## Testing approach

Test in four layers: unit tests on role resolution, API tests on the session context, the nine UAT scenarios, then negative and regression checks. Set up the test profiles first, since most scenarios need a profile that may not exist yet.

### Test profiles to set up

| Profile | ENTEMP setup | Covers scenarios |
| --- | --- | --- |
| Regular RO | One RO row, ELEVEL 8 | 1 |
| RO temporarily profiled as AGM | Personal ROID that also carries the ELEVEL 7 role | 2, 6, 7, 8 |
| Permanent AGM | ELEVEL 7 row for their own group | 3 |
| AGM acting for another group | ELEVEL 7 row for a group other than their home group | 4 |
| GM with an extra acting group | ELEVEL 6 row plus an ELEVEL 7 row for a second group | 5 |
| AGM in CF org | ELEVEL 7 user with ORG = CF who can switch to AD | 9 |
| AGM who also has a staff row | ELEVEL 7 row plus a 859062xx row | Negative tests |

### Unit tests

- `getAssignmentForUser` returns the same row on every call for a user with several assignments.
- A staff row doesn't win over an ELEVEL 7 row, unless that is the agreed rule.
- Group ROID derivation: 22061787 gives 22061700.
- ELEVEL 7 Group mode includes Case Assignment and Time Verification; Self mode matches ELEVEL 8.
- Every staff check gives the same answer for the same user.
- `findByRoidAndSeid` returns the right person when two rows share a ROID.

### API tests

- After AGM login, the session or user-context response returns ELEVEL 7, the mode, the effective assignment, and isStaff = false.
- Switching Self → Group → Self changes scope and menus in each response, with no leftover filters.
- Inventory, Module, Activity, Time, Reports and Queries return only the user's rows in Self mode and the group's rows in Group mode.

### UAT scenarios

For each run, capture the header, "Viewing as," the menu bar and one data screen.

| # | Scenario | Pass when | Result |
| --- | --- | --- | --- |
| 1 | RO with normal RO assignment | Sees only own employee-level data; no Group option | Not run |
| 2 | RO temporarily profiled as AGM | The same ROID offers both Employee and Acting GM | Not run |
| 3 | Permanent/long-term AGM | Correct Employee and Group options appear for the profile | Not run |
| 4 | AGM acting for another group | Header identifies the acting group; data limited to it | Not run |
| 5 | GM with an additional Acting GM group | Regular GM and Acting GM access are clearly separate, each with its own scope | Not run |
| 6 | AGM → Employee | Inventory, Module, Time, Activity, Reports and Queries restrict to own data immediately | Not run |
| 7 | AGM → Group | Group data, Case Assignment and Time Verification appear immediately | Not run |
| 8 | AGM → Employee → Group | No employee-level filters, permissions or data remain | Not run |
| 9 | AGM/CF → AD role | Prior CF assignment, group, employee, role and data context are cleared; only AD access remains | Not run |

### Negative and security checks

- An AGM with no staff row doesn't see Realignment or Utilities.
- An AGM can't reach another group's data by changing a ROID in the URL or request.
- Change Access shows nothing outside the user's own group.
- After a timeout and re-login, the mode resets to the agreed default.
- Two users sharing ROID 22061787 each see only their own context.

### Regression

- Login, menus and Change Access for ELEVEL 0, 2, 4, 6 and 8 are unchanged.
- Staff users keep Realignment and Utilities.

## Open questions and ticket housekeeping

The ELEVEL 7 login behavior was never decided, so get answers to the first four questions before step 3 starts.

### Decisions needed

- [ ] At login, should an ELEVEL 7 user be prompted for Group or Self, or default to Group with a switch in Change Access?
- [ ] In Group mode, should the header show the group ROID (22061700) or the personal ROID (22061787)?
- [ ] Should a staff assignment ever override an ELEVEL 7 assignment at login?
- [ ] Should staff status come from the current assignment only, or from any assignment?
- [ ] Should hold-file ROIDs (22061791, 22061799) appear in Change Access?

### Information needed from the tester

- [ ] Which group-level functions were tried, and what happened?
- [ ] Is the tester a temporary or permanent AGM, and do they really have a staff assignment?
- [ ] Is sharing ROID 22061787 with "R COLLINS AGM" an intended UAT setup?

### Ticket housekeeping

- [ ] Retitle to "AGM (ELEVEL 7): no Group-level access, wrong Acting Manager ROID, blank Viewing as."
- [ ] Add environment, test user details and steps to reproduce.
- [ ] Fix the garbled arrows in the attached table: "$B"\*(B" should read "→".
- [ ] Change the link: "relates to" the testing story, "caused by" the dev story.
- [ ] Consider a separate story for the ELEVEL 7 role selector, linked to this bug.
