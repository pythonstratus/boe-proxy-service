/**
 * Query Builder pick lists and padding (EN-1677).
 * Location: src/utils/fieldListUtils.ts  ->  import from "@/utils/fieldListUtils"
 *
 * Lists are keyed by DATABASE COLUMN NAME, because that is what the query
 * builder stores in rule.field and QueryWhereExpression.name.
 *
 * Why padding exists: Legacy stores some codes right-aligned in fixed-width
 * fields (" 99a", not "99a"). The leading space is what makes them sort
 * correctly as text: a space sorts before "1", so " 99a" comes before "100a".
 * Modern sends exactly what the user picks or types, so at run time we pad
 * the value back to the stored width.
 */
import { Field, ValueEditorType } from "react-querybuilder";

export type PickOption = { code: string; literal: string };

// TODO: replace with the real column names from QueryBuilderConstant.ts
export const COL = {
    PRIORITY_ALPHA: "TODO_PRIORITY_ALPHA_COLUMN",
    PRIORITY: "TODO_PRIORITY_COLUMN",
} as const;

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

export const PICK_LISTS: Record<string, PickOption[]> = {
    [COL.PRIORITY_ALPHA]: PRIORITY_ALPHA,
    [COL.PRIORITY]: PRIORITY,
};

/**
 * Stored width of each right-aligned column.
 * Explicit rather than DbColumn.columnSize, because a view column can be
 * wider than the code stored in it. Only add columns confirmed padded in
 * the database: padding a left-aligned column (a name) breaks searches on it.
 */
export const PADDED_WIDTH: Record<string, number> = {
    [COL.PRIORITY_ALPHA]: 4,
    // [COL.PRIORITY]: 3,   // add only if the DB check shows " 99"
};

// Exact-match operators only (backend names). LIKE searches stay as typed.
const PAD_OPERATORS = new Set(["=", "<>", "IN", "NOT IN"]);

/** Adds a dropdown to any field that has a pick list. */
export const withPickList = (field: Field): Field => {
    const list = PICK_LISTS[field.name];
    if (!list) return field;
    return {
        ...field,
        values: list.map((o) => ({ name: o.code, label: `${o.code} – ${o.literal}` })),
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

/** Saved-query load: maps " 99a" or "99A" back to the option code "99a". */
export const matchPickListValue = (field: string, value: unknown): unknown => {
    const list = PICK_LISTS[field];
    if (!list || typeof value !== "string") return value;
    return value
        .split(",")
        .map((v) => {
            const t = v.trim();
            const hit = list.find((o) => o.code.toLowerCase() === t.toLowerCase());
            return hit ? hit.code : t;
        })
        .join(",");
};
