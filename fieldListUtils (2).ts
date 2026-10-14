/**
 * Query Builder pick lists and padding (EN-1677).
 * Location: src/utils/fieldListUtils.ts  ->  import from "@/utils/fieldListUtils"
 *
 * Source: "Query Fields - Updated Field Names" spreadsheet, Query Fields - Values tab.
 * Each list is written as "code|description" lines, one per spreadsheet row,
 * so it can be checked against the sheet row by row. Comments give the
 * spreadsheet control number.
 *
 * Lists are attached per view and per database column, because the sheet
 * recommends each list only for certain views.
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

// Control 10. Inventory, Module, Activity, Time
const BOD_DIVISION = parse(`
LM|Large Business & International
SB|Small Business/Self-Employed
TE|Tax Exempt & Govt Entities
WI|Wage & Investment Income
`);

// Controls 15, 33, 51. Case Code, Case Code - Current, Case Code - Time of Charge
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

// Controls 69, 75, 81. Case Grade, - Current, - Time of Charge
const CASE_GRADE = parse(`
00|No Grade Associated
09|Grade 09
11|Grade 11
12|Grade 12
13|Grade 13
`);

// Control 87. Module, Activity. (Inventory is "F6-Sort Only", so no dropdown there.)
const CASE_STATUS = parse(`
O|OPEN
C|CLOSED
R|RTN2QUE
Q|QUEUE
`);

// Control 92. Case SubCode. Inventory, Module, Activity
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

// Control 151. Case SubCode - Current. Time. Same codes; one description differs in the sheet.
const CASE_SUBCODE_CURRENT = withLiterals(CASE_SUBCODE, {
    "362": "NATL: SOLUTN SAT",
});

// Control 210. Case SubCode - Time of Charge. Time. Same codes; three descriptions differ in the sheet.
const CASE_SUBCODE_TOC = withLiterals(CASE_SUBCODE, {
    "321": "NATL: IRC 6707A PEN ASSTMT",
    "324": "NATL: ATAT COLLECTION STRATEGY", // sheet says this; the other two lists say CAPTIVE INSURANCE ABUSE
    "362": "NATL: SOLUTN SAT",
});

// Controls 269, 276, 283. Case Type (TDA/TDI), - Current, - Time of Charge
const CASE_TYPE = parse(`
A|TDA ONLY
C|TDA&TDI (COMBO)
F|FTDALERT
I|TDI ONLY
O|INVESTIG
P|NON-IDRS
`);

// Control 292. Clsd ENTITY Method (Case). Inventory only
const CLOSED_METHOD = parse(`
00|UNDO PRIOR TRANSACTION
01|FULL PAYMENT
02|INSTALLMENT AGREEMENT
03|CNC
04|ABATEMENT
05|PAYMENT TRACER
06|TRANSFER-INTRA-GRP
08|OTHER
09|IDRS CLOSING
10|CREDIT TRANSFER
11|TDI CLOSURE
12|OFFER IN COMPROMISE
13|LITIGATION
14|ERRONEOUSLY CREATED
15|REASSIGN WITHIN GROUP
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

// Control 337. Code - FTL. Module only
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

// Control 349. Code - Progname1. Inventory, Module, Time. The name itself is the value.
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

// Control 374. Code - Progname2. Same as Progname1 without the two FC SECURE MESSAGING rows (as in the sheet).
const PROGNAME2 = PROGNAME1.filter((o) => !o.code.startsWith("FC SECURE MESSAGING"));

// Control 398. Code - TDI Select. Inventory, Module. "Pick list of numeric codes 00 - 99"
const TDI_SELECT: PickOption[] = Array.from({ length: 100 }, (_, i) => ({
    code: String(i).padStart(2, "0"),
}));

// Control 400. Contact Code. Time only
const CONTACT_CODE = parse(`
F|FIELD
O|OFFICE
T|TELE
C|C-LETTER
X|NONE
`);

// Control 412. CSED/ASED. Inventory only
const CSED_ASED = parse(`
0|OKAY
1|CSED
2|ASED
3|EITHER OR BOTH (CSED&ASED)
`);

/** Every list by name, including ones not wired to a view yet. */
export const PICK_LISTS = {
    PRIORITY,
    PRIORITY_ALPHA,
    BOD_DIVISION,
    CASE_CODE,
    CASE_GRADE,
    CASE_STATUS,
    CASE_SUBCODE,
    CASE_SUBCODE_CURRENT,
    CASE_SUBCODE_TOC,
    CASE_TYPE,
    CLOSED_METHOD,
    FTL_CODE,
    PROGNAME1,
    PROGNAME2,
    TDI_SELECT,
    CONTACT_CODE,
    CSED_ASED,
};

/* ------------------------------------------------------------------ */
/* Where each list shows up                                            */
/* ------------------------------------------------------------------ */

type ColumnLists = Record<string, PickOption[]>;

/** Shipped first with EN-1677. Applies in every view that has these columns. */
const ALL_VIEWS: ColumnLists = {
    ARANK: PRIORITY_ALPHA, // VARCHAR2(4), stored " 99a"
    RISK: PRIORITY,        // NUMBER
};

/**
 * Spreadsheet view columns -> view names used by the query builder:
 * Inventory = CASE, Module = MODULE, Activity = ACTIVITY, Time = TIME,
 * Employee = EMPLOYEE, GM Case Assignment = CA and CAINTL.
 */
const GM_CASE_ASSIGNMENT: ColumnLists = {
    GRADE: CASE_GRADE,
    CASEIND: CASE_TYPE, // confirm: Case Type (TDA/TDI)
};

export const VIEW_PICK_LISTS: Record<string, ColumnLists> = {
    // EVIEW_MV
    CASE: {
        BODCD: BOD_DIVISION,
        CASECODE: CASE_CODE,
        GRADE: CASE_GRADE,
        SUBCODE: CASE_SUBCODE,
        CASEIND: CASE_TYPE,        // confirm: Case Type (TDA/TDI)
        DISPCD: CLOSED_METHOD,     // confirm: Clsd ENTITY Method (Case)
        PRGNAME1: PROGNAME1,
        PRGNAME2: PROGNAME2,
        CCNIPSELECTCD: TDI_SELECT, // confirm: Code - TDI Select
        // TODO CSED_ASED: no matching EVIEW_MV column found yet
        // TODO Code - BOD Client (BODCLCD?): no values in the sheet (yellow row)
    },
    CA: GM_CASE_ASSIGNMENT,
    CAINTL: GM_CASE_ASSIGNMENT,

    // TODO MODULE (MVIEW_MV): BOD_DIVISION, CASE_CODE, CASE_GRADE, CASE_STATUS, CASE_SUBCODE,
    //      CASE_TYPE, FTL_CODE, PROGNAME1, PROGNAME2, TDI_SELECT
    // TODO ACTIVITY (AVIEW_MV): BOD_DIVISION, CASE_CODE, CASE_STATUS, CASE_SUBCODE
    // TODO TIME (TVIEW_MV): BOD_DIVISION, CASE_CODE (Current and TOC), CASE_GRADE (Current and TOC),
    //      CASE_SUBCODE_CURRENT, CASE_SUBCODE_TOC, CASE_TYPE (Current and TOC),
    //      PROGNAME1, PROGNAME2, CONTACT_CODE
};

/**
 * Stored width of each right-aligned column.
 * Explicit rather than DbColumn.columnSize, because a view column can be
 * wider than the code stored in it. Only add columns confirmed padded in
 * the database: padding a left-aligned column (a name) breaks searches on it.
 */
export const PADDED_WIDTH: Record<string, number> = {
    ARANK: 4,
};

// Exact-match operators only (backend names). LIKE searches stay as typed.
const PAD_OPERATORS = new Set(["=", "<>", "IN", "NOT IN"]);

/* ------------------------------------------------------------------ */
/* Helpers used by QueryBuilderHelper.ts                               */
/* ------------------------------------------------------------------ */

const listFor = (view: string, column: string): PickOption[] | undefined =>
    VIEW_PICK_LISTS[String(view ?? "").toUpperCase()]?.[column] ?? ALL_VIEWS[column];

/** Any list for this column. Saved-query loading does not know the view. */
const anyListFor = (column: string): PickOption[] | undefined =>
    ALL_VIEWS[column] ?? Object.values(VIEW_PICK_LISTS).find((cols) => cols[column])?.[column];

const optionLabel = (o: PickOption): string => (o.literal ? `${o.code} – ${o.literal}` : o.code);

/** Same code, ignoring case; numeric codes also match without leading zeros ("9" = "09"). */
const sameCode = (code: string, typed: string): boolean =>
    code.toLowerCase() === typed.toLowerCase() ||
    (/^\d+$/.test(code) && /^\d+$/.test(typed) && Number(code) === Number(typed));

/** Adds a dropdown to a field when its view has a pick list for that column. */
export const withPickList = (field: Field, view: string): Field => {
    const list = listFor(view, field.name);
    if (!list) return field;
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
    if (!width || !PAD_OPERATORS.has(backendOp)) return value;
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
    const list = anyListFor(field);
    if (!list || typeof value !== "string") return value;
    return value
        .split(",")
        .map((v) => {
            const hit = list.find((o) => sameCode(o.code, v.trim()));
            return hit ? hit.code : v;
        })
        .join(",");
};
