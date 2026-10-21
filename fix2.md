Right, the writer is `NationalSlice.ts` line 314 (`submitChangeAccess.fulfilled`), and `National.tsx` only removes the key on Reset. That's all I needed. Here's the patch: two files, no server change.

**Why it happens, now confirmed line by line:** `submitChangeAccess.fulfilled` saves the whole response (including `viewingAsDescription: "Viewing as: Area 25 (Southwest)"`) under `entity_user_context_${seid}`. `handleChangeProfile` in `CascadeComboStaff.tsx` then changes the role on the server, updates only `org` in `entity_original_role_${seid}`, and does a full reload (`window.location.href = "/"`). The reload wipes Redux but not localStorage, so the header's effect at line 403 finds the old context and displays it.

**Fix 1 — `CascadeComboStaff.tsx`, `handleChangeProfile`, insert after line 148 (`setfieldError(null);`):**

```tsx
// A profile change invalidates any Change Access "view as" context saved
// under the previous profile; the reload below would otherwise re-display it.
localStorage.removeItem(`entity_user_context_${seid}`);
```

`seid` is already in scope (line 150 uses it).

**Fix 2 — `EntityHeader.tsx`, replace lines 403–404 with this block.** It drops a saved context that doesn't belong to the current role, which also covers re-login after a manual ENTEMP change, where Fix 1 never runs:

```tsx
const raw = localStorage.getItem(`entity_user_context_${seid}`);
let selectedNewProfile = raw ? JSON.parse(raw) : null;

// A saved "view as" context is only valid inside the current role's own
// hierarchy (ROID digits: 2 = area, 4 = territory, 6 = group, 8 = officer).
// National (ELEVEL 0) and staff ROIDs (859062xx) may view anything.
if (selectedNewProfile && currentRole?.roid != null) {
  const roleRoid = String(currentRole.roid);
  const prefixLen = roleRoid.startsWith("859062")
    ? 0
    : ({ 2: 2, 4: 4, 6: 6, 7: 6, 8: 8 } as Record<number, number>)[currentRole.elevel ?? -1] ?? 0;
  const savedScope = String(
    selectedNewProfile.viewingOfficerRoid
      ?? selectedNewProfile.viewingGroupCode
      ?? selectedNewProfile.viewingTerritoryCode
      ?? selectedNewProfile.viewingAreaCode
      ?? ""
  );
  if (prefixLen > 0 && savedScope.slice(0, prefixLen) !== roleRoid.slice(0, prefixLen)) {
    localStorage.removeItem(`entity_user_context_${seid}`);
    selectedNewProfile = null;
  }
}
```

Lines 406–417 stay as they are. One small change in the `else` branch on line 416: replace `user?.roid` with `(user?.roid ?? currentRole?.roid)`, so Viewing as falls back to the role's ROID instead of "-" when `user.roid` is empty. The dependency array already has `currentRole`, so the guard re-runs when roles load.

**Why this is safe:** it mirrors the server's rule that hierarchy comes from ROID digits; national and staff are exempt, so a legitimate Change Access still survives a refresh; it only removes a context that contradicts the role; `Selected Profile`, `entity_original_role_`, the Reset flow, and `changeStaffRole` are untouched.

**Two checks before review:**
- Find in Files for `changeStaffRole(` and `changeRole(`. If a non-staff profile modal has its own handler, it needs the same one-line `removeItem`.
- Retest: as national, Change Access to Area 25, confirm the banner; Change Profile to AGM; after reload the banner must show her own ROID, not Area 25. Then Reset as national to confirm that path still clears.
