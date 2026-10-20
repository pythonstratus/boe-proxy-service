Hi Ranjita,

Here's where EN-1742 landed after going through the data today.

**What the issue is.** When you log into ENTITY, the app decides who you are from one row in ENTEMP: the row for your SEID flagged EACTIVE = 'A' and PRIMARY_ROID = 'Y'. That single row drives the header, your ELEVEL, Viewing As, the menus, and what Change Access offers. Your SEID (HS6XB) has two rows: 22061787 at ELEVEL 7, which is the Acting Group Manager assignment, and 85906288 at ELEVEL 0, which is a staff ROID (anything starting with 859062 is treated as staff). The active flag is on the staff row. The AGM row is flagged EACTIVE = 'Y' / PRIMARY_ROID = 'N', which is the app's inactive state. So every login makes ENTITY see you as an ELEVEL 0 staff user, not an AGM. That's why the header shows a staff ROID, Viewing As is blank, and Change Access only gives you employee tiles. The AGM behavior hasn't been exercised at all yet, because the app has never seen you as ELEVEL 7.

Side note: the ROID in your original screenshot, 85906221, no longer exists in ENTEMP, so the UAT data has changed since you filed the ticket.

**What the fix is.** Flip the flags on your two rows: 22061787 becomes EACTIVE = 'A' / PRIMARY_ROID = 'Y', and 85906288 becomes EACTIVE = 'Y' / PRIMARY_ROID = 'N'. Then log out and back in. If the header still shows the old ROID after that, we'll bounce entity-service, because it caches the user context in memory.

**Where the fix is.** The ENTEMP table in the UAT database, on the two rows for your SEID. No code change and no deployment for this step.

**Why this approach.** The A/Y flags are the app's source of truth for your role, and the code is reading them correctly. Changing the code to prefer an ELEVEL 7 row over the flagged row would hide the data problem and change login behavior for every user. Fixing the row makes the app see what the test scenarios assume: an AGM on the 22061787 assignment. It's also the prerequisite for the rest of the ticket. Until you're running as ELEVEL 7, we can't tell which AGM symptoms are real code gaps.

**What could have happened.** Two possibilities, and I'm not sure which yet. One, the profile was set up or edited with the staff row active. Your 22061787 row changed during today (ELEVEL -2, N/N this morning; ELEVEL 7, Y/N by noon), so someone or something was touching it. If you or Bryan edited it, let me know. Two, the app did it. Change Access works in two steps: reset every row for the user to Y/N, then activate the selected row to A/Y. If that activation went to the staff row instead of the AGM row, every Change Access would put you back in the staff state. If that's what happened, it's a bug in the activation step, and we'd fix it in ChangeAccessService. There's also a data cleanup to do separately: 22061787 is on another SEID too (the R COLLINS AGM entry from your screenshot), and 85906288 is active for another SEID as well. The code expects one person per ROID.

**What's next.** After the data change, log in and confirm the header shows 22061787 and Acting Manager. Then use Change Access once and tell me. I'll re-query ENTEMP to see whether the staff row came back active, which tells us whether the activation bug is real. After that, run the AGM scenarios from the acceptance table. If Viewing As or the Group option still doesn't appear at ELEVEL 7, that's the remaining code work (the Group/Self mode for ELEVEL 7) and a separate fix from this one.

```

HS6XB's rows explain the bug, and the fix is data, not code. (I'm taking HS6XB as Ranjita: the results of your first query included her 85906288 row, which only the SEID filter would return. If she's 05VSB, tell me and the fix changes.)

Her only active-primary row (A/Y) is **85906288, ELEVEL 0**, a staff ROID. Her AGM row, **22061787 at ELEVEL 7, is Y/N**, the reset state. `findCurrentActiveAssignment` only returns A/Y, so the app resolves her to the staff row every time. The code is reading the data as written; the wrong row is active. Also, 85906221 has no rows at all now, so the ticket screenshot is stale.

**Fix:** make 22061787 her active row and reset the staff row.

```sql
UPDATE ENTEMP SET EACTIVE='Y', PRIMARY_ROID='N'
 WHERE TRIM(SEID)='HS6XB' AND ROID=85906288;
UPDATE ENTEMP SET EACTIVE='A', PRIMARY_ROID='Y'
 WHERE TRIM(SEID)='HS6XB' AND ROID=22061787;
COMMIT;
```

Have Ranjita log out and back in. If the header still shows the old ROID, restart entity-service, since `ChangeAccessService` keeps the user context in an in-memory map. Expected result: header shows 22061787 at ELEVEL 7. Then retest Viewing As and Change Access. If those are still wrong at ELEVEL 7, that's the code gap (no Group/Self handling for ELEVEL 7). To patch that, I need `ELevelService.getELevel` and the Change Access apply handler.

Two things to watch:
- HS6XB's 22061787 row changed between your 9:37 and 12:44 queries, from -2/N/N to 7/Y/N. Whoever changed it set it to the reset state, not the active one. The UPDATE above corrects that.
- `findByRoid(Long)` returns `Optional`, and 22061787 and 85906288 each have two rows now (05VSB/HS6XB and HS6XB/S2HNB). Any caller will throw `IncorrectResultSizeDataAccessException`. Grep for `findByRoid(` and switch callers to `findByRoidAndSeid`. The staff row also keeps `isStaffUser('HS6XB')` true, so if staff-only menus still show, that's why.

```
