Hi Bryan, yes, that point is about whatever code picks the user's assignment at login. So if UserService.loadUser → findUserBySeid is what sets the ROID shown in the header, that's the spot.

You're also right about findAssignmentsWithPriority. My notes were based on an earlier version of the RBAC code, so please go by RbacUserService.getAssignmentForUser. Since findAllAssignmentsBySeid already orders by ROID, you can drop the tiebreaker item.

That ordering may actually narrow it down. If it's ascending, the fallback would pick 22061787 before 85906221. So if the header still shows 85906221, either:
1. The staff row is the one flagged EACTIVE='A' and PRIMARY_ROID='Y', so findCurrentActiveAssignment returns it, or
2. findUserBySeid has its own query and picks the row some other way.

Could you check whether loadUser/findUserBySeid goes through getAssignmentForUser, and run this for the tester's SEID?

SELECT ROID, TITLE, ELEVEL, EACTIVE, PRIMARY_ROID FROM ENTEMP WHERE TRIM(SEID) = '<tester SEID>';

If the staff row is the one flagged A/Y, it's likely a test data issue, not code. In that case we may not need a new staff vs. ELEVEL 7 rule at all.
