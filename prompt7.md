The prompt now includes the full layouts, sorts and rules for all five views, so Claude can write the specs even if the workbook isn't available. Replace `<PATH_TO_WORKBOOK>` with the real path, or delete that line if the file won't be in the repo.

```text
Produce five implementation spec files in Markdown, one per inventory view. Do NOT change any application code in this session. Only investigate the codebase and write the specs. Follow-up sessions will implement each view from its own spec.

# Output files
| View          | Workbook sheet             | Output file                                     |
|---------------|----------------------------|-------------------------------------------------|
| Case View     | Case View Layout Formats   | docs/view-layouts/case-view-layout-spec.md      |
| Module View   | Module View Layout Formats | docs/view-layouts/module-view-layout-spec.md    |
| Activity View | Activity Layout Formats    | docs/view-layouts/activity-view-layout-spec.md  |
| Time View     | Time Layout Formats        | docs/view-layouts/time-view-layout-spec.md      |
| Employee View | Employee View Formats      | docs/view-layouts/employee-view-layout-spec.md  |

# Source of truth
The requirements below are transcribed from the requirements workbook.

If the workbook is available at <PATH_TO_WORKBOOK>.xlsx:
- Read it with Python (openpyxl), including cell fill colors and font colors.
- Use it to verify the requirements below, and to fill in anything marked "see workbook".
- Where it differs from the requirements below, the workbook wins. Flag every difference.

If the workbook is not available, treat the requirements below as authoritative and list anything missing as an open question.

# Background
A reviewer testing on dev found that the default sorts (and possibly the default column layouts) have regressed from the requirements workbook. Recent work (item 443) added many missing fields to the backend and frontend, and all fields now exist in both. However, the default layouts and sorts were never re-aligned with the workbook's "Layout Formats" sheets.

The reviewer confirmed that the Case view and Activity view sorts are wrong. Verify every view. These sorts were correct for a long time, so a previous working version likely exists in git history.

# Global rules (apply to every view)
1. The "Layout Formats" sheets define the default columns (in order) and the default sort for each status, layout or sub-view. Implement them as written. Do NOT use the example sheets ("Case Layout Example", "MODman Layout Examples").
2. A field in a red/pink-shaded cell (cell fill) is not available upstream yet. Exclude it and list it under "Future fields". Known example: Letter 725-B Date (a date that will come in the future).
3. Colored FONT (red, blue) is revision or emphasis markup, not exclusion. Follow what the text says. Where an old value and a new value are both shown, the new value wins.
4. Selecting a status filter OR selecting a layout must both apply that status's default columns and default sort.
5. Column titles are stacked (wrapped) so columns stay as narrow as possible.
6. Legacy navigation notes are N/A in Java, because dropdowns replace them. Examples: "Press Enter To Return To…", "Press Enter To View a Valid List", "F7 – OK". The legacy F2 time-range selection becomes a selector control in Java.
7. "Original: HQ All Fields" lists are legacy only. Java replaces them with user-customizable views (users choose any fields). Do NOT build an HQ All Fields layout. Record the legacy list in the spec as reference only.
8. Where a sheet says the sort is "same as legacy" or "appears to be", confirm the actual legacy sort from git history or legacy code. Record it, and flag any difference from the sheet.
9. If a sort direction is not stated, use the legacy behavior and flag it.
10. No test deployment. Changes stay on dev.

# 1. CASE VIEW — sheet "Case View Layout Formats"
Constraint: Open, Closed and Queue cannot be displayed all together.

1.1 OPEN (master default)
   Sort: Date – Assigned RO (Case), DESC (most recent first). The previous default, Priority, is no longer correct.
   Columns: Assignment Number, TIN, Name Control, Balance (Case), Date – Assigned RO (Case), Date – Last Worked, Case Type, Date – Initial Contact Due, Date – Initial Contact Made, [Letter 725-B Date — EXCLUDE, future field], Indicator – Lien Filing, Case Code, Case SubCode, Priority, Case Grade

1.2 CLOSED
   Sort: Date – Closed (Case), DESC.
   Columns: Assignment Number, TIN, Name Control, Priority, Balance (Case), Date – Assigned Field, Date – Assigned RO (Case), Date – Closed (Case), Case Closure (ENTITY), Code – Closing (CC) Case, Case Type, Case Grade, Case SubCode, TIN File Source, Case Status

1.3 QUEUE
   Sort: always Priority Alpha.
   Columns: Potential Assignment Number, TIN, Taxpayer Name, Priority Alpha, Balance (Case), Zip Code, TIN File Source, Indicator – HINF, Indicator – 941, Case Type, Count TDI, Count TDA, Indicator – Statute, Case Grade, Flag – Queue Pick, Date – Assigned Queue, Case Status

1.4 OPEN + CLOSED
   Sort: TIN, then case status. Open cases come first, then closed cases by most recent Date – Closed first.
   Columns: Assignment Number, TIN, Name Control, Balance (Case), Date – Assigned RO (Case), Date – Closed (Case), Case Closure (ENTITY), Case Type, Count TDI, Count TDA, [Letter 725-B Date — EXCLUDE, future field], Indicator – HINF, Indicator – 941, Case Grade, Case SubCode, Case Status

1.5 OPEN + QUEUE
   Sort: Priority, then TIN (so like TINs group together).
   Columns: Assignment Number, Potential Assignment Number, TIN, Taxpayer Name, Balance (Case), Priority Alpha, Date – Assigned RO (Case), Date – Assigned Queue, Count Modules Total, Case Type, Indicator – HINF, Indicator – 941, Zip Code, Case Grade, Case SubCode, Case Status

1.6 RETURN TO QUEUE
   Access: everyone except national.
   Columns and sort: see workbook. If they are not available, list them as an open question.

Side comments (open questions, not requirements):
- "consider ENTITY calculating?" (Open layout, near Date – Initial Contact Made / Letter 725-B Date)
- "Need for hold file reasons" (Open layout, under Priority)
- Does "Priority" in 1.5 mean Priority Alpha?

# 2. MODULE VIEW — sheet "Module View Layout Formats"
Default sort for every status unless stated otherwise: TIN, then MFT, then Period (legacy multi-level sort).

Valid view combinations:
- Open & Closed
- Open & Queue
- Open, Closed & Rtn2que
- Closed & Rtn2que
- Rtn2que & Queue

Open, Closed and Queue cannot be displayed all together.

Statuses: Open, Closed, RTN2QUE, Transfer Queue/ASFR/OIC, Queue. RTN2QUE is staff-only in Java.

The Case Status column can be removed if there is a good header.

Also read the "Module View Fields" sheet; the layout sheet points to notes there.

2.1 OPEN (master default)
   Columns: Assignment Number, TIN, Name Control, MFT, Period, Type – TDI/TDA/ETC (Module), Balance (Module), Balance (Case), Date – Assigned RO (Module), Indicator – LFI (Module), Date – LFI Determination, Date – ASED (Module), Date – CSED (Module)

2.2 CLOSED
   Columns: Assignment Number, TIN, Name Control, MFT, Period, Type – TDI/TDA/ETC (Module), Balance (Module), Balance (Case), Date – Assigned RO (Module), Date – Closed (Module), Disposition Code (Module), Code – Closing (CC) Module, Case Status

2.3 QUEUE
   Columns: Potential Assignment Number, TIN, Name Control, MFT, Period, Type – TDI/TDA/ETC (Module), Balance (Module), Balance (Case), Case Type, Indicator – HINF, Indicator – 941, Date – ASED (Module), Date – CSED (Module), Priority, Case Status

2.4 OPEN + CLOSED
   Columns: Assignment Number, TIN, Name Control, MFT, Period, Type – TDI/TDA/ETC (Module), Balance (Module), Balance (Case), Date – Assigned RO (Module), Date – Closed (Case), Case Closure (ENTITY), Module Status, Case Status

2.5 OPEN + QUEUE
   Columns: Assignment Number, Potential Assignment Number, TIN, Name Control, MFT, Period, Type – TDI/TDA/ETC (Module), Balance (Module), Balance (Case), TIN File Source, Module Status, Case Status, Priority

2.6 RETURN TO QUEUE (RTN2QUE)
   Sort: TIN, then Period. This is a legacy dual sort with no MFT.
   Use this layout for these combinations:
   - Open, Closed & Rtn2que
   - Closed & Rtn2que
   - Rtn2que & Queue
   - the parallel Transfer Queue/ASFR/OIC combinations: Open, Closed & Transfer Queue/ASFR/OIC; Closed & Transfer Queue/ASFR/OIC; Transfer Queue/ASFR/OIC & Queue
   Access: everyone except national.
   Context note: nationally, there were 81 mods for MODman as of 7/30/2023.
   Columns: Assignment Number, TIN, Name Control, MFT, Period, Type – TDI/TDA/ETC (Module), Balance (Module), Balance (Case), Case Type, Date – Assigned RO (Module), Date – Closed (Module), Disposition Code (Module), Count TDI, Count TDA, Count Modules Total, TIN File Source, Module Status, Case Status, Case SubCode, Priority

Legacy HQ All Fields (reference only): Assigned To Field, BAL 941 ALL TDA, BAL 941 14 QTRs, BOD Code, Case Type, PDT, Mod CC, City, 941 TDA, 941 14, Overage Mod Date, Pot Age Mod Date, Case Pyr, FATCA, Field Hours, Case Grade, HI INC, IND 941, INN SP, Last Touch, Age, … (full list in workbook)

Open questions:
- RTN2QUE is marked in red, with "Transfer Queue/ASFR/OIC" in blue next to it. Is RTN2QUE being renamed, or is Transfer Queue/ASFR/OIC a separate status?
- RTN2QUE access says "staff only in Java" in one place and "everyone except national" in another. Which is correct?

# 3. ACTIVITY VIEW — sheet "Activity Layout Formats"
Time-range selector: 90 Days (default), 1 Year, Full File.

Second layer: Return/Remittance and/or Dispositions. These do NOT get a different default view; they use the same columns and sort.

Sort (legacy multi-level): Date – Activity, then TIN, then MFT, then Period. Confirm the Date – Activity direction against legacy.

Columns: RO Assigned Case, RO Posting Action, TIN, Name Control, Date – Activity, MFT, Period, Sequence Number, Module Type, Activity, Disposition Code, IDRS – Transaction Code, IDRS – Closing Code, Amount, Case Status, Extract Date

Legacy HQ All Fields (reference only): Action Date, Amount, RO POST, Assigned CFF, Assigned RO, BOD Code, Case Code, Clos Code, City, Disp Code, ENT Ind, Extract Date, Form 809, Lrg $, MFT, Period, PRTY, RO ASSN, Rpt Code, … (full list in workbook)

# 4. TIME VIEW — sheet "Time Layout Formats"
Time-range selector: 90 Days (default), 1 Year, Full File.

Second layer: Case (TIN) Time and/or Functional Time. All three views below use the same default sort.

Sort (legacy): Date – Time Report, then Assignment Number. Confirm direction against legacy.

4.1 CASE TIME + FUNCTIONAL TIME (default view)
   Columns: Assignment Number, Date – Time Report, TIN, Taxpayer Name, Contact Code, Hours, Time Code, Time Code – Description, Case SubCode – Time of Charge, Date – Extract

4.2 CASE (TIN) TIME ONLY
   Columns: Assignment Number, Date – Time Report, TIN, Taxpayer Name, Contact Code, Hours, Case SubCode – Time of Charge, Date – Extract

4.3 FUNCTIONAL TIME ONLY
   Columns: Assignment Number, Date – Time Report, Contact Code, Hours, Time Code, Time Code – Description, Date – Extract

Legacy HQ All Fields (reference only): BOD Code, Contact Code, Code CURR, Type CURR, Grade CURR, PRTY CURR, SUB CURR, Extract Date, Hours, Code TOC, Type TOC, … (full list in workbook)

Open question: the sheet shows these headers in ALL CAPS. Does the Employee View rule ("nothing stays all caps unless it's an acronym") apply here too?

# 5. EMPLOYEE VIEW — sheet "Employee View Formats"
Rules:
- Nothing stays in ALL CAPS unless it's an acronym.
- Anywhere in Employee Management, ROID displays as "Assignment Number".
- The sheet does not state a default sort. Use the legacy sort and flag it as an open question.
- "Adding employee type and title" means adding the Position Type and Title columns.

5.1 SUMMARY INFORMATION (default view)
   This replaces the prior default screen, with the 90-day column added back.
   Columns: Assignment Number (formerly ROID), Employee Name, Grade – Employee, Position Type, Title, Open Cases – Total, INV Adjust Percent, INV Warning, Last ICS EOD, Above Grade Time – 90 Days (added back), 30 Day Direct Time, Tour of Duty (TOD), SEID (Standard Employee ID), Badge

5.2 INVENTORY LEVELS
   Renamed from "Grade Level of Cases". Remove the Grade 9 column.
   Columns: Assignment Number, Employee Name, Grade, Position Type, Title, Open Cases – Grade 11, Open Cases – Grade 12, Open Cases – Grade 13, Open Cases – Total, Above Grade Time – 90 Days, Inventory Top, Inventory Bottom, Inventory Adjustment, Inventory Adjustment Reason, Warning
   Display rules:
   - Above Grade Time – 90 Days (formerly "90DAY ABV"): display as a percentage.
   - Inventory Top / Inventory Bottom: hover text says the adjustment is reflected here.
   - Inventory Adjustment: show the number with a % sign.
   - Warning: indicates the employee is above or below the target inventory range.

5.3 DIRECT CASE TIME HOURS
   Add Position Type and Title.
   Columns: Assignment Number, Employee Name, Grade, Position Type, Title, Direct Case Time Hours – 30 Day, 60 Day, 90 Day, 120 Day

5.4 ABOVE GRADED DIRECT TIME
   Add Position Type and Title.
   Columns: Assignment Number, Employee Name, Grade, Position Type, Title, Above Graded Direct Time – 30 Day, 60 Day, 90 Day, 120 Day

5.5 FORM 809
   Renamed from "Last Time Report/Form 809". Add Position Type and Title. Remove ICS Last EOD.
   Columns: Assignment Number, Employee Name, Grade, Position Type, Title, Form 809 Issued, Form 809 Date

5.6 TOTAL HOURS/BALANCE
   Marked "REMOVE VIEW", but the note says to confirm with a few people (possibly a high-level focus group) before removing it. Do NOT plan its removal; list it as an open decision.

5.7 UNIQUE ID/TSIGN USAGE
   REMOVE VIEW. Its fields move to the Summary default (likely SEID and Badge; confirm).

# Steps (repeat for each view)
1. If the workbook is available, parse the view's sheet (values, fill colors, font colors, notes) and reconcile it with the requirements above.
2. Explore the codebase (Java backend and frontend). Find where this view's default columns, default sort, sub-views, time-range default, view names and access rules are defined. Look in config files, enums, constants, SQL/JPA ORDER BY clauses, frontend grid or column definitions, and saved-layout defaults. Record file paths and line numbers.
3. Search git history (git log -S, git log -p on those files) for the last version where the defaults matched the requirements. Identify the commit(s) that changed them.
4. Map every workbook label to its backend field / DB column and its frontend column key. Flag any label with no mapping.
5. For every status, layout or sub-view, compare current behavior to required behavior:
   - which columns are present, and in what order;
   - the default sort fields, directions and tie-breakers;
   - renamed and removed views;
   - display rules;
   - access rules;
   - the time-range default.
6. Write that view's spec file.

# Required structure for EACH spec file (self-contained)
Each file must be implementable on its own, without reading the other four.
1. Summary: what's wrong in this view and what the fix is.
2. Source: sheet name, and the workbook path if it was read.
3. Rules: the global rules that apply, plus the view-specific rules.
4. Views and access: statuses, valid combinations, sub-views, time-range selector, renamed/removed views, access restrictions.
5. Requirements by status/layout/sub-view: for each one, a heading with its default sort, then a table with these columns:
   Order | Workbook Label | Backend Field / DB Column | Frontend Column Key | Sort Priority | Sort Direction | Display Rules / Notes
6. Future fields: excluded red/pink-shaded fields.
7. Legacy reference: the HQ All Fields list (not to be implemented).
8. Current vs. required: a gap table with file:line references.
9. Regression origin: commit hash(es) and what changed. Say "none found" if that's the case.
10. Implementation plan: ordered, file-by-file, smallest safe change set. Reuse the prior working code where sensible.
11. Dev test checklist:
    - each status/layout/sub-view shows the right columns in the right order with the right default sort;
    - switching status AND switching layout both re-apply the defaults;
    - excluded fields are hidden;
    - renamed views show their new names and removed views are gone;
    - display rules (%, hover text, casing) work;
    - the time-range selector defaults to 90 days where applicable.
12. Open questions and assumptions: include every open question listed above for this view.

# Finish
Do not modify code. When all five files are written, reply with:
- the file list;
- one line per view: its status/layout count, the number of gaps found, and whether the regression commit was found;
- every open question, grouped by view.
```
