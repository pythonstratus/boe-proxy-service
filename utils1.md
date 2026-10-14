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
