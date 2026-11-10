# System Views Modernization – Meeting Notes

Nov 10, 2026 · @Santosh Srinivasaiah

## At a glance

Views are testing well in Open status, but Queue status has widespread data defects that Sarah will log in a new spreadsheet. The call also settled several display rules and cleared up how prior assignment numbers and activity detail screens should work.

**Meeting:** System Views Modernization sprint check-in, Nov 10, 2026 (about 35 minutes; transcribed by Minutes AI at 12:03 PM).

**Topics:** sprint status, field and sort fixes, Queue defects, Employee view Advisor record, Time Management, prior assignment number, Employee view demo, Activity detail screen.

| Person | Side | Role in this meeting |
| --- | --- | --- |
| Santosh | Dev team | Gave the sprint overview; demoed the Employee view layout change |
| Ranjitha | Dev team | Testing all views; raised most of the questions |
| Sarah | Client | Main business reviewer; testing Open and Queue status |
| Nicole | Client | Commented on the Advisor record and E levels |
| Steve (inferred) | Client | Explained assignment-number rules (Speaker E) |
| Samuel | Client | Explained how parentless activities get a parent record |
| Client SME, unnamed (Speaker D; possibly Eric) | Client | Walked through Legacy screens, time codes, rounding and prior assignment number |

Also mentioned: Eric (thanked for answering earlier questions), Salish (dev who built the Add Field grouping) and Sri (updated the meeting invite).

**Transcript caveats:** speaker labels are auto-assigned and sometimes merge people. For example, Speaker A is mostly Sarah but is Nicole at 10:27. Names are inferred from context, and garbled terms are marked "as transcribed".

## Key decisions and confirmations

Seven points were settled in the call. Each is explained in the sections below.

- **Case view stays code-only for case code and subcode.** Time view shows code plus literal. Some case subcodes double as time codes, so the literal is only needed in Time.
- **Address/demographic fields stay grouped together.** This is the one exception to alphabetical order, in both Add Field and Sort By. No prefix is needed.
- **Employee view Display options move into Layout.** Sarah approved. Employee view has no statuses or filters (other than querying), so a separate Display menu isn't needed.
- **Case Closure Entity shows blank when the DIAL value is 0.** A zero means null. It does not mean "Undo Prior Transaction."
- **Prior Assignment Number and Prior Assignment Action Date are a new enhancement.** They aren't shown in Legacy, and they apply to open cases as well as Queue.
- **A blank case summary on activities is expected.** Activities can exist without a parent case. Legacy's activity detail screen has no Time or Summary section. Not a defect.
- **Queue defects will arrive in batches.** Sarah will send Case view first so the dev team can start. Ranjitha agreed.

## Action items

The dev team has 11 follow-ups. The client team has 5. No due dates were set in the call. Owners in brackets are inferred where the transcript didn't name one.

### Dev team

- [ ] **Ranjitha / Salish:** group address/demographic fields together in the Sort By dropdown, the same way as Add Field.
- [ ] **Dev team:** fix Case Type in Queue status. It shows "Other", but the queue holds only TDA, TDI and combo cases.
- [ ] **Dev team:** swap the Code Closing CC Case and Case Closure Entity mappings. Code Closing CC Case should be a 3-digit number.
- [ ] **Dev team:** show Case Closure Entity as blank when the DIAL value is 0.
- [ ] **Ranjitha:** test Queue cases in detail across all views.
- [ ] **\[Dev team\]:** find out why the Advisor assignment number shows in the new Employee view but not in Legacy. Check E level, duplicate assignment numbers in one group, and position title.
- [ ] **\[Dev team\]:** apply the two-decimal fix to Excel exports in Time Management (Weekly Time Verification).
- [ ] **\[Dev team\]:** use the right case code/subcode (current vs at time of charge) when matching time records to DIAL.
- [ ] **Ranjitha:** review the prior assignment number examples and confirm the data source.
- [ ] **Santosh:** deploy the Employee view Layout change.
- [ ] **Ranjitha:** finish validating the Time view code and descriptor fields.

### Client team

- [ ] **Sarah:** send the Queue defect spreadsheet in batches, starting with Case view.
- [ ] **Sarah:** retest Open status (likely Open and Closed together) and report what still fails.
- [ ] **Client SME (Speaker D):** send example cases for Prior Assignment Number and Action Date. The query details were already sent.
- [ ] **Client SME (Speaker D) / Steve:** check the Legacy Employee Management code to see what it displays and hides.
- [ ] **\[Eric or Steve\]:** check the E level / active status of the Advisor record, as Nicole suggested.

## Sprint status and testing progress

The whole sprint went to Views: adding fields and meeting sort expectations. Fixes are in the test environment. (00:06–04:22)

**Santosh's overview**

- Ranjitha flags gaps as she tests, and the team fixes and pushes them right away.
- He thanked Sarah and Eric for answering the team's earlier questions.

**What Ranjitha tests against**

- The **All Views** sheet, which sets the layout for every view.
- The **SP443** sheet, which lists all sorts. The client team also traced Legacy code to specify primary and secondary sorts.

**Fixes completed across all views**

- Sorts work as specified.
- Literals display, and fields that need both code and literal show both.
- Export fixed: no more 1/1/1900 dates, and nulls show blank instead of a wrong zero.
- Amount fields are formatted.
- Hours in time fields show two decimal places.
- Every dropdown lists the correct fields, mapped correctly. This includes Add Field.
- Demographic fields are grouped together at the bottom of the field list.

## Case view vs Time view: code and literal display

Sarah confirmed the current build is correct. Case view shows case code and subcode as code only, and Time view shows code plus literal. (01:28–03:18)

- **Ranjitha's question:** Time view now shows code and literal for fields like case code current and case subcode current. Should Case view stay as it is?
- **Sarah's answer:** yes. The fields serve different purposes. Some case subcodes are also time codes, and the IRM treats them as interchangeable in many ways. So Time needs the literal and Case doesn't.

## Address field grouping in Sort By

Address fields are grouped in Add Field but still alphabetical in Sort By. The dev team will group them there too. (04:22–05:47)

- **What Sarah saw:** in Sort By, City sits near the top and Zip Code at the bottom, mixed in with other fields.
- **Why grouping matters:** users prefer address-type fields together. They're used only in specific situations and usually together, so they're the one exception to alphabetical order.
- **Legacy approach:** each address field name had an "XADD;" prefix. Sarah offered an "Address" prefix or abbreviation if that helps.
- **Dev response:** Ranjitha said no prefix is needed. Salish built the grouping for Add Field and can do the same for Sort By.

## Queue status defects

Open status looks "really, really good," but switching to Queue shows wrong or missing data in several fields. Sarah gave two examples and will log the rest. (05:47–09:41)

**Example 1: Case Type**

- Sorting by Case Type in Queue shows "Other" in the column. Sarah assumes that means Other Investigation (OI).
- There are no OIs in the queue at all. It holds only TDA, TDI and combo cases, and those are what the column should show.

**Example 2: two closing fields are swapped**

| Field | Should show | Shows now |
| --- | --- | --- |
| Code Closing CC Case (second-to-last column) | A 3-digit number | The long entity value, e.g. "00 – Undo Prior Transaction" |
| Case Closure Entity (last column) | The long entity value, or blank when the value is 0 | Swapped with the field above |

- "00 – Undo Prior Transaction" is also a wrong translation. DIAL sends 0 to mean null, not "undo prior transaction."
- Rule: whenever Case Closure Entity is 0, show blank.

**Next steps agreed**

- Sarah is building a new defect spreadsheet just for Queue, like the earlier sort sheets ("4, 4, 3, 4 and 5" as transcribed, probably SP443/445). She'll start with Case view. There are too many defects to cover in one call.
- Since Open is nearly done (probably Open and Closed together), Sarah will also retest it and report anything still wrong.
- Ranjitha will test Queue cases in detail across all views. Santosh took notes.

## Employee view: Advisor assignment number

An Advisor assignment number shows in the new Employee view but not in Legacy. The cause isn't confirmed yet. (09:41–16:32)

- **Ranjitha's question:** the same person appears under different assignment numbers. Why doesn't the Advisor one show in Legacy's Employee view?
- **Nicole:** she isn't sure why it shows in the new view. It may be the E level: a value like "minus two" can make a record inactive. It could be an old assignment number for the group that's no longer an active E level, so the dev team should check how E levels are read.
- **Client SME (Speaker D):** looked up the employee. Assignment numbers 2212-1690 and 2212-1651 (as transcribed) are both active, which points to the assignment-number structure.
- **Legacy screen walkthrough:** Legacy Employee Management has its own display rules. For example, it shows the group hold file, which other areas hide. The person appears as active with position type "long term," which is normal. This assignment number still doesn't show in their profile list.
- **Possible causes raised:** a "T type" setting, or code that hides anyone titled Advisor. An Advisor in this function is unusual but allowed.
- **Speaker E (likely Steve):** one person shouldn't hold two assignment numbers in the same group. This one has no last login, so it has never been used.
- **Speaker D:** that may not affect the employee table. The client side will check Legacy Employee Management code to see what it displays and hides.

**Checks for the dev team:** E level / active status, duplicate assignment numbers in a group, position title = Advisor, T type, and last login.

## Time Management: case codes and Excel rounding

Time records carry two case codes, and Excel exports still round hours to one decimal. Both came from the client SME (Speaker D). (16:32–19:00 approx.)

**Two case codes and subcodes per time record**

- Each record has the **current** case code/subcode and the one **at time of charge**.
- Queries cover about 90 days of time records, and codes can change in that window. Example: 101 when time was charged 90 days ago, 102 today.
- Keep this in mind if matching against DIAL is giving trouble.

**Excel export rounding (Weekly Time Verification)**

- On screen, hours show two decimals. Exported to Excel, they round to one.
- The two-decimal fix was just made for views, so apply it here while it's fresh. It should be an easy fix.
- Formal testing for Weekly Time Verification hasn't been sent out yet.

## Prior assignment number and action date

Prior Assignment Number and Prior Assignment Action Date are new in modernization, and they appear on open cases, not only Queue. Their data source still needs confirming. (11:09–12:03, 19:00–23:04 approx.)

- **Ranjitha's ask:** examples of when Prior Assignment Number is filled in, so the team can check it.
- **Sarah:** there are no Legacy examples to show. It's an enhancement. The data exists in the system but has never been displayed, and the client asked to show it as part of modernization.
- **Client SME (Speaker D):** showed a Q-menu query the client uses often. It includes both Prior Assignment Number and Prior Assignment Action Date.
- **The confusion:** Sarah's chat comment and Sri's meeting-invite update suggested these might be DIAL-only fields. The client thinks of DIAL as the queue, but the fields show up on open cases as well as queue cases.
- **Example:** a case that stayed open the whole time and was reassigned from one revenue officer to another. It never closed or returned to the queue. Prior Assignment Number is the earlier RO, and the current assignment number is the new RO.
- **Confirmed:** Ranjitha restated it as "open, assigned to one assignment number, then reassigned while still open," and Speaker D said that's exactly right.
- **Still open:** whether DIAL holds open-case data the fields could come from. It may all come from DIAL.
- **Follow-up:** Speaker D already sent the query details ("the cat," as transcribed) and will send specific example cases. Ranjitha will dig into them.

## Demo: Employee view layout and Time view descriptors

In Employee view, the Display dropdown options now sit under Layout (local build only), and Sarah approved. (23:10–26:10)

**Employee view**

- Santosh demoed it locally. The team is deploying in phases.
- Ranjitha asked for the change because the Display options looked like Layout options. Inventory levels, direct case type hours and similar fields now sit under Layout.
- It will deploy soon.
- **Sarah:** correct. Employee view has no statuses or filters (aside from querying), so it doesn't need display options. They belong under Layout.

**Time view**

- Code and descriptor fields were added, for example Case Code Current, which has a very long list of values.
- Ranjitha is still validating them.

## Activity view detail screen

Blank case details on some activities are expected, because an activity can exist without a parent case. No fix is needed. (26:10–34:06)

**Ranjitha's question**

- For one TIN, clicking through an activity opened a detail screen with case data. Activity counts (about 60–65) matched Legacy.
- But opening case details from the disposition activity showed nothing. Is something about this case causing that?

**Sarah's explanation: the "mother and babies" model**

- The activity detail screen works differently from the Case and Module detail screens.
- A case is the "mother." Its "babies" are time records, modules and activities.
- Activities are one of the few places a baby can exist without a mother. Example: no case is open, but a revenue officer receives and processes a check. That activity has no case to attach to.
- The top section of the detail screen is the mother's (case) information. With no case, it's blank. Activity view generally has no summary.
- There's no Time dropdown in Activity Management, because time has to belong to a case.
- **Activity Management detail shows only three things:** modules, name and address (the only top-section data), and the activities for that TIN.
- Ranjitha's activities looked right, and most modules were there.

**Other input**

- **Samuel:** when an activity arrives with no parent, the system creates one and labels it as created by the activity.
- **Sarah:** that's why the top section shouldn't show all that information. Legacy shows only name and address.
- **Speaker D** posted a picture of the Legacy activity detail screen in chat. Time and Summary aren't on it.
- **Sarah:** this is normal and not something to correct.

## Open questions

Four questions were left unresolved or only implied.

- Where should Prior Assignment Number and Action Date come from for open cases: DIAL or open-case data?
- Which rule hides the Advisor assignment number in Legacy (E level, duplicate assignment number in a group, position title or T type)? Should the new view match it?
- Should the new Activity detail screen be cut down to Legacy's three parts (modules, name and address, activities) with no summary section? Sarah implied yes but didn't log it as a defect.
- Is a person allowed two assignment numbers in the same group? Steve's understanding is no, but this record has two.

## Glossary

Terms as they were used in this meeting.

| Term | Meaning |
| --- | --- |
| Legacy | The current system being modernized; the reference for expected behavior |
| Open / Queue | View statuses. Queue holds cases not yet assigned to a revenue officer |
| DIAL | Source data feed the views draw from; the client associates it with the queue |
| IRM | Internal Revenue Manual |
| TIN | Taxpayer Identification Number |
| TDA / TDI / OI | Case types: Taxpayer Delinquent Account, Taxpayer Delinquency Investigation, Other Investigation |
| Combo case | A case that combines TDA and TDI |
| RO | Revenue officer |
| Assignment number | Number tying an employee to a group and position; one person can have several |
| E level | Employee record status level; some values mark an assignment as inactive |
| Literal | The text description shown with a code |
| All Views / SP443 sheets | Specs for each view's layout (All Views) and sorts (SP443) |
| Q-menu query | A Legacy query the client uses, which includes prior assignment fields |
