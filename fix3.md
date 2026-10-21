In the thunk itself, not the reducer. Line 83 reloads the page from inside `changeRole` before it returns, so `changeRole.fulfilled` (line 133) may never run. Put the clear next to the existing localStorage writes, so it runs before the reload.

**`roleSlice.ts`, between line 82 and line 83:**

```ts
localStorage.setItem("nonstaffRolePayload", JSON.stringify(data.newRole));
localStorage.removeItem("staffRolePayload");
// The active role changed, so any saved Change Access "view as" context
// belongs to the previous role. Must run before the reload on the next line.
localStorage.removeItem(`entity_user_context_${seid}`);
window.location.href = "/";
return data.newRole;
```

`seid` is already in scope from the thunk's argument on line 69.

Nothing in `extraReducers` changes. The `CascadeComboStaff` edit stays as is; `changeStaffRole` doesn't reload inside its thunk, so the component is the right place there.
