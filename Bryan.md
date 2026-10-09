Hi Bryan, yes, that point is about whatever code picks the user's assignment at login. So if UserService.loadUser → findUserBySeid is what sets the ROID shown in the header, that's the spot.

You're also right about findAssignmentsWithPriority. My notes were based on an earlier version of the RBAC code, so please go by RbacUserService.getAssignmentForUser. Since findAllAssignmentsBySeid already orders by ROID, you can drop the tiebreaker item.

That ordering may actually narrow it down. If it's ascending, the fallback would pick 22061787 before 85906221. So if the header still shows 85906221, either:
1. The staff row is the one flagged EACTIVE='A' and PRIMARY_ROID='Y', so findCurrentActiveAssignment returns it, or
2. findUserBySeid has its own query and picks the row some other way.

Could you check whether loadUser/findUserBySeid goes through getAssignmentForUser, and run this for the tester's SEID?

SELECT ROID, TITLE, ELEVEL, EACTIVE, PRIMARY_ROID FROM ENTEMP WHERE TRIM(SEID) = '<tester SEID>';

If the staff row is the one flagged A/Y, it's likely a test data issue, not code. In that case we may not need a new staff vs. ELEVEL 7 rule at all.

```
Hi Bryan, thanks for trying that. Could you revert that update (back to N/N and the original ELEVEL)? Two active rows on 85906221 will muddy the results, and the 859062 prefix makes the row look like a staff user.

Instead, could you set up a clean AGM test record for yourself:

1. Run this and send me the result. I want to see whether Ranjita's own row is titled ACTING MANAGER:
SELECT SEID, NAME, TITLE, ELEVEL, EACTIVE, PRIMARY_ROID FROM ENTEMP WHERE ROID = 85906221;

2. Create a new acting manager row for HLHXB on an unused, non-staff ROID in Group 220617. SELECT COUNT(*) FROM ENTEMP WHERE ROID = <new ROID> should return 0. On that row:
- TITLE = ACTING MANAGER, ICSACC = 3, ELEVEL = 7
- AREACD/PODCD copied from an existing row in Group 220617
- EACTIVE/PRIMARY_ROID = A/Y, with your other rows set to inactive

3. Remove your old acting row from 85906221, or move it to another ROID, so only Ranjita's row is left there.

4. Log in as yourself and send screenshots of the header, Viewing as, Change Access and the menu bar.

5. Ask Ranjita to log out and back in. If her ACTING MANAGER label goes away, something was reading your row by ROID alone.

Also, Toad was connected to DEV2. Can you confirm that's the database behind the environment we're testing in?

That should tell us which issues are data and which are code.

```
