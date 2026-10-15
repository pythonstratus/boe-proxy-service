/**
 * Query Builder pick lists and padding (EN-1677).
 * Location: src/utils/fieldListUtils.ts  ->  import from "@/utils/fieldListUtils"
 *
 * Source: "Query Fields - Updated Field Names" spreadsheet, Query Fields - Values tab.
 * Each list is written as "code|description" lines, one per spreadsheet row, so it
 * can be checked against the sheet row by row.
 *
 * PICK_LIST_FIELDS (bottom of the lists) is one entry per spreadsheet field: its
 * Control Number, its Legacy and Modernized names, the views it has a dot in, and
 * its list. A field gets the dropdown when the query builder is in one of those
 * views and either:
 *   - the field's DB column is the confirmed `column`, or
 *   - the field's label matches one of `names` (ignoring case, spaces and punctuation).
 *
 * In development, the browser console reports any spreadsheet list that did not
 * find its field in the current view (search the console for "[pick lists]").
 *
 * Why padding exists: Legacy stores some codes right-aligned in fixed-width
 * fields (" 99a", not "99a"). The leading space is what makes them sort
 * correctly as text: a space sorts before "1", so " 99a" comes before "100a".
 * Modern sends exactly what the user picks or types, so at run time we pad
 * the value back to the stored width.
 */
import type { Field, ValueEditorType } from "react-querybuilder";

export type PickOption = { code: string; literal?: string };

/** One "code|description" per line. A line with no "|" is a code with no description. */
const parse = (block: string): PickOption[] =>
    block
        .trim()
        .split("\n")
        .map((line) => {
            const [code, ...rest] = line.split("|");
            const literal = rest.join("|").trim();
            return literal ? { code: code.trim(), literal } : { code: code.trim() };
        });

/** Same codes as `base`, with some descriptions replaced. */
const withLiterals = (base: PickOption[], changes: Record<string, string>): PickOption[] =>
    base.map((o) => (changes[o.code] ? { ...o, literal: changes[o.code] } : o));

/** Numeric codes with no description, e.g. 00 - 99. */
const numberCodes = (from: number, to: number, width = 2): PickOption[] =>
    Array.from({ length: to - from + 1 }, (_, i) => ({ code: String(from + i).padStart(width, "0") }));

/* ------------------------------------------------------------------ */
/* Lists                                                               */
/* ------------------------------------------------------------------ */

const PRIORITY_BANDS = [
    99, 100, 101, 102, 103, 104, 105, 106, 107, 108,
    201, 202, 203, 204, 205, 206, 207, 208,
    301, 302, 303, 399,
];

// 22 values: 99, 100 ... 399
const PRIORITY: PickOption[] = PRIORITY_BANDS.map((b) => ({
    code: `${b}`,
    literal: `PRIORITY LEVEL ${b}`,
}));

// 110 values: 99a ... 399e
const PRIORITY_ALPHA: PickOption[] = PRIORITY_BANDS.flatMap((b) =>
    ["a", "b", "c", "d", "e"].map((s) => ({
        code: `${b}${s}`,
        literal: `PRIORITY LEVEL ${b}${s}`,
    }))
);

const NO_YES_01 = parse(`
0|NO
1|YES
`);

const NO_YES_NY = parse(`
N|NO
Y|YES
`);

const FALSE_TRUE = parse(`
F|FALSE
T|TRUE
`);

const BOD_DIVISION = parse(`
LM|Large Business & International
SB|Small Business/Self-Employed
TE|Tax Exempt & Govt Entities
WI|Wage & Investment Income
`);

const CASE_CODE = parse(`
000|No CaseCode from ICS
101|Ind-Wage Earner
102|Ind-Self Employed
103|Ind-Combined Wage/Self
104|Ind-TFRP-Wage Earner
105|Ind-TFRP-Self Employed
106|Ind-TFRP-Combined Wage/Self
201|Corp-In Business
202|Corp-Out of Business
301|Sole Prop-In Business
302|Sole Prop-Out of Business
401|Prtnr-In Business
402|Prtnr-Out of Business
501|Estate-Decedent
502|Estate-Fiduciary
601|LLC In-Business
602|LLC Out of Business
`);

const CASE_GRADE = parse(`
00|No Grade Associated
09|Grade 09
11|Grade 11
12|Grade 12
13|Grade 13
`);

// Case Status and Module Status
const STATUS = parse(`
O|OPEN
C|CLOSED
R|RTN2QUE
Q|QUEUE
`);

const CASE_SUBCODE = parse(`
000|NATL: COMPLIANCE TDA/TDI
101|NATL: OI-INTRA-AREA ON DA
104|NATL: OI-INTER-AREA ON DA
105|NATL: FTD ALERT
106|NATL: OIC
107|NATL: TAXPAYER ADVOCATE
120|NATL: CDP RELATED OICS
201|NATL: OI-INTRA-AREA ON DR
204|NATL: OI-INTER-AREA ON DR
301|NATL: LOCAL CIP-1 (301)
302|NATL: LOCAL CIP-2 (302)
303|NATL: LOCAL CIP-3 (303)
305|NATL: LOCAL OUTREACH-1 (305)
306|NATL: LOCAL OUTREACH-2 (306)
307|NATL: LOCAL OUTREACH-3 (307)
308|NATL: LOCAL OUTREACH-4 (308)
309|NATL: ATAT
310|NATL: VOLUNTARY DISCLOSURE PRACTICE
311|NATL: OVDI/OVDP
312|NATL: INTERMEDIARY TRANSACTIONS
313|NATL: PROMOTER/PREPARER
314|NATL: NOTICE 2000-44 (SON OF BOSS)
315|NATL: VIRTUAL CURRENCY
316|NATL: ATAT FPLP CONTRACT VENDOR
317|NATL: COLLECTION STRATEGY
318|NATL: COLLECTION STRATEGY
319|NATL: REFUND SCHEMES
320|NATL: LB&I DAT
321|NATL: IRC 6707A PENALTY ASSESSMENTS
322|NATL: FATCA
323|NATL: ATAT ESTATE AND GIFT
324|NATL: CAPTIVE INSURANCE ABUSE
325|NATL: ATAT COLLECTION STRATEGY
326|NATL: ATAT COLLECTION STRATEGY
327|NATL: ATAT COLLECTION STRATEGY
328|NATL: ATAT COLLECTION STRATEGY
329|NATL: ATAT COLLECTION STRATEGY
361|NATL: MCAR
362|NATL: SOLUTN SATURDAY
500|NATL: SEIZURE
505|NATL: PAYROLL SERVICE PROVIDER CLIENTS
510|NATL: ETS OI
601|NATL: TAXPAYER NEEDS CONTACT - IMF
602|NATL: TAXPAYER NEEDS CONTACT - BMF
603|NATL: TAXPAYER NEEDS CONTACT - OTHER
604|NATL: LARGE $ ASSET CASE - IMF
605|NATL: LIEN PROJECT
900|NATL: IBTF IA/CASE PROCESSING
901|NATL: MMIA/CASE PROCESSING
902|NATL: CONT LEVY/CASE PROCESSING
903|NATL: POTENTIAL SIGNIF EQTY - FRM ACS
904|NATL: POTENTIAL SIGNIF EQTY - FRM CAMPUS
905|NATL: DEFAULTED PPIA
910|NATL: FRAUD DEVELOPMENT - FTA
912|NATL: OTHER FRAUD RELATED ASSIGNMENTS
919|NATL: IBTF RRAP/CDP
966|NATL: LOCAL CDP CASE NOT FORWARDED TO APPEALS
967|NATL: LOCAL CDP CASE FORWARDED TO APPEALS
`);

// Same codes; the sheet words a few descriptions differently in these two.
const CASE_SUBCODE_CURRENT = withLiterals(CASE_SUBCODE, {
    "362": "NATL: SOLUTN SAT",
});
const CASE_SUBCODE_TOC = withLiterals(CASE_SUBCODE, {
    "321": "NATL: IRC 6707A PEN ASSTMT",
    "324": "NATL: ATAT COLLECTION STRATEGY", // sheet says this; the other two lists say CAPTIVE INSURANCE ABUSE
    "362": "NATL: SOLUTN SAT",
});

const CASE_TYPE = parse(`
A|TDA ONLY
C|TDA&TDI (COMBO)
F|FTDALERT
I|TDI ONLY
O|INVESTIG
P|NON-IDRS
`);

// Disposition Code. 07, 16 and 17 are marked "Activity Mgmt only" in the sheet.
const DISPOSITION = parse(`
00|UNDO PRIOR TRANSACTION
01|FULL PAYMENT
02|INSTALLMENT AGREEMENT
03|CNC
04|ABATEMENT
05|PAYMENT TRACER
06|TRANSFER-INTRA-GRP
07|REMITTANCE (Activity Mgmt only)
08|OTHER
09|IDRS CLOSING
10|CREDIT TRANSFER
11|TDI CLOSURE
12|OFFER IN COMPROMISE
13|LITIGATION
14|ERRONEOUSLY CREATED
15|REASSIGN WITHIN GROUP
16|RETURN SECURED (Activity Mgmt only)
17|RETURN SECURED MANUAL (Activity Mgmt only)
21|FTD RETURN SECURED
22|FTD TP IN COMPLIANCE
23|FTD NO DEP REQUIRED
24|FTD SPORADIC/SEASONAL
25|FTD NOW IN COMPLIANCE
26|FTD TDA/TDI RECEIVED
27|FTD CLOSED NEW ALERT
31|CIP CLOSURE
32|OIC-ACCEPTED
33|OIC-WITHDRAWN/TERMINATED
34|OIC-REJECTED
35|OIC-TRANSFER
36|OIC-RETURNED
37|CWA
38|POLICY
39|DIRECTED WITHHOLDING LETTER
40|CWA W/EXAM REFERRAL
41|POLICY W/EXAM REFERRAL
42|DWL W/EXAM REFERRAL
70|TRANSFER TO QUEUE
77|ICS-UNDO
80|XFER - CASE PROC/TECH SVC/ASFR
88|MISMATCH
99|CLEANUP OPEN->CLSD
`);

// Module view and Clsd ENTITY Method (Case): everything except the Activity-only codes (39 values).
const DISPOSITION_NOT_ACTIVITY = DISPOSITION.filter((o) => !/Activity Mgmt/i.test(o.literal ?? ""));

const FTL_CODE = parse(`
0|Await Determination
1|Extend Determination
2|Request FTL
3|Do Not File FTL
4|Filed
5|Lien Deferred
6|Approval Requested
7|Lien Not Required
9|N/A
`);

// The name itself is the value.
const PROGNAME1 = parse(`
FRAUD
FRAUD POST AI
MCAR CANADA INBOUND
MCAR CANADA OUTBOUND
MCAR DENMARK INBOUND
MCAR DENMARK OUTBOUND
MCAR FRANCE INBOUND
MCAR FRANCE OUTBOUND
MCAR SWEDEN INBOUND
MCAR SWEDEN OUTBOUND
MCAR NETHERLANDS INBOUND
MCAR NETHERLANDS OUTBOUND
ADV RESTITUTION
CF THIRD PARTY PAYER
FC MARIJUANA
FC GHOST EMPLOYER
FC SURROUND SOUND
FC ULTRA HIDEF
FC FATCA PROJECT
FC RETURN PREPARER
FC SECURE MESSAGING
FC SECURE MESSAGING DECLINED/DENIED
FC EGREGIOUS EMPLOYMENT TAX
FC VIRTUAL CURRENCY
`);

// Same as Progname1 without the two FC SECURE MESSAGING rows (as in the sheet).
const PROGNAME2 = PROGNAME1.filter((o) => !o.code.startsWith("FC SECURE MESSAGING"));

const CONTACT_CODE = parse(`
F|FIELD
O|OFFICE
T|TELE
C|C-LETTER
X|NONE
`);

const CSED_ASED = parse(`
0|OKAY
1|CSED
2|ASED
3|EITHER OR BOTH (CSED&ASED)
`);

const EMPLOYEE_TYPE = parse(`
C|CLERICAL
D|AREA
H|HOLDFILE
M|MANAGER
P|PARAPROF
R|REV OFF
S|SPF
T|TXRESREP
`);

const DISASTER_VICTIM = parse(`
0|NO SIGNIFICANCE
1|-S FREEZE ON - TC 971 AC 688 POSTED
2|-O FREEZE ON - TC 971 AC 86/87 POSTED
3|BOTH -S AND -O FREEZE ON, TC 971 AC 86/87 & 688 POSTED
4|HISTORICAL, -S FREEZE AND/OR -O FREEZE WERE ON IN PAST
`);

const QUEUE_PICK = parse(`
0|NONE
1|HOLD
2|SKIPDATE
3|SELECTED
4|PENDING
5|ACCEPTED
6|REJECTED
7|NONIDRS
`);

const EMPLOYEE_GRADE = parse(`
00|No Grade Associated
04|Grade 04
05|Grade 05
06|Grade 06
07|Grade 07
08|Grade 08
09|Grade 09
11|Grade 11
12|Grade 12
13|Grade 13
`);

const ICS_ACCESS = parse(`
1|MANAGER
2|SECRETARY/CLERK
3|ACTING MANAGER / SPF ADVISOR
4|RO AIDE /TE/OCR/TSR/TSS
5|RO/REV REP/STAFF/ISP
6|TERRITORY
8|AREA DIRECTOR
9|SYSTEM ADMINISTRATOR
A|LIMITED ACCESS *
B|REGION-READ ONLY
C|ICS QUALITY ANALYST
`);

// "Indicator - ASED (Module)" in the sheet. The values read like a TFRP determination; see notes.
const ASED_MODULE = parse(`
0|NO TFR DETERMINATION
1|TFR ASSESSED
2|NO RESPONSIBLE OFFICER
3|NO COLLECTION POTENTIAL
4|TFR TRUST FUNDS PAID
5|TFR NOT APPLICABLE
`);

// Indicator - Case Age and Indicator - Module Age
const AGE_INDICATOR = parse(`
C|CURRENT
O|OVERAGE
P|POTENTIAL AGING
`);

const CAUTION = parse(`
0|NO CAUTIONS
1|PDT POTENTIALLY DANGEROUS TAXPAYER
2|CAUTION
`);

const CSED_MODULE = parse(`
0|CSED VALID
1|CSED REVIEWED - OKAY
`);

const ENTITY_ICS = parse(`
0|NOT ICS
1|ICS
`);

const FATCA = parse(`
P|PRIMARY
S|SECONDARY
B|BOTH
1|YES (Queue)
`);

// IDRS Pyramid (Case) and (Module)
const IDRS_PYRAMID = parse(`
0|NO
1|PYRAMIDING
4|PYRAMIDING RO
9|NO FORM941
`);

const IRS_EMPLOYEE = parse(`
0|NO
1|PRIMARY
2|SECONDARY
3|BOTH
`);

const LARGE_DOLLAR = parse(`
F|FALSE
T|TRUE - LARGE $
`);

const LFI_CASE = parse(`
0|OPEN TDA W/O LFI
1|LFI INPUT ON ALL TDAS
`);

const LFI_MODULE = parse(`
0|NO
1|FILED IDRS
2|REFILE IDRS
4|FILED MF
5|REFILE MF
`);

const LLC = parse(`
0|Not an LLC
M|Multi Member
S|Single Member
`);

const POA = parse(`
0|NO REPRESENTATIVE
1|ONE POA RECEIVES NOTICES
2|TWO POAS RECEIVE NOTICES
3|ONE POA RECEIVES NOTICES/REFUND
4|TWO POAS RECEIVE NOTICES/REFUND
5|OTHER AUTHORIZATION
6|RESERVED
7|ONE POA RECEIVES REFUND
8|BLIND TRUST
9|ICS ONLY W/POA
`);

const REPEATER = parse(`
F|FALSE
P|AT RISK
T|REPEATER
`);

const INV_ADJUST_REASON = parse(`
AGM|ACTING GROUP MANAGER
COLL|COLLATERAL DUTIES
DOUT|DETAILED-OUT
DARE|DISASTER AREA
EEOC|EEO COUNSELOR
EEOI|EEO INVESTIGATOR
ELV|EXTENDED LEAVE
FSS|FILING SEASON SUPPORT
GRPR|GROUP RESOURCE PERSON
J/L|EMPLOYEE JOINS/LEAVES GROUP
MED|MEDICAL REASONS
NTEU|NTEU- UNION DUTIES
OJI|ON THE JOB INSTRUCTOR (COACH)
PT|PART TIME EMPLOYEE
`);

const POSITION_TYPE = parse(`
B|DISBANDED OR BLOCK
D|DETAILED OUT
E|ENTITY COORDINATOR
F|FRAUD REFERRAL SPECIALIST
I|TRAINEE
L|LONG-TERM ACTING MANAGER
M|MANUALLY MONITORED INSTALLMENT AGREEMENTS
N|NO ADDITIONAL DUTIES - NORMAL
O|OIC SPECIALIST
Q|CQMS
S|SPECIAL COMPLIANCE SPECIALIST
T|OJI COACH
U|COLLATERAL DUTIES
V|VACANT
W|WI
Y|NO INVENTORY
`);

const ACTIVITY_REPORT = parse(`
D|DISPOSITION(DISP)
R|RETURN/REMITTANCE(RRN/RMIT)
T|TRANSFER
`);

const TIME_CODE = parse(`
102|NOTICE-ACS/UNASSIGNED
106|OIC
107|TAXPAYER ADVOCATE
109|SUMMONS
120|CDP RELATED OIC
202|NOTICE-ACS/UNASSIGNED
305|LOCAL OUTREACH-1 (305)
306|LOCAL OUTREACH-2 (306)
307|LOCAL OUTREACH-3 (307)
308|LOCAL OUTREACH-4 (308)
309|ATAT
310|VOLUNTARY DISCLOSURE PRACTICE
311|OVDI/OVDP
312|INTERMEDIARY TRANSACTIONS
313|PROMOTER/PREPARER
314|NOTICE 2000-44
315|VIRTUAL CURRENCY
316|ATAT FPLP CONTRACT VENDOR
317|COLLECTION STRATEGY
318|COLLECTION STRATEGY
319|REFUND SCHEMES
320|LB&I DAT
321|IRC 6707A PEN ASSTMT
322|FATCA
323|ATAT ESTATE AND GIFT
324|CAPTIVE INSURANCE ABUSE
325|ATAT COLLECTION STRATEGY
326|ATAT COLLECTION STRATEGY
327|ATAT COLLECTION STRATEGY
328|ATAT COLLECTION STRATEGY
329|ATAT COLLECTION STRATEGY
360|APPEALS
361|MCAR
362|SOLUTN SATURDAY
401|COLLATERAL DUTIES
402|ADP SUPPORT
403|ICS/ENTITY SUPPORT
404|COORDINATOR DUTIES
502|ANALYSIS PERFECTION
504|FILES MAINTENANCE
505|CLERICAL
601|TRAINING - CPE
602|TRAINING - RO PHASE
603|TRAINING - OJI
604|TRAINING - INSTRUCTOR ASSN
605|TRAINING - ALL OTHER
610|MANAGEMENT
611|ADMINISTRATIVE
613|LEAVE
614|MANAGEMENT CASE DIRECT
615|ECM MIGRATION
619|DETAIL IN
620|DETAIL OUT
621|FIELD TIME
622|FLEXIPLACE
623|COMPUTER DOWNTIME
624|IDENTITY THEFT
625|PASSPORT LEGISLATION
626|OIC NEH-ETA CHECK SHEET TIME
62A|DETAILED OUT-WALK-IN
62B|DETAILED OUT-ACS/TOLL-FREE
62C|DETAILED OUT-TAXPAYER ADVOCATE
62E|DETAILED OUT-STKHLDR
62F|DETAILED OUT-DISASTER RELIEF
62G|DETAILED OUT-OTHER
710|CREDIT/COMP HRS EARNED
711|CREDIT/COMP HRS USED
720|RELIGIOUS COMP EARNED
721|RELIGIOUS COMP USED
730|OVERTIME WORKED
740|LEAVE WITHOUT PAY
750|HOLIDAY
760|NON-WORK DAY
770|ADMIN LEAVE
809|MISCELLANEOUS DIRECT
810|ASSIST ANOTHER RO
`);

const TIME_CODE_DEFINER = parse(`
A|INFO ONLY - HOURS PLUS
C|NON-CASE DIRECT
D|SUBCODE DIRECTED
E|DETAILED OUT
G|MANAGEMENT DIRECT
I|INFO ONLY
M|MISC DIRECT
N|NON - DIRECT
O|OVERHEAD
R|TRAINING
S|INFO ONLY - HOURS MINUS
T|TIN BASED
U|UNASSIGNED
`);

const TIN_FILE_SOURCE = parse(`
1|IMF
2|BMF
3|EPMF
4|IRAF
6|NMF
9|CAF
`);

const TIN_TYPE = parse(`
0|SSN
1|SSN** INVALID
2|EIN
`);

// The name itself is the value. The sheet shows each with a trailing " -", left off here; see notes.
const TITLE = parse(`
ACT. TERRITORY MANAGER
ACTING MANAGER
ADVISOR
ADVISORY GROUP MANAGER
ANALYST
AREA DIRECTOR
BANKRUPTCY SPECIALIST
CAC
CADRE MANAGER
CASE PROC. SUPPORT MGR
CLERK
COLLECTION SUPPORT MGR
GRP/TERRITORY DESIG.
ICS/ENT QUALITY ANALYST
INSOLVENCY MANAGER
INSOLVENCY SUPPORT MGR
IQA MANAGER
LIMITED ACCESS
MANAGER
NATIONAL ANALYST - SB/SE
NATIONAL ANALYST-WI
POLICY ANALYST
PROP AP & LIQ SPEC-PALS
REVENUE OFFICER
REVENUE REPRESENTATIVE
RO AIDE
RTO
SECRETARY-AREA
SECRETARY-GROUP
SECRETARY-TERRITORY
STAFF ASSISTANT
TAX EXAMINER
TAX RESOLUTION REP
TAXPAYER SERVICE SPEC.
TECH SERVICES GRP MGR
TER CASE PROC SUPRT MGR
TER INSOL SUPRT MGR
TER TECH SERVICES MGR
TERRITORY MANAGER
`);

const TOUR_OF_DUTY = parse(`
1|REG
2|5/4/9
3|4/10
4|PT
5|MAXI
`);

const MODULE_TYPE = parse(`
A|TDA-IDRS
B|ICS ONLY TDA-MODULE PRE-ASSESSED
C|ICS ONLY TDA-MODULE PROMPT ASSD
D|ICS ONLY TDA-MODULE IN NOTICE STATUS
E|ICS ONLY TDA-MODULE WITH PRIOR TC530
F|ICS ONLY TDI-MODULE IN NOTICE STATUS
G|ICS ONLY TDI-OTHER MODULE
I|TDI-IDRS
N|OI-IN
O|OI-OUT
R|CIP
T|FTD
Y|OIC
`);

const TDI_SELECT = numberCodes(0, 99);   // "Pick list of numeric codes 00 - 99"
const MFT = numberCodes(0, 99);          // "Pick list of MFTs 00 - 99"
const PERCENT = numberCodes(0, 100);     // "Pick list of percentages 00 - 100"

/** Every list by name. */
export const PICK_LISTS = {
    PRIORITY, PRIORITY_ALPHA, NO_YES_01, NO_YES_NY, FALSE_TRUE,
    BOD_DIVISION, CASE_CODE, CASE_GRADE, STATUS, CASE_SUBCODE, CASE_SUBCODE_CURRENT, CASE_SUBCODE_TOC,
    CASE_TYPE, DISPOSITION, DISPOSITION_NOT_ACTIVITY, FTL_CODE, PROGNAME1, PROGNAME2, TDI_SELECT,
    CONTACT_CODE, CSED_ASED, EMPLOYEE_TYPE, DISASTER_VICTIM, QUEUE_PICK, EMPLOYEE_GRADE, ICS_ACCESS,
    ASED_MODULE, AGE_INDICATOR, CAUTION, CSED_MODULE, ENTITY_ICS, FATCA, IDRS_PYRAMID, IRS_EMPLOYEE,
    LARGE_DOLLAR, LFI_CASE, LFI_MODULE, LLC, POA, REPEATER, PERCENT, INV_ADJUST_REASON, MFT,
    POSITION_TYPE, ACTIVITY_REPORT, TIME_CODE, TIME_CODE_DEFINER, TIN_FILE_SOURCE, TIN_TYPE, TITLE,
    TOUR_OF_DUTY, MODULE_TYPE,
};

/* ------------------------------------------------------------------ */
/* Where each list shows up                                            */
/* ------------------------------------------------------------------ */

/** Priority lists by DB column, in every view (shipped first with EN-1677). */
const ALL_VIEWS: Record<string, PickOption[]> = {
    ARANK: PRIORITY_ALPHA, // VARCHAR2(4), stored " 99a"
    RISK: PRIORITY,        // NUMBER
};

// Spreadsheet view columns -> query builder view names (currentNav, upper-cased).
const INV = "CASE";
const MOD = "MODULE";
const ACT = "ACTIVITY";
const TIME = "TIME";
const EMP = "EMPLOYEE";
const GM = ["CA", "CAINTL"]; // GM Case Assignment

export type PickListField = {
    control?: number;  // spreadsheet Control Number
    names: string[];   // Legacy Field and Modernized Field Name, matched against the field label
    views: string[];   // views with a dot in the sheet
    list: PickOption[];
    column?: string;   // DB column confirmed in TOAD, matched against the field name
};

export const PICK_LIST_FIELDS: PickListField[] = [
    { control: 10, names: ["BOD - Division Code"], views: [INV, MOD, ACT, TIME], list: BOD_DIVISION, column: "BODCD" },
    { control: 15, names: ["Case Code"], views: [INV, MOD, ACT], list: CASE_CODE, column: "CASECODE" },
    { control: 33, names: ["Case Code - Current"], views: [TIME], list: CASE_CODE },
    { control: 51, names: ["Case Code - Time of Charge"], views: [TIME], list: CASE_CODE },
    { control: 69, names: ["Case Grade"], views: [INV, MOD, ...GM], list: CASE_GRADE, column: "GRADE" },
    { control: 75, names: ["Case Grade - Current"], views: [TIME], list: CASE_GRADE },
    { control: 81, names: ["Case Grade - Time of Charge"], views: [TIME], list: CASE_GRADE },
    { control: 87, names: ["Case Status"], views: [MOD, ACT], list: STATUS }, // Inventory is "F6-Sort Only"
    { control: 92, names: ["Case SubCode"], views: [INV, MOD, ACT], list: CASE_SUBCODE, column: "SUBCODE" },
    { control: 151, names: ["Case SubCode - Current"], views: [TIME], list: CASE_SUBCODE_CURRENT },
    { control: 210, names: ["Case SubCode - Time of Charge"], views: [TIME], list: CASE_SUBCODE_TOC },
    { control: 269, names: ["Case Type (TDA/TDI)"], views: [INV, MOD, ...GM], list: CASE_TYPE, column: "CASEIND" },
    { control: 276, names: ["Case Type (TDA/TDI) - Current"], views: [TIME], list: CASE_TYPE },
    { control: 283, names: ["Case Type (TDA/TDI) - Time of Charge"], views: [TIME], list: CASE_TYPE },
    { control: 292, names: ["Clsd ENTITY Method (Case)"], views: [INV], list: DISPOSITION_NOT_ACTIVITY, column: "DISPCD" },
    { control: 337, names: ["Code - FTL"], views: [MOD], list: FTL_CODE },
    { control: 349, names: ["Code - Progname1"], views: [INV, MOD, TIME], list: PROGNAME1, column: "PRGNAME1" },
    { control: 374, names: ["Code - Progname2"], views: [INV, MOD, TIME], list: PROGNAME2, column: "PRGNAME2" },
    { control: 398, names: ["Code - TDI Select"], views: [INV, MOD], list: TDI_SELECT, column: "CCNIPSELECTCD" },
    { control: 400, names: ["Contact Code"], views: [TIME], list: CONTACT_CODE },
    { control: 412, names: ["CSED/ASED"], views: [INV], list: CSED_ASED },
    { control: 451, names: ["Disposition Code"], views: [MOD], list: DISPOSITION_NOT_ACTIVITY },
    { control: 451, names: ["Disposition Code"], views: [ACT], list: DISPOSITION },
    { control: 495, names: ["Employee Type"], views: [EMP], list: EMPLOYEE_TYPE },
    { control: 504, names: ["Flag - Disaster Victim", "Code - Disaster Victim"], views: [INV], list: DISASTER_VICTIM },
    { control: 510, names: ["Flag - Queue Pick"], views: [INV, ...GM], list: QUEUE_PICK },
    { control: 521, names: ["Grade - Employee", "Employee Grade"], views: [EMP], list: EMPLOYEE_GRADE },
    { control: 537, names: ["ICS Access"], views: [EMP], list: ICS_ACCESS },
    { control: 552, names: ["Indicator - ASED (Module)"], views: [MOD], list: ASED_MODULE },
    { control: 559, names: ["Indicator - Case Age"], views: [INV], list: AGE_INDICATOR },
    { control: 563, names: ["Indicator - Caution (Case)"], views: [INV, MOD], list: CAUTION },
    { control: 567, names: ["Indicator - CSED (Module)"], views: [MOD], list: CSED_MODULE },
    { control: 569, names: ["Indicator - Entity (1=ICS)"], views: [ACT], list: ENTITY_ICS },
    { control: 573, names: ["Indicator - Ent Pyramid", "Indicator - Pyramid"], views: [INV], list: NO_YES_01 },
    { control: 576, names: ["Indicator - Erroneous Refund"], views: [MOD], list: NO_YES_NY },
    { control: 579, names: ["Indicator - FATCA"], views: [INV, MOD], list: FATCA },
    { control: 584, names: ["Indicator - Federal Contractor"], views: [INV], list: NO_YES_01 },
    { control: 587, names: ["Indicator - Federal Employee", "Indicatory - Federal Employee"], views: [INV], list: NO_YES_01 },
    { control: 592, names: ["Indicator - IDRS Pyramid (Case)"], views: [INV, MOD], list: IDRS_PYRAMID },
    { control: 597, names: ["Indicator - IDRS Pyramid (Module)"], views: [MOD], list: IDRS_PYRAMID },
    { control: 603, names: ["Indicator - Innocent Spouse"], views: [INV, MOD], list: NO_YES_NY },
    { control: 606, names: ["Indicator - IRS Employee"], views: [INV], list: IRS_EMPLOYEE },
    { control: 611, names: ["Indicator - L903"], views: [INV], list: NO_YES_01 },
    { control: 614, names: ["Indicator - Large $"], views: [INV, ACT], list: LARGE_DOLLAR },
    { control: 617, names: ["Indicator - LFI (Case)", "Indicator - Lien Filing"], views: [INV], list: LFI_CASE },
    { control: 620, names: ["Indicator - LFI (Module)"], views: [MOD], list: LFI_MODULE },
    { control: 626, names: ["Indicator - LLCIND", "Indicator - LLC"], views: [INV], list: LLC },
    { control: 630, names: ["Indicator - Module Age"], views: [MOD], list: AGE_INDICATOR },
    { control: 634, names: ["Indicator - POA"], views: [MOD], list: POA },
    { control: 645, names: ["Indicator - Repeater"], views: [INV], list: REPEATER },
    { control: 649, names: ["Indicator - RTNSEC", "Tax Return"], views: [ACT], list: FALSE_TRUE },
    { control: 652, names: ["Indicator - Theft"], views: [INV], list: NO_YES_01 },
    { control: 656, names: ["INV Adjust Percent", "Inventory Adjustment"], views: [EMP], list: PERCENT },
    { control: 657, names: ["INV Adjust Reason", "Inventory Adjustment Reason"], views: [EMP], list: INV_ADJUST_REASON },
    { control: 674, names: ["MFT"], views: [MOD, ACT], list: MFT },
    { control: 675, names: ["Module Status"], views: [MOD], list: STATUS },
    { control: 688, names: ["Position Type"], views: [EMP], list: POSITION_TYPE },
    // Same 22 values as Priority. Names from the ticket; views assumed to match the other "- Current" fields.
    { names: ["Priority Code - Current", "Priority Level Code - Current"], views: [TIME], list: PRIORITY },
    { names: ["Priority Code - Time of Charge", "Priority Level Code - Time of Charge"], views: [TIME], list: PRIORITY },
    { control: 885, names: ["Report - (Disp Ret/Remit-Transfers)", "Activity Report"], views: [ACT], list: ACTIVITY_REPORT },
    { control: 892, names: ["Time Code"], views: [TIME], list: TIME_CODE },
    { control: 969, names: ["Time Code - Definer"], views: [TIME], list: TIME_CODE_DEFINER },
    { control: 985, names: ["TIN File Source"], views: [INV, MOD, ACT, TIME, ...GM], list: TIN_FILE_SOURCE },
    { control: 992, names: ["TIN Type"], views: [INV, MOD, ACT, TIME], list: TIN_TYPE },
    { control: 996, names: ["Title"], views: [EMP], list: TITLE },
    { control: 1040, names: ["Tour of Duty (TOD)", "Tour of Duty"], views: [TIME, EMP], list: TOUR_OF_DUTY },
    { control: 1046, names: ["Type - TDA, TDI, Etc (Module)"], views: [MOD, ACT], list: MODULE_TYPE },
    // Left out on purpose: Code - BOD Client and Indicator - Income AGI/TPI (no values in the sheet),
    // Indicator - 941 and Indicator - HINF (one value, not marked "Pick list available"),
    // INV Warning and Time Code Description ("F6-Sort Only").
];

/**
 * Stored width of each right-aligned column.
 * Explicit rather than DbColumn.columnSize, because a view column can be
 * wider than the code stored in it. Only add columns confirmed padded in
 * the database: padding a left-aligned column (a name) breaks searches on it.
 */
export const PADDED_WIDTH: Record<string, number> = {
    ARANK: 4,
};

// Backend operator names.
const EXACT_OPERATORS = new Set(["=", "<>", "IN", "NOT IN"]);
const LIKE_OPERATORS = new Set(["LIKE", "NOT LIKE"]);

/* ------------------------------------------------------------------ */
/* Matching                                                            */
/* ------------------------------------------------------------------ */

/** "Indicator – LFI (Case)" and "indicator-lfi case" both become "indicatorlficase". */
const norm = (s: unknown): string => String(s ?? "").toLowerCase().replace(/[^a-z0-9]/g, "");

const NAME_KEYS = new Map(PICK_LIST_FIELDS.map((d) => [d, d.names.map(norm)]));

const matches = (d: PickListField, field: Field): boolean =>
    d.column === field.name || (NAME_KEYS.get(d) ?? []).includes(norm(field.label));

const findList = (field: Field, view: string): PickOption[] | undefined =>
    ALL_VIEWS[field.name] ??
    PICK_LIST_FIELDS.find((d) => d.views.includes(view) && matches(d, field))?.list;

/** Lists attached at run time, by column, so saved queries can be matched when they load. */
const attachedByColumn = new Map<string, Set<PickOption[]>>();

const optionLabel = (o: PickOption): string => (o.literal ? `${o.code} – ${o.literal}` : o.code);

/** Same code, ignoring case; numeric codes also match without leading zeros ("9" = "09"). */
const sameCode = (code: string, typed: string): boolean =>
    code.toLowerCase() === typed.toLowerCase() ||
    (/^\d+$/.test(code) && /^\d+$/.test(typed) && Number(code) === Number(typed));

/* ------------------------------------------------------------------ */
/* Development-only report: which spreadsheet lists found no field     */
/* ------------------------------------------------------------------ */

const DEV = process.env.NODE_ENV !== "production";
const pending = new Map<string, { field: Field; listed: boolean }[]>();
const reported = new Set<string>();

const reportCoverage = (view: string): void => {
    const seen = pending.get(view) ?? [];
    pending.delete(view);
    reported.add(view);
    const expected = PICK_LIST_FIELDS.filter((d) => d.views.includes(view));
    if (!expected.length) {
        console.warn(`[pick lists] view "${view}": no spreadsheet lists use this view name. ` +
            `Expected one of CASE, MODULE, ACTIVITY, TIME, EMPLOYEE, CA, CAINTL.`);
        return;
    }
    const missing = expected.filter((d) => !seen.some(({ field }) => matches(d, field)));
    console.info(`[pick lists] view "${view}": ${expected.length - missing.length} of ${expected.length} spreadsheet lists attached`);
    if (!missing.length) return;
    console.table(missing.map((d) => ({ control: d.control ?? "", sheetField: d.names.join(" / ") })));
    console.table(seen.filter((s) => !s.listed).map(({ field }) => ({ column: field.name, label: field.label })));
};

const noteForReport = (view: string, field: Field, listed: boolean): void => {
    if (!DEV || reported.has(view)) return;
    let seen = pending.get(view);
    if (!seen) {
        seen = [];
        pending.set(view, seen);
        queueMicrotask(() => reportCoverage(view)); // runs once every field of this view has been mapped
    }
    seen.push({ field, listed });
};

/* ------------------------------------------------------------------ */
/* Helpers used by QueryBuilderHelper.ts                               */
/* ------------------------------------------------------------------ */

/** Adds a dropdown to a field when the spreadsheet has a pick list for it in this view. */
export const withPickList = (field: Field, view: string): Field => {
    const viewName = String(view ?? "").toUpperCase();
    const list = findList(field, viewName);
    noteForReport(viewName, field, !!list);
    if (!list) return field;

    const known = attachedByColumn.get(field.name) ?? new Set<PickOption[]>();
    known.add(list);
    attachedByColumn.set(field.name, known);

    return {
        ...field,
        values: list.map((o) => ({ name: o.code, label: optionLabel(o) })),
        valueEditorType: (op: string): ValueEditorType =>
            op === "in" || op === "notIn"
                ? "multiselect"
                : op === "contains" || op === "doesNotContain"
                    ? "text"
                    : "select",
    };
};

/** Run time only: pads each value back to the stored width. */
export const padForRun = (field: string, backendOp: string, value: string): string => {
    const width = PADDED_WIDTH[field];
    if (!width) return value;
    const op = String(backendOp ?? "").toUpperCase();
    if (LIKE_OPERATORS.has(op)) {
        // No % or _ typed: treat it like Equals, so "99a" finds " 99a" and not "399a".
        // With wildcards, the user's pattern is sent exactly as typed.
        const t = value.trim();
        return !t || /[%_]/.test(t) ? value : t.padStart(width, " ");
    }
    if (!EXACT_OPERATORS.has(op)) return value;
    return value
        .split(",")
        .map((v) => {
            const t = v.trim();
            return t ? t.padStart(width, " ") : t;
        })
        .join(",");
};

/** Saved-query load: maps " 99a", "99A" or "9" back to the option code. Leaves anything else as it was. */
export const matchPickListValue = (field: string, value: unknown): unknown => {
    if (typeof value !== "string") return value;
    const lists = [
        ALL_VIEWS[field],
        ...(attachedByColumn.get(field) ?? []),
        ...PICK_LIST_FIELDS.filter((d) => d.column === field).map((d) => d.list),
    ].filter((l): l is PickOption[] => !!l);
    if (!lists.length) return value;
    return value
        .split(",")
        .map((v) => {
            for (const list of lists) {
                const hit = list.find((o) => sameCode(o.code, v.trim()));
                if (hit) return hit.code;
            }
            return v;
        })
        .join(",");
};
