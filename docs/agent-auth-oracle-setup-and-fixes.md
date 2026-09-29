# Local Oracle Setup & Fixes for the Agent Journey (Gambling / MGD)

This explains why the agent `hasClient` journey works on some local machines but not others, and gives an immediate SQL fix plus the durable fix. It complements `agent-auth-local-testing.md` (the step-by-step journey runbook).

## TL;DR

The agent journey is **multi-schema**. A single request crosses four Oracle schemas:

`CLIENT_EXCHANGE_APP` (app user) → `SHARED_DATA.CLIENT_LIST_STATUS` / `DB_LOGGER` (definer packages) → `MGD_DATA` packages & tables (`MGD_AGENT_LIST_PK`, `MGD_CLIENT_SEARCH`, `MGD_CLIENTLISTSTATUS`) → `MGD_OPERATOR_DETAILS` / agent-auth data.

It only works if **all those schemas + their cross-schema synonyms, grants, types and the `LOGBOOK_DIR` directory live in one Oracle instance**. If your DB is built per-service (or a `setup.sh` was skipped / ran out of order), you get schemas that exist but with missing cross-schema objects, and the flow fails with `ORA-06576` / `ORA-00942` / `ORA-29283`.

## Why it works on some machines and not others

`prh-oracle-xe`'s stock `helpers.sh` builds **one service per container** (`start_database <svc>`). The agent flow spans multiple schemas, so a single-service DB can never satisfy it.

A working machine used a **local, uncommitted `combined-helpers.sh`** — a variant that overrides `get_proxy_host`/`get_port`/`start_database`/`run_sqlplus` to point **every** service's `setup.sh` at **one shared container** (`oraclexe-portal`, port 1521). Running each service's setup with `helper_file=combined-helpers` loads all schemas side by side, so the cross-schema synonyms/grants/types/directory all resolve — mirroring real RDS.

Because `combined-helpers.sh` was never committed, colleagues who clone the repo don't have it and fall back to stock `helpers.sh` → incomplete DB.

A working combined DB contains (at least): `SHARED_DATA, MGD_DATA, GTR_DATA, CLIENT_EXCHANGE_APP, PAYE_DATA, SAPORTAL_DATA, CTPORTAL_DATA`.

## Why individual grants "didn't work" when applied one at a time

Three things bite when patching reactively:

1. **`LOGBOOK_DIR` broken (`ORA-29283`).** `DB_LOGGER.Register` runs *first* in every client-exchange transaction and writes via `UTL_FILE` to the `LOGBOOK_DIR` directory. If that directory points at a non-existent/unwritable OS path, register fails before anything else — no grant elsewhere helps.
2. **INVALID packages.** If a package was left `INVALID` by a partial build, **adding a synonym/grant afterwards does not fix it** — the package stays INVALID until it is **recompiled** *after* the dependency exists.
3. **Stuck state.** A `FAILED` status row sticks for the grace period (~4h) and a permanently-failing Start causes a hot re-init loop, so retries look like "same error" even after a fix.

So the fix must apply **all** grants + fix `LOGBOOK_DIR` + **recompile** + clear the stuck row, together.

## Errors seen → cause → fix

| Error | Where | Cause | Fix |
|---|---|---|---|
| `ORA-06576` (not a valid function or procedure) | client-exchange Finish (`MGD_AGENT_LIST_PK`) | `CLIENT_EXCHANGE_APP` can't resolve the package (missing synonym/grant) | synonym + EXECUTE for `CLIENT_EXCHANGE_APP` |
| `ORA-06576` on `setAgentList` | client-exchange Finish | `ARRAY_LIST` collection type not usable | `ARRAY_LIST` synonym + EXECUTE (PUBLIC on full builds) |
| `ORA-00942` (table or view does not exist) | client-exchange Start (`CLIENT_LIST_STATUS`) | DEFINER package (`SHARED_DATA`) can't reach `MGD_CLIENTLISTSTATUS` | `SHARED_DATA` synonym + CRUD on `MGD_DATA.MGD_CLIENTLISTSTATUS` |
| `ORA-29283` (invalid file operation) | `DB_LOGGER` / `eai_error.report` | `LOGBOOK_DIR` directory points at a missing/unwritable path | recreate `LOGBOOK_DIR` at a writable path |

## Immediate fix (SQL)

Replace `0000001619037237` with the agent credential id. Safe to re-run. `EXEC`/`SET` are SQL\*Plus-only (they error `ORA-00900` in DBeaver/SQL Developer) so the blocks below use plain SQL / `BEGIN…END;`.

### A. Synonyms + grants — run as DBA / SYSTEM

```sql
CREATE OR REPLACE SYNONYM CLIENT_EXCHANGE_APP.CLIENT_LIST_STATUS FOR SHARED_DATA.CLIENT_LIST_STATUS;
CREATE OR REPLACE SYNONYM CLIENT_EXCHANGE_APP.MGD_AGENT_LIST_PK  FOR MGD_DATA.MGD_AGENT_LIST_PK;
CREATE OR REPLACE SYNONYM CLIENT_EXCHANGE_APP.ARRAY_LIST         FOR SHARED_DATA.ARRAY_LIST;
CREATE OR REPLACE SYNONYM CLIENT_EXCHANGE_APP.DB_LOGGER          FOR SHARED_DATA.DB_LOGGER;
GRANT EXECUTE ON MGD_DATA.MGD_AGENT_LIST_PK     TO CLIENT_EXCHANGE_APP;
GRANT EXECUTE ON SHARED_DATA.CLIENT_LIST_STATUS TO CLIENT_EXCHANGE_APP;
GRANT EXECUTE ON SHARED_DATA.DB_LOGGER          TO CLIENT_EXCHANGE_APP;
GRANT EXECUTE ON SHARED_DATA.ARRAY_LIST         TO CLIENT_EXCHANGE_APP;

CREATE OR REPLACE SYNONYM SHARED_DATA.MGD_CLIENTLISTSTATUS FOR MGD_DATA.MGD_CLIENTLISTSTATUS;
GRANT SELECT, INSERT, UPDATE, DELETE ON MGD_DATA.MGD_CLIENTLISTSTATUS TO SHARED_DATA;

CREATE OR REPLACE SYNONYM MGD_DATA.CLIENT_LIST_STATUS FOR SHARED_DATA.CLIENT_LIST_STATUS;
CREATE OR REPLACE SYNONYM MGD_DATA.DB_LOGGER          FOR SHARED_DATA.DB_LOGGER;
GRANT EXECUTE ON SHARED_DATA.CLIENT_LIST_STATUS TO MGD_DATA;
GRANT EXECUTE ON SHARED_DATA.DB_LOGGER          TO MGD_DATA;

GRANT EXECUTE ON SHARED_DATA.ARRAY_LIST TO PUBLIC;
```

### B. Logging directory (fixes ORA-29283) — run as DBA

Point `LOGBOOK_DIR` at a path that exists and is writable inside the Oracle container (`/tmp` is safe).

```sql
CREATE OR REPLACE DIRECTORY LOGBOOK_DIR AS '/tmp';
GRANT READ, WRITE ON DIRECTORY LOGBOOK_DIR TO PUBLIC;
```

### C. Recompile INVALID objects — run as DBA

```sql
BEGIN
  DBMS_UTILITY.COMPILE_SCHEMA('SHARED_DATA', FALSE);
  DBMS_UTILITY.COMPILE_SCHEMA('MGD_DATA',    FALSE);
END;
/
```

### D. Clear the stuck status row — run as DBA / MGD_DATA

```sql
DELETE FROM MGD_DATA.MGD_CLIENTLISTSTATUS WHERE credentialId = '0000001619037237';
COMMIT;
```

### E. Verify — connect as CLIENT_EXCHANGE_APP, unqualified

Both blocks should complete OK. If you need a `MGD_DATA.` prefix you're on the wrong user. (In DBeaver, enable the DBMS Output panel to see the messages, or ignore output — the blocks still run.)

```sql
DECLARE s NUMBER;
BEGIN CLIENT_LIST_STATUS.getClientListDownloadStatus('0000001619037237','MGD',14400,s);
      DBMS_OUTPUT.PUT_LINE('getStatus OK, status='||s); END;
/
DECLARE c NUMBER;
BEGIN MGD_AGENT_LIST_PK.agentListRefreshTimestamp('0000001619037237', c);
      DBMS_OUTPUT.PUT_LINE('MGD_AGENT_LIST_PK OK, checksum='||c); END;
/
```

Then **retry the statement page** (no service restart needed for grants/synonyms/directory).

## Diagnose first (optional but recommended, run as DBA)

Required packages/types/tables:

```sql
SELECT owner, object_name, object_type, status FROM dba_objects
WHERE (owner='SHARED_DATA' AND object_name IN ('CLIENT_LIST_STATUS','DB_LOGGER','ARRAY_LIST'))
   OR (owner='MGD_DATA'    AND object_name IN
        ('MGD_AGENT_LIST_PK','MGD_CLIENT_SEARCH','MGD_CLIENTLISTSTATUS',
         'MGD_OPERATOR_DETAILS','MGD_AGT_DESIG','MGD_AGT_DESIG_LIST'))
ORDER BY owner, object_name;
```

Cross-schema synonyms:

```sql
SELECT owner, synonym_name, table_owner, table_name FROM dba_synonyms
WHERE synonym_name IN ('CLIENT_LIST_STATUS','MGD_AGENT_LIST_PK','ARRAY_LIST','DB_LOGGER','MGD_CLIENTLISTSTATUS')
ORDER BY owner, synonym_name;
```

Key grants:

```sql
SELECT grantee, owner, table_name, privilege FROM dba_tab_privs
WHERE (owner='MGD_DATA'    AND table_name IN ('MGD_AGENT_LIST_PK','MGD_CLIENTLISTSTATUS'))
   OR (owner='SHARED_DATA' AND table_name IN ('CLIENT_LIST_STATUS','ARRAY_LIST','DB_LOGGER'))
ORDER BY grantee, table_name;
```

Logging directory:

```sql
SELECT directory_name, directory_path FROM dba_directories WHERE directory_name='LOGBOOK_DIR';
```

INVALID objects (partial/failed setup leaves these):

```sql
SELECT owner, object_name, object_type FROM dba_objects
WHERE owner IN ('SHARED_DATA','MGD_DATA','CLIENT_EXCHANGE_APP') AND status='INVALID'
ORDER BY owner, object_type;
```

If a **package/table** in the first query is missing (not just a synonym), grants can't help — that service's `setup.sh` never ran, so rebuild.

## Durable fix (recommended)

1. **Commit / share `combined-helpers.sh`** into `prh-oracle-xe` — it's the one artefact that makes a complete, all-in-one DB reproducible.
2. Rebuild the local Oracle as a combined instance: start one shared container, then run each required service's `setup.sh` with `helper_file=combined-helpers` in dependency order (foundation/`SHARED_DATA` first, then `mgd-filing-db`, `gtr-db`, then `account-db` agent-auth data).

This lands every schema, synonym, grant, type, directory and data row together — ending the per-error patching for good.
