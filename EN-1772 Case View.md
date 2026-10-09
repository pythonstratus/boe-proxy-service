# EN-1772 – Case View Sorting

EN-1772 should cover sorting only. Of the fields on screen in Case View, 22 fail sort today; 19 have a stated rule and can start now, and 3 need Sarah's confirmation first. Source: Inventory-Case Fields tab, rows 2–39 (rows 40 and up weren't in the screenshots).

## Ticket details

| Field | Value |
| --- | --- |
| Summary | Case View – Sorting: match Legacy sort order for each Sort By field (SP 445) |
| Type / Epic | Story · Views |
| Labels | ENTITY-UAT-Defects, sp445 |
| Source of truth | SP 443 Views Testing 05-29-2026 workbook, Inventory-Case Fields tab, column H (SP 445) and column I (notes) |

**User story:** As a group-level user in Case View, I want each Sort By option to order the list exactly as Legacy ENTITY does, so I can sort a field, check the top and bottom, and spot outliers.

### Rules for every field

1. **Sort on the stored value, not the display text.** Dates sort as dates; amounts and counts sort as numbers, not text with commas.
2. **Blanks sit together** as one block.
3. **Sort the full list before paging**, so the first and last records match Legacy.
4. **Coded fields sort by code** when the rule says ascending, even if the screen shows only the description.

## Fields to fix

The first 13 rows are ready now; the last 3 wait on Sarah.

| Field | Required sort | Note |
| --- | --- | --- |
| Balance Due 941 14 Quarters | Smallest to largest | Sort as a number |
| Balance Due 941 TDAs | Smallest to largest | Currently reversed |
| BOD – Division Code | A to Z | — |
| Case Code | Smallest to largest | — |
| Case Status | A to Z | — |
| Case SubCode | Ascending | — |
| Case Type | TDA ONLY at the top | Order of the other values isn't stated; use the sheet's listed order (TDA&TDI (COMBO), FTDALERT, TDI ONLY, Other Investigation, NON-IDRS) unless Sarah says otherwise |
| Case Closure (ENTITY) | Ascending by code (00 → 99) | Sort by code even though the screen shows only the description |
| Code – Closing (CC) Case | Smallest to largest | — |
| Code – NAICS (Case) | Ascending | — |
| Code – Program Name 1 and Code – Program Name 2 | A to Z, blanks grouped | — |
| Code – TDI Selection Code | Smallest to largest, blanks grouped | Sort as numbers (2 before 11) |
| Count 941 14 Quarters, Count 941 TDAs, Count 941 TDIs, Count Modules Total, Count TDA, Count TDI | Lowest to highest | Sort as numbers (9 before 10) |
| Date – Assigned CFF | Must match Legacy | **Confirm first:** direction not stated; likely oldest to newest |
| Date – Assigned Field | Must match Legacy | **Confirm first:** direction not stated; likely oldest to newest |
| Date – Overage (Case) | Must match Legacy (wrong today) | **Confirm first:** direction not stated |

Already passing; don't break these: Assignment Number, Balance (Case), Case Grade, Indicator – Statute, Date – Assigned Queue, Date – Assigned RO (Case), Date – Closed (Case), Date – Date Death (Case), Date – Initial Contact Due, Date – Initial Contact Made.

## Acceptance criteria

1. Each field in the table sorts per its rule across the full list, and the first and last records match Legacy for the same group and status.
2. Amounts, counts and codes sort as numbers: 950.00 before 1,200.00, 9 before 10, TDI code 2 before 11.
3. Blank values sit together as one block.
4. Balance Due 941 TDAs no longer sorts in reverse.
5. Case Closure sorts by code (00 → 99) while the screen shows only descriptions.
6. The 10 fields already passing still pass.
7. Open and Queue are tested separately, since they load from different sources (ICS and DIAL).

## Not in this ticket

- **Displayed values (SP 443):** money format on the 941 balances, full words for Case Status, Report Builder values for Case Type, descriptions only for Case Closure, missing TDI Selection Codes, 6–7 digit BOD codes, and blanks instead of 00 or 01/01/1900. Re-test sorting after these land, especially Case Status, Case Type and the five date fields that show 01/01/1900.
- **Titles (SP 444):** add "Total" to Count Modules Total; remove "(TDA/TDI)" from the Case Type Sort By name.
- **Fields not on screen yet:** AGI, TPI and AGI/TPI Year (Latest Return); Assignment – Area, Territory and Group (SP 393).

## Questions for Sarah

1. Which direction should Date – Assigned CFF, Date – Assigned Field and Date – Overage sort?
2. Case Type: after TDA ONLY, what order should the other values follow?
3. When two cases share the sorted value, what breaks the tie?
