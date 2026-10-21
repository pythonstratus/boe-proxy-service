Yes, that's the whole change for this bug. She can test as soon as the UI build with these three files is deployed to the UAT environment she uses; she can't exercise it against your local dev server.

**Files changed (all `entity-ui`, no server changes):**
1. `src/app/components/CascadeComboStaff/CascadeComboStaff.tsx` — `handleChangeProfile` clears `entity_user_context_${seid}` after a successful staff profile change, before the reload.
2. `src/redux/slices/roleSlice.ts` — `changeRole` thunk clears `entity_user_context_${seid}` before its reload (non-staff profile change).
3. `src/app/components/Header/EntityHeader.tsx` — the Viewing-as effect discards a saved context outside the current role's hierarchy and falls back to the role's ROID instead of "-".

One precondition on your side before she starts: run the HS6XB flags query and confirm 22061787 is A/Y. If it's still Y/N, run the UPDATE first, or her AGM session is only working by fallback.

**Note to Ranjita:**

> Hi Ranjita,
>
> The fix for the "Viewing as" carrying over after a profile change is in the UI build now deployed to UAT. Please don't change any ENTEMP rows while testing; log out and back in before you start, and send a screenshot of the header after each step.
>
> **Test 1 — the bug itself**
> 1. Log in as your National profile.
> 2. Change Access → select Area 25 → Apply. The header should say "Viewing as: Area 25 (Southwest)".
> 3. Open Change Profile and switch to your AGM profile.
> 4. After the page reloads, expected: "ACTING MANAGER 22061787" and "Viewing as: 22061787". It must NOT say Area 25.
>
> **Test 2 — the reverse**
> 1. From the AGM profile, Change Access → pick an officer → Apply, and confirm the banner shows that officer.
> 2. Change Profile back to National.
> 3. Expected: the banner shows your National ROID, not the officer.
>
> **Test 3 — no regression**
> 1. As National, Change Access → Area 25 → Apply.
> 2. Refresh the page (F5). Expected: the banner still shows Area 25. A legitimate selection should survive a refresh.
> 3. Click Reset. Expected: the banner returns to your own ROID.
>
> **Test 4 — re-login**
> 1. As National, Change Access → Area 25 → Apply, then log out.
> 2. Log in as the AGM profile. Expected: no Area 25 anywhere in the header.
>
> If you ever see "Viewing as: -", that's a bug; screenshot it with the step you were on.
>
> Not part of this fix, so please don't log these yet: the Employee/Acting GM role choice for an AGM, "Viewing as" showing Employee vs Acting GM, and the group designation and hold-file tiles appearing in the Change Access list. Those are the ELEVEL 7 work we're tracking separately.
