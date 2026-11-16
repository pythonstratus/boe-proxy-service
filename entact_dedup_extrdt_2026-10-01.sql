-- =============================================================================
-- entact_dedup_extrdt_2026-10-01.sql
--
-- Removes duplicate rows from ENTITY.ENTACT for EXTRDT = 10/01/2026 ONLY,
-- keeping the earliest-loaded copy (lowest INPUTTIME) of each row.
-- Rows for every other EXTRDT are not touched.
--
-- Scope is exactly the rows this query returns (expected 14852):
--   select * from ENTITY.entact
--   where extrdt = to_date('10/01/2026', 'mm/dd/yyyy')
--
-- A row counts as a duplicate when every column matches another row in that
-- set, IGNORING INPUTTIME (a second load of the same data gets a new
-- INPUTTIME). The summary also shows how many rows are exact copies including
-- INPUTTIME, so you can see which case you have before committing.
--
-- HOW TO RUN IN TOAD
--   1. Confirm you are connected to DEV (ALSDEVSVC.DEV) and Auto Commit is OFF.
--   2. Press F5 (Execute as Script). Read the "Script Output" tab.
--   3. If the summary looks right, run COMMIT. Otherwise run ROLLBACK.
--      Nothing is saved until you do one of those.
-- =============================================================================

SET SERVEROUTPUT ON SIZE UNLIMITED

-- -----------------------------------------------------------------------------
-- STEP 0. Look before you delete
-- -----------------------------------------------------------------------------
-- Rows in scope (expected 14852)
SELECT COUNT(*) AS rows_in_scope
FROM   ENTITY.ENTACT
WHERE  extrdt = TO_DATE('10/01/2026', 'mm/dd/yyyy');

-- Triggers that would fire on DELETE (auditing, cascading logic, etc.)
SELECT owner, trigger_name, triggering_event, status
FROM   all_triggers
WHERE  table_owner = 'ENTITY'
AND    table_name  = 'ENTACT';


-- -----------------------------------------------------------------------------
-- STEP 1. Back up the rows in scope
-- -----------------------------------------------------------------------------
-- Copies every EXTRDT = 10/01/2026 row, which is everything Step 2 can delete.
-- CREATE TABLE ... AS SELECT is DDL, so it commits on its own.
-- On a re-run this fails with ORA-00955 (name already used). That is fine:
-- the existing backup is kept and Step 2 re-checks it.
CREATE TABLE ENTACT_BKP_EXTR20261001 AS
SELECT *
FROM   ENTITY.ENTACT
WHERE  extrdt = TO_DATE('10/01/2026', 'mm/dd/yyyy');

-- Both numbers should be identical
SELECT (SELECT COUNT(*)
        FROM   ENTITY.ENTACT
        WHERE  extrdt = TO_DATE('10/01/2026', 'mm/dd/yyyy')) AS rows_in_scope,
       (SELECT COUNT(*) FROM ENTACT_BKP_EXTR20261001)        AS backup_rows
FROM   dual;


-- -----------------------------------------------------------------------------
-- STEP 2. Delete the duplicates (keeps the earliest-loaded copy of each row)
-- -----------------------------------------------------------------------------
-- * Reads ENTACT's column list itself, so nothing needs editing.
-- * Refuses to run if the backup is missing or its row count does not match.
-- * Checks its own work and rolls back if anything does not add up.
-- * Does NOT commit.
DECLARE
  c_table    CONSTANT VARCHAR2(128)  := 'ENTITY.ENTACT';
  c_backup   CONSTANT VARCHAR2(128)  := 'ENTACT_BKP_EXTR20261001';
  c_scope    CONSTANT VARCHAR2(4000) :=
             'extrdt = TO_DATE(''10/01/2026'', ''mm/dd/yyyy'')';
  -- Columns left out of the comparison (upper case, wrapped in commas)
  c_ignore   CONSTANT VARCHAR2(4000) := ',INPUTTIME,';
  -- Which copy to keep: earliest INPUTTIME. Change ASC to DESC to keep the latest.
  c_keep     CONSTANT VARCHAR2(4000) := '"INPUTTIME" ASC';

  v_cur          INTEGER;
  v_col_cnt      INTEGER;
  v_desc         DBMS_SQL.DESC_TAB2;
  v_cols_all     VARCHAR2(32767);
  v_cols_cmp     VARCHAR2(32767);
  v_ignored      VARCHAR2(1000);
  v_bad_cols     VARCHAR2(1000);
  v_counts_sql   VARCHAR2(32767);
  v_ranked       VARCHAR2(32767);
  v_total        NUMBER;
  v_exact        NUMBER;
  v_dupes        NUMBER;
  v_backup_cnt   NUMBER;
  v_deleted      NUMBER;
  v_remaining    NUMBER;
  v_exact_after  NUMBER;
  v_dupes_after  NUMBER;

  PROCEDURE say(p_text VARCHAR2) IS
  BEGIN
    DBMS_OUTPUT.PUT_LINE(p_text);
  END;
BEGIN
  -- 2a. Read ENTACT's columns
  v_cur := DBMS_SQL.OPEN_CURSOR;
  BEGIN
    DBMS_SQL.PARSE(v_cur, 'SELECT * FROM ' || c_table, DBMS_SQL.NATIVE);
    DBMS_SQL.DESCRIBE_COLUMNS2(v_cur, v_col_cnt, v_desc);
    DBMS_SQL.CLOSE_CURSOR(v_cur);
  EXCEPTION
    WHEN OTHERS THEN
      IF DBMS_SQL.IS_OPEN(v_cur) THEN
        DBMS_SQL.CLOSE_CURSOR(v_cur);
      END IF;
      RAISE;
  END;

  FOR i IN 1 .. v_col_cnt LOOP
    -- Column types Oracle cannot compare row-to-row:
    -- 8 LONG, 24 LONG RAW, 109 object/XMLTYPE, 111 REF, 112 CLOB/NCLOB,
    -- 113 BLOB, 114 BFILE, 119 JSON, 122 nested table, 123 VARRAY
    IF v_desc(i).col_type IN (8, 24, 109, 111, 112, 113, 114, 119, 122, 123) THEN
      v_bad_cols := SUBSTR(v_bad_cols || v_desc(i).col_name || ' ', 1, 1000);
    END IF;

    v_cols_all := v_cols_all || CASE WHEN v_cols_all IS NOT NULL THEN ', ' END
                             || '"' || v_desc(i).col_name || '"';

    IF INSTR(c_ignore, ',' || v_desc(i).col_name || ',') > 0 THEN
      v_ignored := v_ignored || v_desc(i).col_name || ' ';
    ELSE
      v_cols_cmp := v_cols_cmp || CASE WHEN v_cols_cmp IS NOT NULL THEN ', ' END
                               || '"' || v_desc(i).col_name || '"';
    END IF;
  END LOOP;

  IF v_bad_cols IS NOT NULL THEN
    RAISE_APPLICATION_ERROR(-20001,
      'ENTACT has LOB/LONG/object columns that cannot be compared directly: '
      || v_bad_cols || '. Nothing deleted.');
  END IF;

  IF v_ignored IS NULL THEN
    RAISE_APPLICATION_ERROR(-20002,
      'Column INPUTTIME not found in ' || c_table || '. Nothing deleted.');
  END IF;

  -- 2b. One pass over the rows in scope: total, exact copies, and duplicates
  --     ignoring INPUTTIME
  v_counts_sql :=
       'SELECT COUNT(*),'
    || ' NVL(SUM(CASE WHEN rn_all > 1 THEN 1 ELSE 0 END), 0),'
    || ' NVL(SUM(CASE WHEN rn_cmp > 1 THEN 1 ELSE 0 END), 0)'
    || ' FROM (SELECT'
    || ' ROW_NUMBER() OVER (PARTITION BY ' || v_cols_all
    ||                    ' ORDER BY ROWID) AS rn_all,'
    || ' ROW_NUMBER() OVER (PARTITION BY ' || v_cols_cmp
    ||                    ' ORDER BY ' || c_keep || ', ROWID) AS rn_cmp'
    || ' FROM ' || c_table || ' WHERE ' || c_scope || ')';

  EXECUTE IMMEDIATE v_counts_sql INTO v_total, v_exact, v_dupes;

  -- 2c. Safety check: the backup must exist and match the rows in scope
  BEGIN
    EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM ' || c_backup INTO v_backup_cnt;
  EXCEPTION
    WHEN OTHERS THEN
      RAISE_APPLICATION_ERROR(-20003,
        'Backup table ' || c_backup || ' not found (' || SQLERRM
        || '). Run Step 1 first. Nothing deleted.');
  END;

  IF v_backup_cnt <> v_total THEN
    RAISE_APPLICATION_ERROR(-20004,
      'Backup ' || c_backup || ' has ' || v_backup_cnt || ' rows but there are '
      || v_total || ' rows in scope. Nothing deleted. '
      || '(If you already ran and committed this cleanup, it is done.)');
  END IF;

  say('-------------------------------------------------------------');
  say('Scope                       : ' || c_table || ' where ' || c_scope);
  say('Columns ignored             : ' || RTRIM(v_ignored));
  say('Rows in scope               : ' || v_total);
  say('Exact copies (every column) : ' || v_exact);
  say('Duplicates to delete        : ' || v_dupes);
  say('Rows that will remain       : ' || (v_total - v_dupes));

  IF v_dupes > v_exact THEN
    say('-> ' || (v_dupes - v_exact) || ' of the duplicates differ only in '
        || RTRIM(v_ignored) || ' (loaded again later).');
  END IF;

  IF v_dupes = 0 THEN
    say('No duplicates found - nothing to delete.');
    RETURN;
  END IF;

  -- 2d. Delete every copy after the earliest-loaded one
  v_ranked := 'SELECT ROWID AS rid, ROW_NUMBER() OVER (PARTITION BY ' || v_cols_cmp
           || ' ORDER BY ' || c_keep || ', ROWID) AS rn FROM ' || c_table
           || ' WHERE ' || c_scope;

  EXECUTE IMMEDIATE
    'DELETE FROM ' || c_table
    || ' WHERE ROWID IN (SELECT rid FROM (' || v_ranked || ') WHERE rn > 1)';
  v_deleted := SQL%ROWCOUNT;

  -- 2e. Check the result; undo everything if it does not add up
  EXECUTE IMMEDIATE v_counts_sql INTO v_remaining, v_exact_after, v_dupes_after;

  IF v_deleted <> v_dupes
     OR v_dupes_after <> 0
     OR v_remaining <> v_total - v_dupes THEN
    ROLLBACK;
    RAISE_APPLICATION_ERROR(-20005,
      'Check failed (deleted ' || v_deleted || ', expected ' || v_dupes
      || ', remaining ' || v_remaining || ', duplicates left ' || v_dupes_after
      || '). All changes rolled back.');
  END IF;

  say('-------------------------------------------------------------');
  say('Rows deleted                : ' || v_deleted);
  say('Rows now in scope           : ' || v_remaining);
  say('Duplicates remaining        : ' || v_dupes_after);

  IF v_remaining * 2 = v_total THEN
    say('Result is exactly half of the original ' || v_total || ', as expected.');
  ELSE
    say('NOTE: result is NOT exactly half of ' || v_total
        || '. Some rows had more than one extra copy, or none.');
    say('      Review before committing.');
  END IF;

  say('-------------------------------------------------------------');
  say('NOT COMMITTED YET. Run COMMIT to keep it, or ROLLBACK to undo.');
END;
/

-- Rows in scope as this session now sees them (expected 7426)
SELECT COUNT(*) AS rows_in_scope_after
FROM   ENTITY.ENTACT
WHERE  extrdt = TO_DATE('10/01/2026', 'mm/dd/yyyy');


-- -----------------------------------------------------------------------------
-- STEP 3. Save or undo -- run ONE of these yourself after reading the summary
-- -----------------------------------------------------------------------------
-- COMMIT;
-- ROLLBACK;


-- -----------------------------------------------------------------------------
-- IF YOU EVER NEED THE ORIGINAL ROWS BACK (after a COMMIT)
-- -----------------------------------------------------------------------------
-- DELETE FROM ENTITY.ENTACT WHERE extrdt = TO_DATE('10/01/2026', 'mm/dd/yyyy');
-- INSERT INTO ENTITY.ENTACT SELECT * FROM ENTACT_BKP_EXTR20261001;
-- COMMIT;
--
-- Once you're confident the cleanup is correct, drop the backup:
-- DROP TABLE ENTACT_BKP_EXTR20261001 PURGE;
