Yes, UI first works. I'm writing the pick-list file now; the two Priority lists follow a fixed pattern, so they don't need screenshots.

`fieldListUtils.ts` is ready. It has the Priority and Priority Alpha lists plus three helpers. The comments are optional if you're typing it in by hand.

**Three one-line edits in QueryBuilderHelper.ts**

1. At the end of `mapDbColumnsToFields`, change the final `});` to:
   ```ts
   }).map(withPickList);
   ```
2. In `rqbToQueryWhereDTO`, wrap the run-time value:
   ```ts
   value: runningQuery ? padForRun(rule.field, backendOperator, formatValueByColumnType(columns, {field: rule.field, value: String(rule.value)})) : String(rule.value),
   ```
3. Make this the first line inside `processColumnValue`:
   ```ts
   value = matchPickListValue(name, value);
   ```

These work as long as `CustomValueEditor` hands non-date fields to the standard editor. That's the first screenshot below.

**Screenshots I need, most important first**

1. **QueryBuilderDateType.tsx, the whole file.** It decides whether the dropdowns render at all.
2. **QueryBuilderConstant.ts.** Use Ctrl+F for `PRIOR` and send those lines plus `getSafeFieldValue`. That gives me the real column names to replace the two TODOs.
3. **package.json, just the `react-querybuilder` line.** The version changes some details of the field setup.
4. **Small snippets from QueryBuilderComponent.tsx.** Send a few lines around each `rqbToQueryWhereDTO(` call and around the export handler (search `ExportUrl`). I need to confirm that run, count and export all go through the padding.
5. **A database check, if anyone has SQL access.** It shows only codes, no taxpayer data:
   ```sql
   SELECT '[' || <alpha_col> || ']', LENGTH(<alpha_col>), COUNT(*) FROM EVIEW_MV GROUP BY <alpha_col>;
   ```
   Run it again for the Priority column. That tells us whether Priority needs padding too.
6. **If there's time, spreadsheet photos of the other pick lists** (Time Code, Activity Report and the rest). I'll transcribe them. Adding them is data entry only, no code changes.

None of these need taxpayer names, TINs or balances on screen. Code and config are enough.

**The four hours**

- **First hour:** screenshots in, column names filled, file in place.
- **Second hour:** the three edits, plus any `CustomValueEditor` fix, until the dropdown renders.
- **Third hour:** testing:
  - 99a returns the same records as Legacy, run side by side.
  - The In list works.
  - A saved query reloads correctly.
  - Like searches still work.
  - The other views are unaffected.
- **Fourth hour:** remaining lists and the pull request.

Padding in the UI still needs Santosh's OK on the acceptance-criteria wording, so ask him while you test.

One question, because it changes how I write the rest: can you open this chat on your work machine to copy code, or are you typing it in by hand?



#22

The two columns we need are in your screenshots:

- **ARANK, VARCHAR2(4):** almost certainly Priority Alpha (" 99a" is exactly 4 characters).
- **RISK, NUMBER:** almost certainly Priority. Because it's a number column, it **never needs padding**. The " 99" in Legacy is just how the screen lines numbers up.

I'm updating the file with those names now.

`fieldListUtils.ts` is updated: the two placeholder column names are now ARANK and RISK. One query in TOAD confirms both guesses and the padding:

```sql
SELECT '[' || ARANK || ']' AS shown,
       LENGTH(ARANK)       AS len,
       RISK,
       COUNT(*)            AS cnt
  FROM EVIEW_MV
 GROUP BY ARANK, RISK
 ORDER BY RISK, ARANK;
```

**What we expect to see:**
- Rows like `[ 99a]` with length 4 and RISK = 99.
- Rows like `[100a]` with length 4 and RISK = 100.

If you see that, it confirms ARANK is Priority Alpha, the padding is a leading space, and RISK is Priority. Send me a screenshot of the first 15 or so rows; the output shows only codes and counts.

**Two things worth noting:**

- **More columns may need the same check later.** Several other code columns are fixed-width text: CASECODE, SUBCODE, BODCLCD, ORG, TPCTRL and INDICATOR_STATUTE. If any of them gets a pick list later, run the same query on that column before adding it to `PADDED_WIDTH`.
- **This also answers an open question on the Mandatory Accelerated backend.** MODELRANK looks like the model score whose existence was in doubt, so the "missing-data defect" risk in the README is probably resolved. The invented column names in that code are probably ARANK and MODELRANK.

**Next screenshot after the query: QueryBuilderDateType.tsx.** It's still the main thing standing between us and a working dropdown.

Small tip: crop the connection bar at the bottom of TOAD out of future photos, since it shows the database host name.

## 3

The fallthrough in `CustomValueEditor` is good news. Before I confirm the three edits, I'm checking the operator names in `rqbToQueryWhereDTO` against the padding code.

No changes are needed in `QueryBuilderDateType.tsx`. Lines 107 to 109 send every field that isn't a date to the library's standard `ValueEditor`, and that editor already draws a dropdown from a field's `values` list. So the one-to-two-hour risk from standup is gone. I also checked the operator names in `rqbToQueryWhereDTO`: they're `=`, `<>`, `IN` and `NOT IN`, which is what `padForRun` expects.

**Edits in QueryBuilderHelper.ts, in order**

1. **Add an import at the top.** Change the path if you didn't put the file in `src/utils/`:
   ```ts
   import { withPickList, padForRun, matchPickListValue } from "@/utils/fieldListUtils";
   ```
2. **Line 427**, the `});` that closes `mapDbColumnsToFields`. Change it to:
   ```ts
   }).map(withPickList);
   ```
3. **Lines 210 and 211**, the `value:` line in `rqbToQueryWhereDTO`. Replace it with:
   ```ts
   value: runningQuery ? padForRun(rule.field, backendOperator, formatValueByColumnType(columns, {field: rule.field, value: String(rule.value)})) : String(rule.value),
   ```
4. **First line inside `processColumnValue`:**
   ```ts
   value = matchPickListValue(name, value);
   ```
   I don't have a screenshot of that function. If its parameters aren't called `name` and `value`, use whatever it calls the column name and the value.

**Test it (about 15 minutes)**

- **Dropdown shows:** open the Case query builder and pick Priority Alpha. You should get a dropdown of 110 entries like "99a – PRIORITY LEVEL 99a".
- **Padding reaches the server:** choose `=` and `99a`, then press F12, open the Network tab, and run the query. The request body should show `"value":" 99a"` with the leading space.
  - If it does, compare the record count with Legacy run on the same day.
  - If the space is there but you still get 0 rows, the Java backend is trimming it. Send me that class.
- **In:** select 99a and 100a. The request body should show `" 99a, 100a"`, which is each code padded on its own.
- **Like:** it should still be a plain text box, with no padding.
- **Priority:** it should be a dropdown of 22 numbers, with no padding.
- **Saved query:** load an old one that has 99a. The dropdown should show 99a selected.
- **Export:** CSV export of the 99a query should return the same rows as the grid.

**Still pending:** the ARANK check query from earlier. If it shows `[99a ]` with the space at the end instead of the front, tell me before you test, because the padding would be on the wrong side.

**Separate from this ticket:** line 107 of `QueryBuilderDateType.tsx` checks for `'empty'` and `'notEmpty'`, but the operators are actually named `'null'` and `'notNull'`. So date fields still show a date picker when Empty is selected. That's a one-word fix, but I'd keep it out of this pull request.

## 4

I'll turn these into code now. One thing from your screenshots: Excel column C is hidden. The formula bar shows its header is "Modernized Field Name Update", which is the label users see in Modern. I'll ask you to unhide it for the next batch.

Now I'll type-check it against the real react-querybuilder types and verify every list count against the spreadsheet row counts.

The package registry isn't reachable from here, so I'll type-check against a minimal stand-in for the library's `Field` type and run the count checks with Node directly.

Every count matches the spreadsheet's row count, there are no duplicate codes, and the Priority lists still work in every view.

I've rebuilt `fieldListUtils.ts` with every pick list from these screenshots, from BOD Division Code through CSED/ASED. The Case view and GM Case Assignment are wired up. The Module, Activity and Time lists are written into the file, but they only appear once I have those views' column names.

**One edit in QueryBuilderHelper.ts, line 427.** Each dropdown now depends on which view it's in, so change `}).map(withPickList);` to:

```ts
}).map((f) => withPickList(f, currentNav));
```

If you skip this, TypeScript will show an error, so it can't be missed. The other three edits stay as they are.

**What's wired in the Case view**

| List (control #) | Values | Column | Confidence |
|---|---|---|---|
| BOD Division Code (10) | 4 | BODCD | Likely |
| Case Code (15) | 17 | CASECODE | Sure |
| Case Grade (69) | 5 | GRADE | Likely |
| Case SubCode (92) | 58 | SUBCODE | Sure |
| Case Type, TDA/TDI (269) | 6 | CASEIND | Needs checking |
| Clsd ENTITY Method (292) | 39 | DISPCD | Needs checking |
| Progname1 / Progname2 (349, 374) | 24 / 22 | PRGNAME1 / PRGNAME2 | Sure |
| TDI Select (398) | 100 (00–99) | CCNIPSELECTCD | Needs checking |

GM Case Assignment gets Case Grade and Case Type. I've assumed that covers both the domestic and international Case Assignment views. Every list count matches the spreadsheet's row count.

**Confirm the guessed columns.** One query in TOAD covers all of them:

```sql
SELECT 'BODCD' col, COUNT(DISTINCT BODCD) n, '['||MIN(BODCD)||']' lo, '['||MAX(BODCD)||']' hi, MAX(LENGTH(BODCD)) len FROM EVIEW_MV
UNION ALL SELECT 'CASECODE', COUNT(DISTINCT CASECODE), '['||MIN(CASECODE)||']', '['||MAX(CASECODE)||']', MAX(LENGTH(CASECODE)) FROM EVIEW_MV
UNION ALL SELECT 'SUBCODE', COUNT(DISTINCT SUBCODE), '['||MIN(SUBCODE)||']', '['||MAX(SUBCODE)||']', MAX(LENGTH(SUBCODE)) FROM EVIEW_MV
UNION ALL SELECT 'CASEIND', COUNT(DISTINCT CASEIND), '['||MIN(CASEIND)||']', '['||MAX(CASEIND)||']', MAX(LENGTH(CASEIND)) FROM EVIEW_MV
UNION ALL SELECT 'CCNIPSELECTCD', COUNT(DISTINCT CCNIPSELECTCD), '['||MIN(CCNIPSELECTCD)||']', '['||MAX(CCNIPSELECTCD)||']', MAX(LENGTH(CCNIPSELECTCD)) FROM EVIEW_MV
UNION ALL SELECT 'PRGNAME1', COUNT(DISTINCT PRGNAME1), '['||MIN(PRGNAME1)||']', '['||MAX(PRGNAME1)||']', MAX(LENGTH(PRGNAME1)) FROM EVIEW_MV
UNION ALL SELECT 'PRGNAME2', COUNT(DISTINCT PRGNAME2), '['||MIN(PRGNAME2)||']', '['||MAX(PRGNAME2)||']', MAX(LENGTH(PRGNAME2)) FROM EVIEW_MV
UNION ALL SELECT 'GRADE', COUNT(DISTINCT GRADE), '['||MIN(GRADE)||']', '['||MAX(GRADE)||']', NULL FROM EVIEW_MV
UNION ALL SELECT 'DISPCD', COUNT(DISTINCT DISPCD), '['||MIN(DISPCD)||']', '['||MAX(DISPCD)||']', NULL FROM EVIEW_MV;
```

What the results should look like if the guesses are right:
- **CASEIND:** about 6 values, from `[A]` to `[P]`.
- **DISPCD:** from `[0]` to about `[99]`.
- **CCNIPSELECTCD:** should look like `[00]` with a length of 2. If it shows `[5]` or `[ 5]`, the dropdown would send the wrong format, so tell me before testing.
- **Any column:** a space just inside the opening bracket means that column needs padding like ARANK. All the other codes fill their full width, so I don't expect any.

**Quick test once the edit is in**
- In the Case view, Case Code should show 17 options.
- Grade = 09 should return rows.
- Module and Activity should only show the Priority dropdowns for now. That's expected.
- If the Case dropdowns don't appear but Priority still does, the view isn't reaching the code as "CASE". Add `console.log(currentNav)` inside `mapDbColumnsToFields` and send me what it prints.

**Spreadsheet questions for whoever owns it.** None of these block the work, and the code follows the sheet as written:
- **Case SubCode, Time of Charge, code 324:** described as "ATAT COLLECTION STRATEGY". The other two SubCode lists call it "CAPTIVE INSURANCE ABUSE", so this looks like a copy error.
- **Progname2:** it's missing the two "FC SECURE MESSAGING" values that Progname1 has.
- **Code – BOD Client:** the row is highlighted yellow and has no values, so I skipped it.
- **CSED/ASED:** I couldn't find a matching column in EVIEW_MV. The list is in the file, but it isn't attached to any field yet.
- **Case Status:** it's marked "F6-Sort Only" for Inventory, so by design it gets no dropdown in the Case view.

**For the next batch, starting at Disposition Code**
- **Unhide column C.** Select columns B through D, right-click, and choose Unhide. Column C has the field names users see in Modern, which saves me guessing which database column each field is.
- **Send the TOAD Columns tab for MVIEW_MV, AVIEW_MV and TVIEW_MV.** Once I have those, the Module, Activity and Time lists can be connected.

## 5

Here's a Jira comment ready to paste. The "Dev testing" line in brackets is the only part you need to fill in.

---

**EN-1677: Query Builder pick lists and leading-space fix (UI)**

**Problem**
The Query Builder value field was free text. Some codes are stored right-aligned with a leading space. For example, Priority Alpha (`ARANK`, VARCHAR2(4)) is stored as " 99a". Searching for `99a` returned no records unless the user typed the space first.

**Fix**
- **Pick lists added** from the "Query Fields – Updated Field Names" spreadsheet: 52 lists, each shown only in the views the spreadsheet marks.
  - Equals and Not Equals use a dropdown.
  - In and Not In use a multi-select.
  - Like and Not Like stay a text box.
- **Padding at run time.** Values are padded to the stored width just before the query runs. Currently only Priority Alpha (ARANK, width 4) is padded. The Case-view code columns were checked in the database and fill their full width. Saved queries are stored without padding.
- **Saved queries matched on load.** Values are matched to the dropdown when a saved query loads, ignoring case, extra spaces and leading zeros (for example, "9" matches "09").

**Files**
- `src/utils/fieldListUtils.ts` (new): the lists, which views each list appears in, and the padding.
- `QueryBuilderHelper.ts`: one import and three one-line changes (`mapDbColumnsToFields`, `rqbToQueryWhereDTO`, `processColumnValue`).

**Acceptance criteria note**
Padding is done in the UI before the request is sent, not on the server as the last acceptance criterion says. Santosh needs to confirm this is acceptable or reword the criterion.

**Dev testing**
- Priority Alpha = 99a now returns records; before the fix it returned none. The request body shows " 99a".
- Case view dropdowns verified.
- [Module / Activity / Time / Employee / Case Assignment: add result]

**How to test (QA)**
1. Run Legacy and Modern side by side on the same day, in the same group, with the same output setting (Open). Priority Alpha = 99a: record counts match.
2. Priority Alpha In 99a, 100a: record counts match.
3. In each view, spot-check dropdowns against the Legacy pick lists:
   - Case Code: 17 values
   - Disposition Code: 39 in Module, 42 in Activity
   - Time Code: 76
   - Employee Grade: 10
4. Like and Not Like still show a text box.
5. Load a saved query created before this change. The correct option is selected and the results match.
6. For the 99a query, the record count and the CSV export match the grid.

**Open questions for the spreadsheet owner**
- **Case SubCode – Time of Charge, code 324:** the description says "ATAT COLLECTION STRATEGY", but the other SubCode lists say "CAPTIVE INSURANCE ABUSE".
- **Code – Progname2:** it's missing the two "FC SECURE MESSAGING" values that Progname1 has.
- **"FC SECURE MESSAGING DECLINED/DENIED":** it's 35 characters, but the longest stored program name is 27, so this option can't match any record.
- **Indicator – ASED (Module):** its values (TFR ASSESSED, NO RESPONSIBLE OFFICER…) read like a Trust Fund Recovery Penalty (TFRP) indicator, not ASED.
- **Title:** every value ends with " -" in the sheet. The dropdown uses the title without the dash.
- **No dropdown:** Code – BOD Client and Indicator – Income AGI/TPI have no values in the sheet.

**For product**
Legacy's pick lists cover 58 of the 293 SubCodes and 24 of the 66 Program Names in the data. The other values can still be searched with Like.

---

**No, the tester doesn't need the SQL scripts.** They should test through the screen against Legacy, using the steps in the note.
- **They're your checks, not test steps.** `picklist_check.sql` confirms the column choices and padding. Run it yourself before handing off.
- **The counts wouldn't line up anyway.** The app filters by the user's group and output setting, so a raw count in TOAD won't match the screen. Testers often don't have database access either.

**Before you post:**
- **Run both queries in `picklist_check.sql`.** If query 1 shows values like `[ 5]` or `[5]`, send me a screenshot. TDI Select would then need the same padding treatment, and the "only Priority Alpha is padded" line would change.
- **Fill in the Dev testing line** with the console results from the other views.

**Separate from this ticket:** the fix confirms Priority Alpha is stored with a leading space. That means the Mandatory Accelerated `EligibilitySql` filter `priority_alpha IN ('99a', …)` will match nothing until it trims the value.

