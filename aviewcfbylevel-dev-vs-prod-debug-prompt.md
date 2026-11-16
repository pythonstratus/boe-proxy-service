# Role

You are a senior Oracle database engineer (Exadata, parallel query, PL/SQL) and my debugging partner. The query below returns fewer rows (or smaller values) in DEV than in PROD, and I need to find out exactly why. Treat this as an investigation: establish the cause with evidence before proposing any fix.

# Ground rules

1. Do not rewrite, optimize or "clean up" the query. I want to keep it as it is. Diagnostic queries must reuse its FROM and WHERE clauses word for word and only replace the SELECT list (with counts or flags). If you think the query itself has a bug, say so and show the evidence, but don't change it.
2. Read-only. Every query you give me must be a SELECT. No DML, DDL or ALTER SESSION against PROD. In DEV, ask before suggesting anything that writes.
3. Back every hypothesis with a check: the query that confirms or rules it out, and what each possible result would mean.
4. Batch the work. I have to run everything in two environments, so give me one labeled script per round (Q1, Q2, ...) that runs as-is in both. Mark anything that must differ by environment. Don't send one query per reply.
5. Keep PROD safe. ENTACT, ENT and TRANTRAIL are large. Prefer COUNT / SUM / GROUP BY over row dumps, flag any query that could be expensive at PROD volume, and give a cheaper variant (statistics from ALL_TABLES, a narrower date range, or a note to run it off-peak).
6. Protect taxpayer data. Never ask me to paste TINs, names (TP, TP2), addresses or other PII. To match specific rows across environments, use STANDARD_HASH or ORA_HASH, or non-identifying attributes (ROID, dates, codes).
7. Be honest about uncertainty. Rank hypotheses by likelihood, say when you are guessing, and re-rank as results come in. If my results contradict your leading hypothesis, say so plainly and change course.
8. DEV is usually masked, subsetted or stale. Separate expected differences from real defects before calling anything a bug.

# The problem

- DEV result: [row count, or the values that differ]
- PROD result: [row count, or the values that differ]
- What differs: [fewer rows / smaller or different column values / both]
- How I measured: [through the app / directly in a SQL client / both, with the numbers for each]
- Bind values used, DEV and PROD: :org = [ ], :elevel = [ ], :levelValue = [ ], :daysUpperLimit = [ ]
- Timeline: [always different / started on DATE / started after deploying this rewrite / after a DEV refresh]
- Is this rewritten version deployed in DEV only, PROD only, or both? [ ]

# Environment facts (write "unknown" for anything you don't know)

- Oracle version, and whether each environment runs on Exadata:
- Schema/user the app connects as, DEV and PROD:
- Last DEV refresh from PROD, and whether it was a full copy or a subset:
- Masking in DEV (which tables and columns, and whether it is consistent across ENT, ENTACT, TRANTRAIL and ENTMOD):
- Parallel query settings in each environment (parallel_max_servers, Resource Manager limits), if known:
- App layer: how the four binds are set (positional or named, and where the values come from, e.g. the logged-in user's org), statement timeout, maxRows / fetch size, caching, and how the UI handles the 100001-row sentinel:
- Recent changes (deploys, migrations, config, refreshes):

# The query: AViewCFByLevel.sql

I transcribed this from screenshots of my editor, so small typos are possible; my checked-in file is the source of truth. The file's own header says it is a performance rewrite of an earlier version, and that its SELECT expressions were themselves transcribed from photos (the lines marked VERIFY).

```sql
-- ==========================================================================
-- AViewCFByLevel.sql — Case-Related Activity View, filtered by org level
-- ==========================================================================
-- PERFORMANCE REWRITE (parity with TViewCaseRelatedByLevel.sql).
-- (Header uses -- line comments only, to avoid the */-inside-block-comment
--  hard-parse hazard.)
--
-- Changes vs. prior version (each marked  >>> OPT  inline below):
--   1. Exadata Smart Scan: replaced the forced index_join hint on the driving
--      ENTACT/ENT scan with FULL + PARALLEL, so sargable predicates offload to
--      the storage cells instead of doing millions of single-block index reads.
--   2. Sargability (org-level filter): TRUNC(a.roid / POWER(10, 8-:elevel))
--      = :levelValue  rewritten as a half-open ROID range. Bare column on the
--      left => offloadable. Identical result set (roid > 0 here).
--   3. Sargability (recency filter): SYSDATE - a.actdt <= :daysUpperLimit
--      rewritten as  a.actdt >= SYSDATE - :daysUpperLimit. Identical result set.
--   4. bind_params CTE: OPT 2 references :elevel and :levelValue TWICE each.
--      Under the shared positional binder that expands to 6 binds and throws
--      ORA-17041. The CTE declares each bind EXACTLY ONCE, in the ORIGINAL order
--      (org, elevel, levelValue, daysUpperLimit); the body references bp.* — so
--      the positional bind list stays 4-in-order. Verified by stripping comments
--      before counting placeholders.
--   5. Display cap: deterministic ORDER BY + 100001-row sentinel (TView OOM
--      remediation). ORDER BY here is correct because this is the DISPLAY query;
--      the export SQL must omit it. REQUIRES the AView UI to treat 100001 as the
--      truncation sentinel and show the banner, as TView does — see bottom note.
--
-- Intentionally NOT changed:
--   - index() hints on the correlated STATUS / EXISTS point-lookups (~line 70,
--     ~line 167): single-key equality probes; an index is the correct Smart-
--     Scan-friendly path there, NOT a full scan.
--   - STATUS / ASSNRO correlated subqueries — LEGACY MATCH, logic untouched.
--   - mft_ind_vals pipelined function — known secondary bottleneck, deferred
--     (set-based-join replacement tracked separately).
--
-- TRANSCRIBED FROM SCREENSHOTS: the verbatim SELECT column expressions were read
-- from photos. Treat your checked-in file as source of truth for those and diff
-- before committing. Highest-risk tokens marked  -- VERIFY  (TO_CHAR masks,
-- SUBSTR offsets, DECODE mapping lists, NAICSCD space padding).
-- ==========================================================================

WITH bind_params AS (
    -- >>> OPT 4: each bind declared ONCE, in original positional order.
    SELECT :org            AS org,
           :elevel         AS elevel,
           :levelValue     AS levelValue,
           :daysUpperLimit AS daysUpperLimit
    FROM dual
)
SELECT *
FROM (
    -- NOTE: the disabled ROW_NUMBER() dedup wrapper from the original (was
    -- commented out around lines 28-34 / 78 / 242) is intentionally omitted here
    -- to match current behavior. Re-add from source if you still want it.
    SELECT
        -- >>> OPT 1: Smart Scan on the driving tables.
        --   was: /*+ index_join(a, entact_actdt_ix, entact_roid_ix) */
        /*+ FULL(a) PARALLEL(a, 8) FULL(e) PARALLEL(e, 8) */
        a.ROID                                              AS ROID,
        a.TIN                                               AS TIN,
        a.TINTT                                             AS TINTT,
        a.TINFS                                             AS TINFS,
        a.ACTSID                                            AS TINSID,
        DECODE(e.TPCTRL, 'E3', ' ', 'E7', ' ', e.tpctrl)    AS TPCTRL,
        SUBSTR(e.TP, 1, 35)                                 AS TP,
        SUBSTR(e.TP2, 1, 35)                                AS TP2,
        SUBSTR(e.STREET, 1, 35)                             AS STREET,
        e.CITY                                              AS CITY,
        e.STATE                                             AS STATE,
        (CASE
            WHEN e.zipcde < 100000
                THEN TO_NUMBER(TO_CHAR(e.ZIPCDE, '09999'))                               -- VERIFY mask
            WHEN e.zipcde BETWEEN 99999 AND 999999999
                THEN NVL(TO_NUMBER(SUBSTR(TO_CHAR(e.ZIPCDE, '099999999'), -9, 5)), 0)   -- VERIFY mask/offsets
            ELSE
                NVL(TO_NUMBER(SUBSTR(TO_CHAR(e.ZIPCDE, '099999999999'), -12, 5)), 0)    -- VERIFY mask/offsets
        END)                                                AS ZIPCDE,
        a.ACTDT                                             AS ACTDT,
        a.CODE                                              AS CASECODE,
        a.SUBCODE                                           AS SUBCODE,
        -- STATUS: correlated subquery w/ NVL default 'P' (LEGACY MATCH — unchanged)
        NVL(NULLIF(TRIM(
            (SELECT status
             FROM (
                 SELECT c2.status,
                        ROW_NUMBER() OVER (ORDER BY c2.EXTRDT DESC, c2.ROWID) AS rn
                 FROM TRANTRAIL c2
                 WHERE c2.tinsid = a.actsid
                   AND c2.roid = a.roid
                   AND c2.EXTRDT = (
                       SELECT /*+ index(d, trantrail_tinsid_ix) */
                              NVL(MAX(d.EXTRDT), TO_DATE('01/01/1900', 'mm/dd/yyyy'))
                       FROM TRANTRAIL d
                       WHERE d.TINSID = c2.TINSID
                         AND d.ROID = c2.ROID
                         AND DECODE(d.segind, 'A', 1, 'C', 1, 'I', 1, 0) =
                             DECODE(mft, 0, 0, 1)
                   )
             ) WHERE rn = 1)
        ), ''), 'P')                                        AS STATUS,
        e.LDIND                                             AS LDIND,
        NVL(e.RISK, 399)                                    AS RISK,
        MFT                                                 AS MFT,
        a.PERIOD                                            AS PERIOD,
        TYPCD                                               AS M_TYPE,
        a.AROID                                             AS AROID,
        -- ASSNRO: correlated subquery (LEGACY MATCH — unchanged)
        (SELECT MAX(t2.ASSNRO)
         FROM TRANTRAIL t2
         WHERE (t2.roid = a.aroid OR t2.roid = a.roid)
           AND t2.tinsid = a.actsid
           AND DECODE(t2.segind, 'A', 1, 'C', 1, 'I', 1, 0) =
               DECODE(mft, 0, 0, 1))                        AS ASSNRO,
        e.ASSNCFF                                           AS ASSNCFF,
        BODCD                                               AS BODCD,
        AMOUNT                                              AS AMOUNT,
        RTNSEC                                              AS RTNSEC,
        DISPCODE                                            AS DISPCD,
        NVL(GRPIND, 9)                                      AS ENTITYIND,
        FORM809                                             AS FORM809,
        RPTCD                                               AS RPTCD,
        a.CC                                                AS CC,
        TC                                                  AS TC,
        a.EXTRDT                                            AS EXTRDT,
        a.TYPEID                                            AS TYPEID,
        a.TSACTCD                                           AS TSACTCD,
        TOTASSD                                             AS TOTASSD,
        BAL_941_14                                          AS BAL_941_14,
        e.GRADE                                             AS CASEGRADE,
        CASEIND                                             AS CASEIND,
        NVL(b.NAICSCD, '      ')                            AS NAICSCD,        -- VERIFY spacing
        PRGNAME1                                            AS PRGNAME1,
        PRGNAME2                                            AS PRGNAME2,
        CCNIPSELECTCD                                       AS CCNIPSELECTCD,
        CNT_941_14                                          AS CNT_941_14,
        CNT_941                                             AS CNT_941,
        TDI_CNT_941                                         AS TDI_CNT_941,
        NVL(b.TDAcnt, 0)                                    AS TDACNT,
        NVL(b.TDIcnt, 0)                                    AS TDICNT,
        NVL((b.TDAcnt + b.TDIcnt), 0)                       AS MODCNT,
        DECODE(NVL(b.STATUS, 'X'), 'O', STATIND(a.ACTSID), 0) AS STATIND,
        b.ASSNFLD                                           AS ASSNFLD,
        (CASE
            WHEN NVL(b.segind, ' ') IN ('A', 'C', 'I')
                THEN GETASSNQUE(e.TIN, e.TINTT, e.TINFS, e.ASSNCFF, b.ASSNRO)
            ELSE b.ASSNRO
        END)                                                AS ASSNQUE,
        NVL(
            DECODE(NVL(b.status, ' '),
                'C', b.CLOSEDT,
                'X', b.CLOSEDT,
                TO_DATE('01/01/1900', 'mm/dd/yyyy')),
            TO_DATE('01/01/1900', 'mm/dd/yyyy'))            AS CLOSEDT,
        NVL(DT_DOD, TO_DATE('01/01/1900', 'mm/dd/yyyy'))    AS DT_DOD,
        b.XXDT                                              AS XXDT,
        b.INITDT                                            AS INITDT,
        DT_OA                                               AS DT_OA,
        DT_POA                                              AS DT_POA,
        TRUNC(ASSNPICKDT(e.TIN, e.TINFS, e.TINTT,
            NVL(b.STATUS, 'P'), NVL(b.PROID, 0), a.ROID))   AS PICKDT,
        DVICTCD                                             AS DVICTCD,
        DECODE(DECODE(NVL(b.ZIPCDE, 0), 00000,
            ASSNQPICK(e.TIN, e.TINFS, e.TINTT, INTLQPICK(e.TIN, e.TINFS, e.TINTT, NVL(b.STATUS, 'P'))),
            DECODE(e.CITY, 'APO', ASSNQPICK(e.TIN, e.TINFS, e.TINTT, INTLQPICK(e.TIN, e.TINFS, e.TINTT, NVL(b.STATUS, 'P'))),
            'FPO',
            ASSNQPICK(e.TIN, e.TINFS, e.TINTT, INTLQPICK(e.TIN, e.TINFS, e.TINTT, NVL(b.STATUS, 'P'))),
            ASSNQPICK(e.TIN, e.TINFS, e.TINTT, NVL(b.PROID, 0)))),
            '', 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, '?') AS QPICKIND,   -- VERIFY mapping list
        NVL(b.FLDHRS, 0)                                    AS FLDHRS,
        NVL(b.EMPHRS, 0)                                    AS EMPHRS,
        NVL(b.HRS, 0)                                       AS HRS,
        (CASE
            WHEN NVL(b.ORG, ' ') = 'CP' THEN CCPHRS
            ELSE GREATEST(NVL(TOTHRS, 0), NVL(b.EMPHRS, 0))
        END)                                                AS TOTHRS,
        c.IND_941                                           AS IND_941,
        (CASE WHEN c.ind_941 = 0 THEN 'No' ELSE 'Yes' END)  AS FORMATTED_IND_941,
        HINFIND                                             AS HINFIND,
        DECODE(NVL(b.segind, ' '), 'A', AGEIND, 'C', AGEIND, 'I', AGEIND, 'C') AS AGEIND,   -- VERIFY default arm
        TO_NUMBER(PDTIND)                                   AS CAUIND,
        DECODE(
            NVL(b.STATUS, 'X'),
            'O', (CASE
                    WHEN DECODE(NVL(b.SEGIND, ' '), 'C', 1, 'A', 1, 'I', 1, 0) = 1
                     AND DECODE(e.casecode, '201', 1, '301', 1, '401', 1, '601', 1, 0) = 1
                     AND totassd >= 10000
                     AND EXISTS (
                         SELECT /*+ index(entmod, entmod_sid_ix) */ 1
                         FROM entmod em
                         WHERE em.emodsid = b.tinsid
                           AND em.status = 'O'
                           AND em.mft IN (1, 9, 11, 12, 13, 14, 16, 64)
                           AND b.assnro + 150 < duedt(em.period, em.mft))
                    THEN 1
                    ELSE 0
                  END),
            0)                                              AS PYRENT,
        DECODE(NVL(b.segind, ' '), 'A', PYRIND, 'C', PYRIND, 'I', PYRIND, 0) AS PYRIND,
        FATCAIND                                            AS FATCAIND,
        FEDCONIND                                           AS FEDCONIND,
        FEDEMPIND                                           AS FEDEMPIND,
        IRSEMPIND                                           AS IRSEMPIND,
        L903                                                AS L903,
        TO_NUMBER(NVL(e.LFIIND, 0))                         AS LFIIND,
        LLCIND                                              AS LLCIND,
        DECODE(NVL(b.segind, ' '), 'A', RPTIND, 'C', RPTIND, 'I', RPTIND, 'F') AS RPTIND,
        THEFTIND                                            AS THEFTIND,
        INSPCIND                                            AS INSPCIND,
        OICACCYR                                            AS OICACCYR,
        LPAD(NVL(e.RISK, 399) || NVL(e.ARISK, 'e'), 4, ' ') AS ARANK,          -- VERIFY 'e' default
        0                                                   AS TOT_IRP_INC,
        b.EMPTOUCH                                          AS EMPTOUCH,
        b.LSTTOUCH                                          AS LSTTOUCH,
        (CASE
            WHEN NVL(b.ORG, ' ') = 'CP' THEN CCPTOUCH
            ELSE GREATEST(TOTTOUCH, b.EMPTOUCH)
        END)                                                AS TOTTOUCH,
        NVL(e.STREET2, ' ')                                 AS STREET2,
        NVL(b.PROID, 0)                                     AS PROID,
        0                                                   AS TOT_INC_DELQ_YR,
        0                                                   AS PRIOR_YR_RET_AGI_AMT,
        0                                                   AS TXPER_TXPYR_AMT,
        0                                                   AS PRIOR_ASSGMNT_NUM,
        AGI_AMT                                             AS AGI_AMT,
        TO_DATE('01/01/1900', 'mm/dd/yyyy')                 AS PRIOR_ASSGMNT_ACT_DT,
        BAL_941                                             AS BAL_941,
        (SELECT MIN(selcode) FROM entmod WHERE emodsid = e.tinsid) AS SELCODE
    -- ==========================================================================
    -- FROM clause: TRANTRAIL is OUTER-JOINED as a ranked subquery.
    -- Picks at most ONE row per TINSID (most recent EXTRDT for org).
    -- Records with no TRANTRAIL match survive — b.columns become NULL and are
    -- handled by the NVL defaults above.
    -- mft_ind_vals uses a.ACTSID (not b.tinsid) since b may be NULL.
    -- ==========================================================================
    FROM ENT e,
         ENTACT a,
         TABLE(mft_ind_vals(a.ACTSID, e.tinfs)) c,
         (SELECT tb.*,
                 ROW_NUMBER() OVER (
                     PARTITION BY tb.TINSID
                     ORDER BY tb.EXTRDT DESC, tb.ROWID DESC
                 ) AS trail_rn
          FROM TRANTRAIL tb, bind_params bp          -- >>> OPT 4: :org via bp (declared once)
          WHERE tb.org = bp.org
         ) b,
         bind_params bp                              -- >>> OPT 4: 1-row cross join supplies bp.*
    WHERE e.TINSID = a.ACTSID
      -- OUTER JOIN: TRANTRAIL is optional, records survive without it
      AND a.ACTSID = b.TINSID (+)
      AND b.trail_rn (+) = 1
      -- Standard ENTACT filters (unchanged)
      AND (   (    a.aroid BETWEEN 21011000 AND 35165899
               AND MOD(a.aroid, 1000000) BETWEEN 10000 AND 169999
               AND MOD(a.aroid, 10000) BETWEEN 1000 AND 5899)
           OR (    a.roid BETWEEN 21011000 AND 35165899
               AND MOD(a.roid, 1000000) BETWEEN 10000 AND 169999
               AND MOD(a.roid, 10000) BETWEEN 1000 AND 5899))
      --AND EXTRACT(YEAR FROM a.period) > 1901
      -- >>> OPT 2: org-level filter made sargable. Was:
      --     AND TRUNC(a.roid / POWER(10, 8 - :elevel)) = :levelValue
      --     TRUNC(roid/D) = L  <=>  L*D <= roid < (L+1)*D   (roid > 0 here).
      AND a.roid >= bp.levelValue       * POWER(10, 8 - bp.elevel)
      AND a.roid <  (bp.levelValue + 1) * POWER(10, 8 - bp.elevel)
      -- >>> OPT 3: recency filter made sargable. Was:
      --     AND SYSDATE - a.actdt <= :daysUpperLimit
      AND a.actdt >= SYSDATE - bp.daysUpperLimit
      -- AND TRUNC(SYSDATE) - a.actdt <= :daysUpperLimit
)
-- >>> OPT 5: deterministic display cap (OOM remediation, TView parity).
--   Requires the AView UI to treat 100001 as the truncation sentinel and show the
--   banner, exactly as TViewCaseRelatedByLevel now does — otherwise this silently
--   drops rows.
--   -- VERIFY: set this ORDER BY column list to EXACTLY match the TView display cap.
ORDER BY ROID, TINSID, MFT, PERIOD, EXTRDT DESC
FETCH FIRST 100001 ROWS ONLY
```

# What we already know about this query

Use this as a starting point and correct it if it's wrong.

**Where rows can be lost (this changes the row count)**

1. The inner join of ENT e to ENTACT a on e.TINSID = a.ACTSID.
2. TABLE(mft_ind_vals(a.ACTSID, e.tinfs)) c. It is joined with a comma and no condition, so it acts as an inner join: if the function returns no rows for a pair, that pair disappears, and if it returns several, the pair repeats. What it returns depends on the function's own code and on the data it reads.
3. The hard-coded ROID / AROID ranges and MOD filters ("Standard ENTACT filters").
4. The org-level filter on a.roid (OPT 2), driven by :elevel and :levelValue.
5. The recency filter a.actdt >= SYSDATE - :daysUpperLimit (OPT 3). It depends on the database server's clock and on how recent the data is. If DEV's data is older than the window, rows drop out.
6. FETCH FIRST 100001 ROWS ONLY. If PROD reaches 100001, PROD's number is a cap, not a true count.

The outer join to TRANTRAIL (b, at most one row per TINSID with trail_rn = 1) and the scalar subqueries in the SELECT list cannot remove rows. They can only change values.

**Where values can change without losing rows**

- If :org doesn't match TRANTRAIL.org in DEV, every b column is NULL and the NVL defaults appear (0, 'P', 01/01/1900 and so on).
- The lines marked VERIFY (ZIPCDE masks, NAICSCD padding, QPICKIND mapping list, AGEIND default, ARANK default) were transcribed from photos when this rewrite was written, so they may differ from the version PROD runs.
- STATIND, GETASSNQUE, ASSNPICKDT, ASSNQPICK, INTLQPICK and DUEDT, and the STATUS / ASSNRO / SELCODE subqueries, depend on their own data and code versions.

**What changed from the previous version, and the risk each change carries**

- OPT 1 (FULL + PARALLEL hints): should not change results. Two caveats. First, the hint comment comes after two other comment lines that follow SELECT, and Oracle may only read hints from the first comment after the keyword, so check whether the hints are actually used. Second, if the query does run in parallel, any function that relies on package variables or session context set by the app runs in PX slaves that don't have that state.
- OPT 2 and OPT 3: the header claims identical results. Verify this with data rather than trusting it (Q3 below does this).
- OPT 4 (bind_params CTE): relies on the four binds arriving in exactly this order: org, elevel, levelValue, daysUpperLimit. If the Java binder supplies them in a different order, or counts placeholders that appear inside comments, the values shift and the level and recency filters silently select the wrong rows. Check the captured bind values position by position.
- OPT 5 (ORDER BY + FETCH FIRST 100001): caps the result. The ORDER BY columns may not be unique, so when the cap is hit, which rows are kept can vary from run to run.
- The FROM-clause note says mft_ind_vals now takes a.ACTSID "not b.tinsid". If the other environment runs an older version that passes b.tinsid, rows with no TRANTRAIL match for :org would call the function with NULL and could drop out.
- The header says a ROW_NUMBER() dedup wrapper from the original was left out because it was disabled there. Confirm PROD's version doesn't have it enabled.

**Starting hypotheses, most likely first (re-rank them as evidence comes in)**

1. DEV data is older than the recency window, so the SYSDATE-based filter drops rows.
2. DEV and PROD aren't running the same SQL text or the same bind values: this rewrite is deployed in one environment only, the binds come from a different logged-in user or org, or the positional bind order is off.
3. mft_ind_vals returns no rows for some ENT/ENTACT pairs in DEV (data it reads is missing, its code differs, or it depends on session state).
4. ROID / org structure differs in DEV, so the hard-coded ranges or the level filter exclude rows.
5. ENT or ENTACT is subsetted or masked in DEV, so e.TINSID = a.ACTSID matches less often.
6. App layer: a cache serving an old result, the 100001 sentinel handling, a timeout, or an error swallowed during fetch. Watch for ORA-01722 from TO_NUMBER(PDTIND), from TO_NUMBER(NVL(e.LFIIND, 0)), or from the QPICKIND DECODE, whose default is '?' while its first result is a number.
7. PROD's figure is capped at 100001, so the comparison itself is skewed.
8. Last resort: wrong results from parallel execution or Exadata Smart Scan.

# Round 1 script (check it, fix anything wrong, add what's missing, then give it back to me as one script)

Use the same bind values in both environments, taken from Q1b where possible.

```sql
-- Q1. Which SQL text actually runs, and how many rows did the app get back?
--     (needs SELECT on V$SQL; add AND parsing_schema_name = '<APP_SCHEMA>' to cut noise)
SELECT sql_id,
       child_number,
       plan_hash_value,
       parsing_schema_name,
       executions,
       rows_processed,
       ROUND(rows_processed / NULLIF(executions, 0)) AS rows_per_exec,
       px_servers_executions,
       last_active_time,
       CASE WHEN DBMS_LOB.INSTR(sql_fulltext, 'bind_params') > 0
            THEN 'NEW (bind_params rewrite)' ELSE 'OLD' END AS version_guess,
       DBMS_LOB.SUBSTR(sql_fulltext, 150, 1) AS sql_start
FROM   v$sql
WHERE  UPPER(sql_fulltext) LIKE '%MFT_IND_VALS%'
AND    UPPER(sql_fulltext) NOT LIKE '%V$SQL%'
ORDER  BY last_active_time DESC;

-- Q1b. Captured bind values for the app's SQL_ID, by position (sampled, so it can lag)
SELECT sql_id, child_number, position, name, datatype_string, value_string, last_captured
FROM   v$sql_bind_capture
WHERE  sql_id = '&app_sql_id'
ORDER  BY child_number, position;

-- Q2. Data freshness and server clock
--     (on PROD, use NUM_ROWS from ALL_TABLES instead of COUNT(*) if a full count is too heavy)
SELECT 'ENTACT' AS what, COUNT(*) AS row_count, MIN(actdt) AS min_date, MAX(actdt) AS max_date FROM ENTACT
UNION ALL
SELECT 'TRANTRAIL', COUNT(*), MIN(extrdt), MAX(extrdt) FROM TRANTRAIL
UNION ALL
SELECT 'ENT', COUNT(*), NULL, NULL FROM ENT
UNION ALL
SELECT 'ENTMOD', COUNT(*), NULL, NULL FROM ENTMOD;

SELECT SYSDATE, SYSTIMESTAMP, DBTIMEZONE, SESSIONTIMEZONE FROM dual;

-- Q3. ENTACT funnel, plus a check that OPT 2 and OPT 3 really match the old predicates.
--     opt2_mismatch and opt3_mismatch should both be 0.
WITH bind_params AS (
    SELECT :org AS org, :elevel AS elevel, :levelValue AS levelValue, :daysUpperLimit AS daysUpperLimit
    FROM dual
),
f AS (
    SELECT /*+ FULL(a) PARALLEL(a, 8) */
           CASE WHEN (    a.aroid BETWEEN 21011000 AND 35165899
                      AND MOD(a.aroid, 1000000) BETWEEN 10000 AND 169999
                      AND MOD(a.aroid, 10000) BETWEEN 1000 AND 5899)
                  OR (    a.roid BETWEEN 21011000 AND 35165899
                      AND MOD(a.roid, 1000000) BETWEEN 10000 AND 169999
                      AND MOD(a.roid, 10000) BETWEEN 1000 AND 5899)
                THEN 1 ELSE 0 END                                              AS f_roid_range,
           CASE WHEN a.roid >= bp.levelValue       * POWER(10, 8 - bp.elevel)
                 AND a.roid <  (bp.levelValue + 1) * POWER(10, 8 - bp.elevel)
                THEN 1 ELSE 0 END                                              AS f_level_new,
           CASE WHEN TRUNC(a.roid / POWER(10, 8 - bp.elevel)) = bp.levelValue
                THEN 1 ELSE 0 END                                              AS f_level_old,
           CASE WHEN a.actdt >= SYSDATE - bp.daysUpperLimit THEN 1 ELSE 0 END  AS f_recent_new,
           CASE WHEN SYSDATE - a.actdt <= bp.daysUpperLimit THEN 1 ELSE 0 END  AS f_recent_old
    FROM ENTACT a, bind_params bp
)
SELECT COUNT(*)                                                       AS s0_entact_rows,
       SUM(f_roid_range)                                              AS s1_roid_range,
       SUM(f_roid_range * f_level_new)                                AS s2_level,
       SUM(f_roid_range * f_level_new * f_recent_new)                 AS s3_recent,
       SUM(f_level_new)                                               AS only_level,
       SUM(f_recent_new)                                              AS only_recent,
       SUM(CASE WHEN f_level_new  <> f_level_old  THEN 1 ELSE 0 END)  AS opt2_mismatch,
       SUM(CASE WHEN f_recent_new <> f_recent_old THEN 1 ELSE 0 END)  AS opt3_mismatch
FROM f;

-- Q4. After the ENT join, how many rows does mft_ind_vals return per pair?
--     fn_rows = 0 means that pair is dropped from the result.
--     SUM(pairs) = rows after the ENT join; SUM(result_rows) = rows after the function join.
WITH bind_params AS (
    SELECT :org AS org, :elevel AS elevel, :levelValue AS levelValue, :daysUpperLimit AS daysUpperLimit
    FROM dual
),
pairs AS (
    SELECT /*+ FULL(a) PARALLEL(a, 8) FULL(e) PARALLEL(e, 8) */
           a.ACTSID AS actsid,
           e.TINFS  AS tinfs
    FROM   ENT e, ENTACT a, bind_params bp
    WHERE  e.TINSID = a.ACTSID
      AND  (   (    a.aroid BETWEEN 21011000 AND 35165899
                AND MOD(a.aroid, 1000000) BETWEEN 10000 AND 169999
                AND MOD(a.aroid, 10000) BETWEEN 1000 AND 5899)
            OR (    a.roid BETWEEN 21011000 AND 35165899
                AND MOD(a.roid, 1000000) BETWEEN 10000 AND 169999
                AND MOD(a.roid, 10000) BETWEEN 1000 AND 5899))
      AND  a.roid >= bp.levelValue       * POWER(10, 8 - bp.elevel)
      AND  a.roid <  (bp.levelValue + 1) * POWER(10, 8 - bp.elevel)
      AND  a.actdt >= SYSDATE - bp.daysUpperLimit
)
SELECT fn_rows,
       COUNT(*)           AS pairs,
       fn_rows * COUNT(*) AS result_rows
FROM  (SELECT (SELECT COUNT(*) FROM TABLE(mft_ind_vals(p.actsid, p.tinfs))) AS fn_rows
       FROM pairs p)
GROUP BY fn_rows
ORDER BY fn_rows;

-- Q5. The real FROM/WHERE, word for word, counted without the 100001 cap.
--     s6 should equal SUM(result_rows) from Q4. If s6 > 100001, the app's result is capped.
WITH bind_params AS (
    SELECT :org AS org, :elevel AS elevel, :levelValue AS levelValue, :daysUpperLimit AS daysUpperLimit
    FROM dual
)
SELECT /*+ FULL(a) PARALLEL(a, 8) FULL(e) PARALLEL(e, 8) */
       COUNT(*)        AS s6_final_uncapped,
       COUNT(b.TINSID) AS rows_with_trantrail_match_for_org
FROM ENT e,
     ENTACT a,
     TABLE(mft_ind_vals(a.ACTSID, e.tinfs)) c,
     (SELECT tb.*,
             ROW_NUMBER() OVER (
                 PARTITION BY tb.TINSID
                 ORDER BY tb.EXTRDT DESC, tb.ROWID DESC
             ) AS trail_rn
      FROM TRANTRAIL tb, bind_params bp
      WHERE tb.org = bp.org
     ) b,
     bind_params bp
WHERE e.TINSID = a.ACTSID
  AND a.ACTSID = b.TINSID (+)
  AND b.trail_rn (+) = 1
  AND (   (    a.aroid BETWEEN 21011000 AND 35165899
           AND MOD(a.aroid, 1000000) BETWEEN 10000 AND 169999
           AND MOD(a.aroid, 10000) BETWEEN 1000 AND 5899)
       OR (    a.roid BETWEEN 21011000 AND 35165899
           AND MOD(a.roid, 1000000) BETWEEN 10000 AND 169999
           AND MOD(a.roid, 10000) BETWEEN 1000 AND 5899))
  AND a.roid >= bp.levelValue       * POWER(10, 8 - bp.elevel)
  AND a.roid <  (bp.levelValue + 1) * POWER(10, 8 - bp.elevel)
  AND a.actdt >= SYSDATE - bp.daysUpperLimit;

-- Q6. Which months of activity exist for this level? Shows whether DEV's data stops at a refresh date.
WITH bind_params AS (
    SELECT :org AS org, :elevel AS elevel, :levelValue AS levelValue, :daysUpperLimit AS daysUpperLimit
    FROM dual
)
SELECT TRUNC(a.actdt, 'MM') AS act_month,
       COUNT(*)             AS entact_rows
FROM   ENTACT a, bind_params bp
WHERE  a.roid >= bp.levelValue       * POWER(10, 8 - bp.elevel)
AND    a.roid <  (bp.levelValue + 1) * POWER(10, 8 - bp.elevel)
AND    a.actdt >= ADD_MONTHS(TRUNC(SYSDATE, 'MM'), -24)
GROUP  BY TRUNC(a.actdt, 'MM')
ORDER  BY act_month;

-- Q7. Are the functions and tables the same objects and code in both environments?
--     Compare source_hash across environments; a difference means the code differs.
SELECT name, type, COUNT(*) AS line_count, SUM(ORA_HASH(text)) AS source_hash
FROM   all_source
WHERE  name IN ('MFT_IND_VALS', 'STATIND', 'GETASSNQUE', 'ASSNPICKDT', 'ASSNQPICK', 'INTLQPICK', 'DUEDT')
GROUP  BY name, type
ORDER  BY name, type;

SELECT owner, object_name, object_type, status, last_ddl_time
FROM   all_objects
WHERE  object_name IN ('ENT', 'ENTACT', 'TRANTRAIL', 'ENTMOD',
                       'MFT_IND_VALS', 'STATIND', 'GETASSNQUE', 'ASSNPICKDT', 'ASSNQPICK', 'INTLQPICK', 'DUEDT')
ORDER  BY object_name, owner;

SELECT owner, synonym_name, table_owner, table_name, db_link
FROM   all_synonyms
WHERE  synonym_name IN ('ENT', 'ENTACT', 'TRANTRAIL', 'ENTMOD',
                        'MFT_IND_VALS', 'STATIND', 'GETASSNQUE', 'ASSNPICKDT', 'ASSNQPICK', 'INTLQPICK', 'DUEDT');
```

# How to run the investigation

Phase 0: Review. Restate the problem in 3-4 lines, check my analysis and hypotheses above, point out anything wrong or missing, and tell me what's missing from my description that would change the plan.

Phase 1: Round 1. Return the corrected Round 1 script. When I paste the results back as a DEV | PROD table, tell me where the funnel drops off and re-rank the hypotheses.

Phase 2: Same SQL? If Q1 shows DEV and PROD running different versions of this SQL, first run both versions in the same environment (DEV) with the same binds. If they disagree, the cause is the code change, not the data, and we drill into which change is responsible (OPT 2, OPT 3, the OPT 4 bind order, the mft_ind_vals argument, the VERIFY lines, the dedup wrapper).

Phase 3: Drill into the step where DEV falls off, one cause per query:
- recency: data age, and the server clock and time zone
- level / ROID: how DEV's ROIDs are distributed against the hard-coded ranges and the level range
- ENT join: TINSID coverage and masking between ENT and ENTACT
- mft_ind_vals: read its source (I'll paste it from ALL_SOURCE), work out which tables and session state it depends on, and check that data in DEV
- app layer: compare V$SQL rows_processed with what the UI shows, check the app logs for ORA- errors, and check whether a cache is serving the result

Phase 4: Trace one row. Help me pick one row that is in PROD's result, find its counterpart in DEV by hash or by non-identifying attributes (ROID, ACTDT, codes), and evaluate each condition, including the mft_ind_vals call, for just that row.

Phase 5: Last resort, execution-dependent results. Only if everything above checks out. In DEV, compare Q5's count against a /*+ NO_PARALLEL */ version and an /*+ OPT_PARAM('cell_offload_processing' 'false') */ version, and check the plan with DBMS_XPLAN.DISPLAY_CURSOR('<sql_id>', NULL, 'TYPICAL +HINT_REPORT'). If the count changes, this is an execution or wrong-results problem, not a data problem.

Phase 6: Conclude. Give me:
- a one-paragraph root cause with the evidence that proves it
- whether it is a real defect (in the rewrite or the app) or an expected DEV/PROD difference
- fix options in ranked order, each changing the query as little as possible
- a check I can keep running to catch this again

# Format for every reply

1. Hypotheses: a short table (hypothesis | likelihood | evidence so far)
2. Script for this round: one block of labeled, commented queries, ready to run in both environments
3. How to read the results: for each query, "if X, then...; if Y, then..."
4. What to paste back: the exact compact table you want from me (for example query | DEV | PROD)

Keep the prose short and don't repeat earlier explanations.

# Start

Do Phase 0, then give me the corrected Round 1 script.
