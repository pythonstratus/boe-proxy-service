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
