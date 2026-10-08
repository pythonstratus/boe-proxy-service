# System Data Validation – Action Items

From the Oct 8 System Data Validation call · Oct 8, 2026 · @Santosh Srinivasaiah

## Overview

The agreed priority is data accuracy first. Views and queries must display correct data across Open, Closed and Queue, at every access level, before any other feature is validated. Joint review sessions pause until the tech team shares a prioritized, dated schedule.

- **Owner:** named where the call named someone. "Tech team" means Islam assigns a person in his schedule.
- **Priority:** High blocks Sarah's validation. Medium is needed but not blocking.
- **Done when:** the check that closes the item. Update the Status dropdown as work moves.

## Key dates

The next joint session is likely the week of Oct 13, 2026, once Sri confirms after the team huddle. Friday's session (Oct 9) was cancelled.

- Oct 12, 2026: federal holiday (Columbus Day / Indigenous Peoples' Day), long weekend.
- Nov 11, 2026: Veterans Day (Wednesday).
- Nov 26, 2026: Thanksgiving. Sarah has no leave planned before then.
- Daily, about 9 AM and 3 PM: scheduled test-environment builds; expect slowdowns. Ranjitha to confirm exact times and timezone.

## Communication and environment

Developers and CCD testers share one test environment, so these items stop deployment glitches being logged as defects.

| Action item | Owner | Priority | Done when | Status |
| --- | --- | --- | --- | --- |
| Share the exact test-environment build times, with timezone, in the team chat. | Ranjitha | High | Times posted in chat. | Not started |
| Post a chat notice before each scheduled build and whenever the environment or ETLs are down. | Ranjitha, Santosh, Islam | High | Every build and outage has a notice, so Sarah's team can tell deployment glitches from real defects. | Ongoing |
| Tell the business team when a feature or fix is deployed and ready to test, and where to find it. | Islam | High | Sarah's team hears about each release before testing it. | Ongoing |

## Planning and prioritization

Islam owns one prioritized schedule of all remaining View and Query work, with dates the team can be held to. Target dates for the items below come from that schedule.

| Action item | Owner | Priority | Done when | Status |
| --- | --- | --- | --- | --- |
| Hold a team huddle to agree the plan, then update Sarah on next steps. | Sri, Islam, Ranjitha, Santosh | High | Sri has updated Sarah. | Not started |
| Compile every remaining View and Query item into one prioritized schedule with committed dates, and send it to Sarah. Build on Sarah's existing spreadsheet rather than recreating it. | Islam, with Ranjitha and Santosh | High | Sarah has the schedule, and every item has an owner and a date. | Not started |
| Match each red X on Sarah's spreadsheet to a Jira item and target sprint, adding tickets for any gaps. | Ranjitha, Santosh | High | No red X is left without a ticket and sprint. | Not started |

## Data accuracy and display

Sarah's sign-off test is three checks on every field: column title, data, and sorting with its direction. The checks run separately for Open View and Queue View, because they draw from different sources (ICS Open vs. the queue).

| Action item | Owner | Priority | Done when | Status |
| --- | --- | --- | --- | --- |
| Recheck every red X on Sarah's Open View spreadsheet and record what has changed since she built it. | Ranjitha | High | The spreadsheet reflects the current Open View. | Not started |
| Run the three checks on every field in every view for Queue status. Sarah's spreadsheet covers Open only. | Ranjitha, Santosh | High | A Queue version of the spreadsheet is complete for each view. | Not started |
| Carry Open View display fixes over to Queue View, starting with converting the 111900 placeholder to blank. | Tech team | High | Each Open fix has a matching Queue fix. | Not started |
| Add calculation and formatting logic where backend values aren't display-ready, so fields read as they do in legacy. | Tech team | High | Field values match the legacy view for the same case. | Not started |
| Fix TIN file source showing "no master file designation" in Queue. In legacy, queue records always have one. | Santosh | High | TIN file source is populated for queue records. | Not started |
| Fix the lien filing indicator in the module view, where two fields that should match show different data. | Santosh | High | Both fields show the same, correct value. | Not started |
| Confirm correct data across Open, Closed and Queue at every access level (group through national) in both views and queries, then tell Sarah. | Islam | High | Sarah is told it's ready, and joint sessions resume. | Not started |

Before submitting a fix, developers should compare the field against the same view in legacy, or ask Sarah's team how it should look.

## Views and queries parity

To the business, views and queries are one feature. Anything built for one applies to the other unless Sarah names an exception.

| Action item | Owner | Priority | Done when | Status |
| --- | --- | --- | --- | --- |
| Audit every query-side change (dropdown lists, layout behavior, sorting fixes) and apply it to the matching views, then the reverse. | Ranjitha, Santosh | High | Sarah's team confirms each change works in both places. | Not started |
| Ship every new change to both views and queries by default. | Tech team | High | Nothing ships to one side only without an exception from Sarah. | Ongoing |
| Fix queue query results not loading, and switching a query from Open to Queue applying a layout that doesn't fit the data. | Tech team | High | A Queue query returns data with the matching Queue layout. | Not started |
| Decide how query results will match the view experience, including switching status within results without re-running the query (as F2 does in legacy). | Islam, with Sri and Sarah | High | The approach is agreed and added to the schedule. | Not started |

The last item is an open design question. Islam floated loading data for all statuses (open, closed, queue, return to queue) into query results so users can switch status in place. Sarah's position is that query results should use the full view interface; no decision was made.

## Saved layouts migration

Legacy saved Report Builder layouts haven't been migrated. Today the modern layouts list shows only layouts created in Manage Layouts. Islam will set the final priority in his schedule.

| Action item | Owner | Priority | Done when | Status |
| --- | --- | --- | --- | --- |
| Analyze the legacy Report Builder saved-layout schema and map it to modern layouts. | Ranjitha | Medium | The field mapping is documented. | Not started |
| Build and run the migration of saved layouts (national, area, group and personal), following the earlier national and local query migration. | Tech team | Medium | Sarah's saved layouts (about 50 national) appear in her modern layouts list. | Not started |
| Apply legacy visibility rules to migrated layouts: assignment number, area-to-group hierarchy and change-access role. Confirm the rules with Sarah first. | Tech team, with Sarah | Medium | A layout created at area level is visible to the groups under that area, and to no one it shouldn't be. | Not started |

## Business validation and program lead

Sarah keeps testing in the background while the data work happens, and Sri sets the next joint session.

| Action item | Owner | Priority | Done when | Status |
| --- | --- | --- | --- | --- |
| Schedule the next joint session, likely the week of Oct 13, after the team huddle. | Sri | High | The invite is sent. | Not started |
| Continue case-by-case testing of the Details screen while data fixes are under way. | Sarah | Medium | Findings are shared with the tech team. | Ongoing |
| Prepare for the next joint session and tell Sri when ready. | Sarah | Medium | Sri has her readiness and availability. | Not started |
| Answer developer questions on how fields should look and behave. | Sarah's team | Medium | Developers get answers in chat. | Ongoing |

## Completed and suggested

Two items closed during the call:

- Sarah pasted screenshots of the TIN file source and lien filing indicator issues in chat for Santosh.
- Friday's joint session was cancelled.

The follow-ups below came up in discussion but weren't agreed. Confirm them before adding them to the schedule.

- [ ] Re-demo layout switching in query results for Sarah, who missed the earlier demo. (Islam)
- [ ] Log the intermittent freeze on group-level export as a defect. (Tech team)
- [ ] Give leadership an interim "what's working" view from the green checks on Sarah's spreadsheet, noting her stability caveat. (Sri)
- [ ] Ask about a protected testing window or environment for CCD validation, since integration testing shares the same environment. (Sri)
- [ ] Confirm two unclear points from the transcript with Sarah: the "Process 6" step for checking fields in legacy, and the exact name of the view with the lien filing indicator issue. (Ranjitha)
