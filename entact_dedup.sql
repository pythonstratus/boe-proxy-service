-- =============================================================================
-- entact_dedup.sql
-- Backs up ENTACT, then removes exact-duplicate rows, keeping one copy of each.
--
-- HOW TO RUN IN TOAD
--   1. Open this file in the Editor, connected as a user who can read and
--      delete from ENTACT and create a table in its own schema.
--   2. Make sure Auto Commit is OFF (Session menu / toolbar button), so you
--      can review the result before it is saved.
--   3. Press F5 (Execute as Script) to run the whole file. Results appear in
--      the "Script Output" tab.
--   4. Read the summary printed by Step 2. If it looks right, run:  COMMIT;
--      If not, run:  ROLLBACK;   (the backup table is untouched either way)
--
-- "Duplicate" here means a row whose EVERY column matches another row.
-- =============================================================================

SET SERVEROUTPUT ON SIZE UNLIMITED

-- -----------------------------------------------------------------------------
-- STEP 0. Look before you delete
-- -----------------------------------------------------------------------------
-- Current row count (you expect 14852)
SELECT COUNT(*) AS entact_rows_before FROM ENTACT;

-- Any triggers that would fire on DELETE (auditing, cascading logic, etc.)?
SELECT owner, trigger_name, triggering_event, status
FROM   all_triggers
WHERE  table_name = 'ENTACT';


-- -----------------------------------------------------------------------------
-- STEP 1. Back up the table (full copy of every row)
-- -----------------------------------------------------------------------------
-- CREATE TABLE ... AS SELECT is DDL, so it commits on its own.
-- If you re-run this script, this line fails with ORA-00955 (name already
-- used). That's fine: the existing backup is kept and Step 2 re-checks it.
CREATE TABLE ENTACT_BKP_20261116 AS SELECT * FROM ENTACT;

-- Both numbers should be identical
SELECT (SELECT COUNT(*) FROM ENTACT)              AS entact_rows,
       (SELECT COUNT(*) FROM ENTACT_BKP_20261116) AS backup_rows
FROM   dual;


-- -----------------------------------------------------------------------------
-- STEP 2. Delete the duplicates (keeps one copy of each distinct row)
-- -----------------------------------------------------------------------------
-- * Reads ENTACT's column list itself, so nothing needs editing.
-- * Refuses to run if the backup is missing or its row count doesn't match.
-- * Checks its own work and rolls back if anything doesn't add up.
-- * Does NOT commit.
DECLARE
  c_table   CONSTANT VARCHAR2(128) := 'ENTACT';
  c_backup  CONSTANT VARCHAR2(128) := 'ENTACT_BKP_20261116';

  v_cur          INTEGER;
  v_col_cnt      INTEGER;
  v_desc         DBMS_SQL.DESC_TAB2;
  v_cols         VARCHAR2(32767);
  v_bad_cols     VARCHAR2(1000);
  v_ranked       VARCHAR2(32767);
  v_total        NUMBER;
  v_backup_cnt   NUMBER;
  v_dupes        NUMBER;
  v_deleted      NUMBER;
  v_remaining    NUMBER;
  v_dupes_after  NUMBER;

  PROCEDURE say(p_text VARCHAR2) IS
  BEGIN
    DBMS_OUTPUT.PUT_LINE(p_text);
  END;
BEGIN
  -- 2a. Get ENTACT's columns (also works if ENTACT is a synonym)
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
    v_cols := v_cols || CASE WHEN i > 1 THEN ', ' END
                     || '"' || v_desc(i).col_name || '"';
  END LOOP;

  IF v_bad_cols IS NOT NULL THEN
    RAISE_APPLICATION_ERROR(-20001,
      'ENTACT has LOB/LONG/object columns that cannot be compared directly: '
      || v_bad_cols || '. Nothing deleted.');
  END IF;

  -- 2b. Safety check: the backup must exist and have the same row count
  EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM ' || c_table INTO v_total;
  BEGIN
    EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM ' || c_backup INTO v_backup_cnt;
  EXCEPTION
    WHEN OTHERS THEN
      RAISE_APPLICATION_ERROR(-20002,
        'Backup table ' || c_backup || ' not found (' || SQLERRM
        || '). Run Step 1 first. Nothing deleted.');
  END;

  IF v_backup_cnt <> v_total THEN
    RAISE_APPLICATION_ERROR(-20003,
      'Backup ' || c_backup || ' has ' || v_backup_cnt || ' rows but '
      || c_table || ' has ' || v_total || '. Nothing deleted. '
      || '(If you already ran and committed this cleanup, it is done.)');
  END IF;

  -- 2c. Number each group of identical rows; every row after the first is a duplicate
  v_ranked := 'SELECT ROWID AS rid, ROW_NUMBER() OVER (PARTITION BY ' || v_cols
           || ' ORDER BY ROWID) AS rn FROM ' || c_table;

  EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM (' || v_ranked || ') WHERE rn > 1'
    INTO v_dupes;

  say('-----------------------------------------------------------');
  say('Columns compared       : ' || v_col_cnt);
  say('Rows before            : ' || v_total);
  say('Distinct rows          : ' || (v_total - v_dupes));
  say('Duplicate rows found   : ' || v_dupes);

  IF v_dupes = 0 THEN
    say('No duplicates found - nothing to delete.');
    RETURN;
  END IF;

  -- 2d. Delete the duplicates
  EXECUTE IMMEDIATE
    'DELETE FROM ' || c_table
    || ' WHERE ROWID IN (SELECT rid FROM (' || v_ranked || ') WHERE rn > 1)';
  v_deleted := SQL%ROWCOUNT;

  -- 2e. Check the result; undo everything if it doesn't add up
  EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM ' || c_table INTO v_remaining;
  EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM (' || v_ranked || ') WHERE rn > 1'
    INTO v_dupes_after;

  IF v_deleted <> v_dupes
     OR v_dupes_after <> 0
     OR v_remaining <> v_total - v_dupes THEN
    ROLLBACK;
    RAISE_APPLICATION_ERROR(-20004,
      'Check failed (deleted ' || v_deleted || ', expected ' || v_dupes
      || ', remaining ' || v_remaining || ', duplicates left ' || v_dupes_after
      || '). All changes rolled back.');
  END IF;

  say('Rows deleted           : ' || v_deleted);
  say('Rows after             : ' || v_remaining);
  say('Duplicates remaining   : ' || v_dupes_after);

  IF v_remaining * 2 = v_total THEN
    say('Result is exactly half of the original count, as expected.');
  ELSE
    say('NOTE: result is NOT exactly half of ' || v_total
        || '. Some rows had more than one extra copy, or none.');
    say('      Review before committing.');
  END IF;

  say('-----------------------------------------------------------');
  say('NOT COMMITTED YET. Run COMMIT; to keep it, or ROLLBACK; to undo.');
END;
/

-- Row count as this session now sees it (expected 7426)
SELECT COUNT(*) AS entact_rows_after FROM ENTACT;


-- -----------------------------------------------------------------------------
-- STEP 3. Save or undo -- run ONE of these yourself after reading the summary
-- -----------------------------------------------------------------------------
-- COMMIT;
-- ROLLBACK;


-- -----------------------------------------------------------------------------
-- IF YOU EVER NEED THE ORIGINAL DATA BACK (after a COMMIT)
-- -----------------------------------------------------------------------------
-- DELETE FROM ENTACT;
-- INSERT INTO ENTACT SELECT * FROM ENTACT_BKP_20261116;
-- COMMIT;
--
-- Once you're confident the cleanup is correct, drop the backup:
-- DROP TABLE ENTACT_BKP_20261116 PURGE;
