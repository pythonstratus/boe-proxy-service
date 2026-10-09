# EN-1775 – Employee View Sorting

## What to change on EN-1775

EN-1775's description is copied from the Module View story and must be replaced. The Employee View Fields tab shows each row is an employee (badge, name, grade, title), so the TIN → MFT → Period rule does not apply here. Use the text below instead.

One blocker: on 06/12/2026 the business marked this view "wrong data displaying – cannot validate". Only rows 3–11 have results, so Sarah's team must re-test the rest once the data is right.

On the Employee View Fields tab the status columns sit one to the right of the other tabs. Column G is SP 444 (title and Sort By name), H is SP 443 (displayed values), and I is SP 445 (sort order).

| Field | Value |
| --- | --- |
| Summary | Employee View – Sorting: match Legacy sort order for every Sort By field (SP 445) |
| Type / Priority / Epic | Story · Highest · Views (no change) |
| Labels | ENTITY-UAT-Defects, sp445. Add sp444 if any Sort By name is wrong (e.g., Grade). Link SP 392 and SP 393 for the fields still to be added. Keep sp443 only if this ticket also fixes displayed values. |
| Source of truth | SP 443 Views Testing 05-29-2026 workbook, Employee View Fields tab, columns A–J |

## Description (paste into EN-1775)

### User story

As a group-level user in Employee View, I want each Sort By option to order the list exactly as Legacy ENTITY does, so I can sort a field, check the top and bottom of the list, and spot outliers.

### Background

Users find problem records by sorting, not scrolling, so each field's default sort direction and blank handling must match Legacy. Letting a user flip ascending/descending does not replace the correct default. Sort requirements are tracked in SP 445 (column I of the SP 443 Views Testing workbook, Employee View Fields tab).

### Sorting rules for every field

1. **Sort on the stored value, not the display text.** Dates sort by date, not as MM/DD/YYYY text (text sorting puts 01/05/2026 before 12/31/2025). Amounts and counts sort as numbers, not as text with commas or $.
2. **Direction is set per field** in the table below. Every sort rule recorded in the workbook so far is ascending: smallest to largest, lowest to highest, or A to Z.
3. **Blanks are grouped together**, never scattered through the list.
4. **Blank stays blank.** Never fill a missing value with 0, 00 or 01/01/1900. Those sort as real values and push records to the wrong place, and 00 can be a real code (the Case View sheet says a zero closing code means something else).
5. **Coded fields follow the field's rule even when only the description shows.** If the rule says "ascending" and the screen shows only the description, sort by the code order Legacy uses, not alphabetically by description. Confirm any unclear field before coding.
6. **Sort By names match the column header exactly** (SP 444). No "Default" wording in the Sort By box.
7. **Only the selected field is flagged** as the sort, and only its header is highlighted.
8. **Sort the full result set before paging**, so the first and last records match Legacy.
9. **Display format follows Legacy Report Builder output**, not the Legacy screen, which is cut short by width limits (per the workbook's header note).

## Field-by-field sort rules (Employee View Fields tab)

Only rows 3–11 have test results, and all nine fail sort. The rest of the view hasn't been validated: on 06/12/2026 the business marked it "wrong data displaying – cannot validate". Rules marked **(P)** are proposed by us, not stated in the sheet; Sarah confirms them before coding.

### Key changes for the developer

1. **Assignment Number sorts ascending.** Modern sorts it the opposite way today.
2. **Split Position Type and Employee Type.** Legacy EMPman combines them into one "Position/Employee Type" sort. Modern needs two separate Sort By options.
3. **Remove Open Cases – Grade 9** from the view and from Sort By. There are no grade 9 cases anymore.
4. **Grade Level of Cases group.** Open Cases – Grade 11, 12, 13 and Total, plus Inventory Warning, sit under a two-level header "Grade Level of Cases". Each one stays its own Sort By option.
5. **Inventory Warning must be sortable.** Legacy offers it only through F6 sort. Its column header reads just "Warning".
6. **Add five fields:** POD Code, Employee update, Prior Assignment number, EACTIVE and ELEVEL (SP 392). All must be available to query, sort and layouts. EACTIVE and ELEVEL display for ENTITY staff only.
7. **Add Assignment – Area, Territory and Group** (SP 393).
8. **Hover pop-up text** for each field comes from the ENTITY User Guide, Chapter 6 Employee Management (link in the workbook's Notes header).

### Field table

Status reads Title (SP 444) · Values (SP 443) · Sort (SP 445). A dash means not validated yet.

| Row | Field (Legacy name if different) | Title · Values · Sort | Required sort (SP 445) | Display / other notes |
| --- | --- | --- | --- | --- |
| 3 | Assignment Number | ✓ ✓ ✗ | Ascending. Modern currently sorts it the opposite way. | — |
| 4 | Assignment – Area (Legacy: use Assignment Number) | ✗ ✗ ✗ | (P) Ascending | Not on screen yet; add it (SP 393) |
| 5 | Assignment – Territory (Legacy: use Assignment Number) | ✗ ✗ ✗ | (P) Ascending | Not on screen yet; add it (SP 393) |
| 6 | Assignment – Group (Legacy: use Assignment Number) | ✗ ✗ ✗ | (P) Ascending | Not on screen yet; add it (SP 393) |
| 7 | Badge | – – ✗ | Fails; rule not stated. (P) Ascending | — |
| 8 | Employee Name | – – ✗ | Fails; rule not stated. (P) A to Z | — |
| 9 | Form 809 | – – ✗ | Fails; rule not stated. (P) Ascending | — |
| 10 | Form 809 Date | – – ✗ | Fails; rule not stated. (P) Oldest to newest, sorted as a date | — |
| 11 | Employee Grade (Legacy: Grade – Employee) | – – ✗ | Fails; rule not stated. (P) Ascending | Column header shows just "Grade" |
| 12 | ICS Access | – – – | (P) Code order: 1–9, then A–C | Coded values, list below |
| 13 | Inventory Adjustment (Legacy: INV Adjust Percent) | – – – | (P) Lowest to highest | One of rows 13–14 also appears under Grade Level of Cases (confirm which) |
| 14 | Inventory Adjustment Reason (Legacy: INV Adjust Reason) | – – – | (P) A to Z, blanks grouped | See row 13 |
| 15 | Open Cases – Grade 9 | — | Remove from Sort By | Remove from the view: no longer any grade 9 cases |
| 16 | Open Cases – Grade 11 | – – – | (P) Lowest to highest | Under the two-level header "Grade Level of Cases" |
| 17 | Open Cases – Grade 12 | – – – | (P) Lowest to highest | Under "Grade Level of Cases" |
| 18 | Open Cases – Grade 13 | – – – | (P) Lowest to highest | Under "Grade Level of Cases" |
| 19 | Open Cases – Total | – – – | (P) Lowest to highest | Under "Grade Level of Cases" |
| 20 | Position Type | – – – | Its own Sort By option, split from Employee Type. (P) Code order | Coded values B–Y, list below |
| 21 | SEID (Legacy: SEID (Standard Employee ID)) | – – – | (P) A to Z | — |
| 22 | Title | – – – | (P) A to Z | 39 titles, list below |
| 23 | Tour of Duty (TOD) | – – – | Not stated; confirm | — |
| 24 | Employee Type | – – – | Its own Sort By option, split from Position Type. (P) Code order | Coded values C–T, list below. Show R (REV OFF) as "RO" |
| 25 | Inventory Warning (Legacy: INV Warning) | – – – | Must be a Sort By option (Legacy: F6 sort only) | Header shows just "Warning", under "Grade Level of Cases" |
| 26 | Last ICS EOD | – – – | (P) Oldest to newest, sorted as a date | — |
| 28 | POD Code (new) | — | Sortable. (P) Ascending | Add field. Post of Duty Code, 3 digits. Source below |
| 29 | Employee update (new) | — | Sortable. (P) Oldest to newest, sorted as a date | Add field. Last date the employee record was updated or created. Source below |
| 30 | Prior Assignment number (new) | — | Sortable. (P) Ascending, sorted as a number | Add field. Previous ROID/TSIGN collection assignment number. Source below |
| 31 | EACTIVE (new) | — | Sortable | Add field. Display for ENTITY staff only |
| 32 | ELEVEL (new) | — | Sortable | Add field. Display for ENTITY staff only |

### Code lists

**ICS Access (row 12)**

| Code | Description |
| --- | --- |
| 1 | MANAGER |
| 2 | SECRETARY/CLERK |
| 3 | ACTING MANAGER / SPF ADVISOR |
| 4 | RO AIDE /TE/OCR/TSR/TSS |
| 5 | RO/REV REP/STAFF/ISP |
| 6 | TERRITORY |
| 8 | AREA DIRECTOR |
| 9 | SYSTEM ADMINISTRATOR |
| A | LIMITED ACCESS (marked \* in the sheet) |
| B | REGION-READ ONLY |
| C | ICS QUALITY ANALYST |

**Position Type (row 20)**

| Code | Description |
| --- | --- |
| B | DISBANDED OR BLOCK |
| D | DETAILED OUT |
| E | ENTITY COORDINATOR |
| F | FRAUD REFERRAL SPECIALIST |
| I | TRAINEE |
| L | LONG-TERM ACTING MANAGER |
| M | MANUALLY MONITORED INSTALLMENT AGREEMENTS |
| N | NO ADDITIONAL DUTIES - NORMAL |
| O | OIC SPECIALIST |
| Q | CQMS |
| S | SPECIAL COMPLIANCE SPECIALIST |
| T | OJI COACH |
| U | COLLATERAL DUTIES |
| V | VACANT |
| W | WI |
| Y | NO INVENTORY |

**Employee Type (row 24)**

| Code | Description |
| --- | --- |
| C | CLERICAL |
| D | AREA |
| H | HOLDFILE |
| M | MANAGER |
| P | PARAPROF |
| R | REV OFF – display as "RO" |
| S | SPF |
| T | TXRESREP |

**Title (row 22) – 39 values, A to Z**

```csv
Title
ACT. TERRITORY MANAGER
ACTING MANAGER
ADVISOR
ADVISORY GROUP MANAGER
ANALYST
AREA DIRECTOR
BANKRUPTCY SPECIALIST
CAC
CADRE MANAGER
CASE PROC. SUPPORT MGR
CLERK
COLLECTION SUPPORT MGR
GRP/TERRITORY DESIG.
ICS/ENT QUALITY ANALYST
INSOLVENCY MANAGER
INSOLVENCY SUPPORT MGR
IQA MANAGER
LIMITED ACCESS
MANAGER
NATIONAL ANALYST - SB/SE
NATIONAL ANALYST-WI
POLICY ANALYST
PROP AP & LIQ SPEC-PALS
REVENUE OFFICER
REVENUE REPRESENTATIVE
RO AIDE
RTO
SECRETARY-AREA
SECRETARY-GROUP
SECRETARY-TERRITORY
STAFF ASSISTANT
TAX EXAMINER
TAX RESOLUTION REP
TAXPAYER SERVICE SPEC.
TECH SERVICES GRP MGR
TER CASE PROC SUPRT MGR
TER INSOL SUPRT MGR
TER TECH SERVICES MGR
TERRITORY MANAGER
```

### New field sources (rows 28–32, SP 392)

All five can be queried, sorted or displayed in layouts. Sort Employee update as a date and Prior Assignment number as a number, not as text.

| Display name | ENTEMP name | Field on E5 (ICS or employee table) | Format / note |
| --- | --- | --- | --- |
| POD Code | PODCD | EMP-POD-CD or EMPPODCD | CHAR(3), positions 188–190 |
| Employee update | EMPUPDATEDT | EMP-UPDT-DT | Date YYYYMMDD, positions 57–64 |
| Prior Assignment number | PREVID | OLD-ASG-NUM | 8-digit integer, positions 125–132 |
| EACTIVE | — | EACTIVE (ENTEMP ENTITY field) | Display for ENTITY staff only |
| ELEVEL | — | ELEVEL (ENTEMP ENTITY field) | Display for ENTITY staff only |

## Acceptance criteria, testing and questions

### Acceptance criteria

1. Assignment Number sorts ascending, matching Legacy.
2. Sort By offers Position Type and Employee Type as two separate options, each ordering the full list by its own value.
3. Open Cases – Grade 9 no longer appears in the view or in Sort By.
4. Every other field in the field table is in Sort By. Choosing it orders the full list per its confirmed rule, and the first and last records match Legacy EMPman for the same group.
5. Inventory Warning, POD Code, Employee update, Prior Assignment number, EACTIVE and ELEVEL can be sorted. EACTIVE and ELEVEL are visible only to ENTITY staff.
6. Blank values sit together as one block, and no record shows 0, 00 or 01/01/1900 in place of a blank.
7. Date and number fields sort by value, not text: Form 809 Date, Last ICS EOD and Employee update as dates, Prior Assignment number as a number.
8. Sort By names match the agreed field names, no "Default" text appears, and only the sorted column's header is highlighted.

### Test approach

- Compare against Legacy EMPman (F6 sort) for the same group, field by field, using the Employee View Fields tab as the checklist.
- For each field, sort and check both the top and the bottom of the list, not just page 1.
- Record a short Snagit clip per sort option and post it in Teams for Sarah's sign-off.

### Questions for Sarah

1. What is the default sort when Employee View opens? (P) Assignment Number ascending.
2. When two employees share the sorted value, what breaks the tie? (P) Assignment Number ascending.
3. Rows 7–11 (Badge, Employee Name, Form 809, Form 809 Date, Grade) fail sort with no note. Are our proposed rules right?
4. The 06/12/2026 "wrong data displaying" banner: is that fixed, so her team can validate rows 12–32?
5. The red header says all fields are in the wrong order for the Activity View sort dropdown. Does that apply to Employee View's Sort By list too, and what order does she want?
6. Should Sort By say "Employee Grade" (the new name) or "Grade" (the column header)?
7. Coded fields (ICS Access, Position Type, Employee Type): sort by code or by description?
8. Can non-ENTITY staff sort by EACTIVE and ELEVEL, or are those options hidden too?
9. The note "Also under Grade Level of Cases View" sits by rows 13–14. Does it belong to Inventory Adjustment or Inventory Adjustment Reason?
10. Where do grouped blanks go: top or bottom of the list?
