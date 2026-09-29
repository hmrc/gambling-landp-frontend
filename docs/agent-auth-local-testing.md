# Testing the Agent Journey Locally (Gambling / MGD)

## What this covers

When an agent signs in to the gambling service, we don't just check they have an agent enrolment — we verify they actually *hold* the client they're trying to view (the `hasClient` check). Getting that to pass locally trips people up, because the "which clients does this agent have" data lives across several services, not in one repo.

This page explains how to set up the data end-to-end for MGD, and how to fix the Oracle grants error most people hit on a partial DB build.

**Service chain:** gambling frontends → gambling backend → rds-datacache-proxy → client-exchange-proxy → enrolment-store-stub → onprem Oracle

## Prerequisites

Pull latest service-manager-config which has up-to-date DASS_GAMBLING_ALL profile and run sm2 --start DASS_GAMBLING_ALL

**Note:** You only need *enrolment-store-stub* (9595), not the real enrolment-store-proxy.

## Two values you must get right

- **CredID** — the agent's Government Gateway credential id. Must be identical in the auth-login-stub CredID box, the stub seed, and the client-exchange trigger URL.
- **regNumber** — the client's MGD registration number. Must be identical in the stub seed and the statement URL, **and** must exist as a real client (see below).

Everything else (agentId, `HMRCMGDAGENTREF` value, groupId, agent code/name) is free — the values aren't checked.

Examples used below: CredID `0000001619037237`, client `XWM00000001770`.

## Finding a usable client reg number

This is what catches people out. The `hasClient` check runs an Oracle query (`MGD_CLIENT_SEARCH.hasClient`) that joins:

- `mgd_agt_desig` / `mgd_agt_desig_list` — the agent's client list (written by client-exchange in step 2), and
- `mgd_operator_details` — the client's actual MGD record.

**Important:** a reg number only works if it has a row in `mgd_operator_details`. Having it in the agent's list is not enough. The pre-loaded `mgd_agt_desig_list` rows are **not** backed by operator-details — don't grab a reg number from there.

To find usable reg numbers:

```sql
SELECT mgd_reg_number FROM MGD_DATA.MGD_OPERATOR_DETAILS;
```

Or see `prh-oracle-xe/databases/account-db/scripts/agent_auth_data/data/MGD_OPERATOR_DETAILS.sql`. Out of the box that's just `XWM00000001770`. GTR regimes (GBD/PBD/RGD) work the same way against `prh-oracle-xe/databases/gtr-db` data, using `HMRC-GTS-GBD` etc.

## Step 1 - Seed the delegated enrolment (enrolment-store-stub)

The auth-login-stub wizard enrolments only go into the bearer token; the backend reads the agent's clients live from enrolment-store, so the delegation must be seeded here.

```bash
curl -X POST http://localhost:9595/enrolment-store-stub/data \
  -H 'Content-Type: application/json' -d '{
    "groupId": "90ccf333-0000-0000-0000-0000000000aa",
    "affinityGroup": "Agent", "agentCode": "AGENT001", "agentId": "12345", "agentName": "Test Agent",
    "users": [ { "credId": "2095019333011821", "name": "Test Agent", "email": "agent@test.com", "credentialRole": "Admin" } ],
    "enrolments": [
      { "serviceName": "HMRC-MGD-ORG",
        "identifiers": [ { "key": "HMRCMGDRN", "value": "XWM00000001770" } ],
        "enrolmentFriendlyName": "MGD client", "assignedUserCreds": [ "2095019333011821" ],
        "state": "Activated", "enrolmentType": "delegated" } ]
  }'
```

Verify (should return the enrolment, not a 204):

```bash
curl "http://localhost:9595/enrolment-store-proxy/enrolment-store/users/0000001619037237/enrolments?type=delegated&service=HMRC-MGD-ORG&start-record=1&max-records=10"
```

## Step 2 - Pull the client list into the datacache (client-exchange)

```bash
curl "http://localhost:9101/client-exchange-proxy/MGD/0000001619037237/12345/clientlist"
```

The client-exchange-proxy log should say `Updating 1 clients ... in onprem` (if it says 0, step 1 didn't take; if it errors with `ORA-06576`, see Troubleshooting).

## Step 3 - Sign in as the agent (auth-login-stub Authority Wizard)

- CredID: `0000001619037237` (same as above)
- Affinity Group: `Agent`
- Redirect URL: `http://localhost:10403/manage-gambling-tax/statement/mgd/XWM00000001770/current`
- Add one enrolment: `HMRC-MGD-AGNT`, identifier `HMRCMGDAGENTREF` = `0000001619037237`, Activated

Submit — you should land on the client's statement page.

# Troubleshooting: ORA-06576 during client-list retrieval

## Symptom

The client-list download shows as FAILED and the agent is denied:

- `client-exchange-proxy` log: `Could not update client list for <cred> service MGD in onprem ... ORA-06576: not a valid function or procedure name`
- The `MGD_CLIENTLISTSTATUS` row for the credential is stuck at `status = 2` (Failed).

## Root cause

`client-exchange-proxy` connects as `CLIENT_EXCHANGE_APP` and calls the MGD packages **unqualified**, e.g. `MGD_AGENT_LIST_PK.agentListRefreshTimestamp(?, ?)` (and later `setAgentList(...)`). For those to resolve, `CLIENT_EXCHANGE_APP` needs a **synonym** (`CLIENT_EXCHANGE_APP.MGD_AGENT_LIST_PK` -> `MGD_DATA.MGD_AGENT_LIST_PK`) **and EXECUTE privilege** on the package. On an incomplete DB build these are missing, so the name can't be resolved:

- via JDBC -> `ORA-06576: not a valid function or procedure name`
- in a PL/SQL block -> `PLS-00201: identifier 'MGD_AGENT_LIST_PK.AGENTLISTREFRESHTIMESTAMP' must be declared`

It fails in `FinishUpdateService` at the first MGD call (`retrieveStoredChecksum` -> `agentListRefreshTimestamp`), i.e. before the "Updating N clients" log line. The write call `setAgentList` additionally needs the collection type `ARRAY_LIST` (also a synonym + EXECUTE for `CLIENT_EXCHANGE_APP`).

**Tip:** the tell-tale sign it's a grants/name-resolution issue (not a missing procedure): the same call **works when fully qualified** in a SQL tool — `call MGD_DATA.MGD_AGENT_LIST_PK.agentListRefreshTimestamp(...)`.

## Why it works on some machines and not others

The synonyms/grants are created by the `prh-oracle-xe` provisioning scripts (`mgd-filing-db/scripts/grants.sql`; `ARRAY_LIST` is even granted `TO PUBLIC` in `moss-db/schema_SHARED_DATA.sql` with a PUBLIC synonym in `dps-db`). A machine built from the full script set has them all; a partial/older build is missing whichever scripts didn't run.

## Confirm (connect as CLIENT_EXCHANGE_APP)

```sql
SELECT USER FROM DUAL;   -- must be CLIENT_EXCHANGE_APP for this test to mean anything

-- synonym present? expect a row -> TABLE_OWNER = MGD_DATA
SELECT owner, synonym_name, table_owner, table_name
FROM   all_synonyms
WHERE  synonym_name = 'MGD_AGENT_LIST_PK' AND owner IN ('CLIENT_EXCHANGE_APP','PUBLIC');

-- EXECUTE visible? any row = ok
SELECT grantee, table_name, privilege
FROM   all_tab_privs
WHERE  table_name = 'MGD_AGENT_LIST_PK'
AND    grantee IN ('CLIENT_EXCHANGE_APP','CLIENT_EXCHANGE_EXECUTE','PUBLIC');

-- definitive test: the exact unqualified call the app makes.
-- "OK ..." = fine; ORA-06576 / PLS-00201 = the bug.
SET SERVEROUTPUT ON
DECLARE c NUMBER;
BEGIN
  MGD_AGENT_LIST_PK.agentListRefreshTimestamp('0000001619037237', c);
  DBMS_OUTPUT.PUT_LINE('OK - MGD_AGENT_LIST_PK resolved, checksum=' || c);
END;
```

## Fix (connect as SYSTEM / a DBA - NOT as CLIENT_EXCHANGE_APP)

Run as a DBA. Creating a synonym in another schema and granting on `MGD_DATA` / `SHARED_DATA` objects both need DBA rights. Safe to run even if some already exist.

```sql
CREATE OR REPLACE SYNONYM CLIENT_EXCHANGE_APP.MGD_AGENT_LIST_PK FOR MGD_DATA.MGD_AGENT_LIST_PK;
GRANT EXECUTE ON MGD_DATA.MGD_AGENT_LIST_PK TO CLIENT_EXCHANGE_APP;

CREATE OR REPLACE SYNONYM CLIENT_EXCHANGE_APP.ARRAY_LIST FOR SHARED_DATA.ARRAY_LIST;
GRANT EXECUTE ON SHARED_DATA.ARRAY_LIST TO CLIENT_EXCHANGE_APP;
-- optional, matches a full build:
-- GRANT EXECUTE ON SHARED_DATA.ARRAY_LIST TO PUBLIC;
```

No app restart needed — pooled connections re-parse. Restart client-exchange-proxy only if it somehow doesn't pick it up.

## Reset the stuck status and retry

A FAILED status sticks for the grace period (`14400s` = 4h), so clear it to force a fresh download:

```sql
DELETE FROM MGD_DATA.MGD_CLIENTLISTSTATUS WHERE credentialId = '0000001619037237';
COMMIT;
```

Then reload the statement page (or re-run the step-2 curl). Verify:

```sql
-- status should now be 1 (Succeeded)
SELECT credentialId, status, startDate FROM MGD_DATA.MGD_CLIENTLISTSTATUS WHERE credentialId = '0000001619037237';

-- the agent should now have client rows
SELECT COUNT(*) FROM MGD_DATA.MGD_AGT_DESIG ad
JOIN MGD_DATA.MGD_AGT_DESIG_LIST adl ON adl.sequence_number = ad.sequence_number
WHERE ad.credential_id = '0000001619037237';
```

## Status codes (GETCLIENTLISTDOWNLOADSTATUS)

| Value | Meaning | Notes |
|-------|-----------------|-------|
| -1 | InitiateDownload | no row, or row older than the grace period |
| 0 | InProgress | download running |
| 1 | Succeeded | client list ready; reused for the grace period (~4h) |
| 2 | Failed | sticks for the grace period; won't auto-retry until it expires or the row is cleared |
