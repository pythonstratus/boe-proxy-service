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
