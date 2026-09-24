
# gambling-landp-frontend

This is the new gambling-landp-frontend repository

## Git Hooks

This project includes a pre-push hook that checks code formatting with scalafmt before pushing.

To activate it, run once after cloning:

If the check fails, format your code with `sbt scalafmtAll` and try again.

## Running the service

Service Manager: `sm2 --start DASS_GAMBLING_ALL`

To run all tests and coverage: `./run_all_tests.sh`

To start the server locally: `sbt run`

To check coverage: `sbt clean coverage test it/test coverageReport`

To enable test-only routes when running locally, start the server with: `sbt 'run -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes 10401'`

## Adding New Pages

### Folder Structure
The project uses domain-based organisation. Each new page should be placed in the appropriate domain folder:

```
app/
├── controllers/[domain]/          # e.g., monthlyreturns/
├── models/[domain]/               # e.g., monthlyreturns/
├── views/[domain]/                # e.g., monthlyreturns/
├── forms/[domain]/                # e.g., monthlyreturns/
├── pages/[domain]/                # e.g., monthlyreturns/
└── viewmodels/checkAnswers/[domain]/
```

```
test/
├── controllers/[domain]/
├── models/[domain]/
├── forms/[domain]/
└── views/[domain]/
```

### Example: routes and messages

```routes
GET        /there-is-a-problem-with-the-service                       controllers.SystemErrorController.onPageLoad()
```

Message key (messages.en):

```properties
monthlyreturns.inactivityRequest.title = Do you want to submit an inactivity request?
```

## Testing the agent journey locally

When an agent signs in, we don't just check they have an agent enrolment. 
We check they actually hold the client they're trying to view (`hasClient`). Getting that to pass locally trips people up, because the "which clients does this agent have" data lives across a few services, not in this repo. Here's how to set it up for MGD.

A couple of things worth knowing before you start:

- **The client reg number is the fiddly bit** - it has to be one that already exists as a client in the data (see "Finding a usable client" below). You can't just make up a valid-looking GRN. The examples here use `XWM00000001770`.
- **The delegation has to be seeded in enrolment-store-stub, not just in the login session.** The enrolments you add in the auth-login-stub wizard only go into the bearer token. The backend reads the agent's clients live from enrolment-store, so that's where the agent --> client link needs to be.
- Pick any agent credential id and use the *same one* everywhere. The examples below use `2095019333011821`.

### Finding a usable client reg number

This is what catches people out, so it's worth understanding. The `hasClient` check runs an Oracle query (`MGD_CLIENT_SEARCH.hasClient`) that joins three things:

- `mgd_agt_desig` / `mgd_agt_desig_list` - the agent's list of clients (this is what client-exchange writes for you in step 2), and
- `mgd_operator_details` - the client's actual MGD record.

So a reg number only works if it has a row in **`mgd_operator_details`**. Having it in the agent's list isn't enough - if there's no operator-details record, the join returns nothing and you get access denied. (Heads-up: the pre-loaded rows in `mgd_agt_desig_list` are *not* backed by operator-details, so don't just grab a reg number from there - it won't work.)

To find reg numbers you can actually use, list what's in operator-details:

```sql
select mgd_reg_number from mgd_data.mgd_operator_details;
```

Or look in the seed file that populates it: `prh-oracle-xe/databases/account-db/scripts/agent_auth_data/data/MGD_OPERATOR_DETAILS.sql`. Out of the box that's just `XWM00000001770` — if you need more clients, add operator-details rows there (and reload) or insert them directly.

The same principle applies to the GTR regimes (GBD/PBD/RGD): the client has to exist in the equivalent operator-details data (see `prh-oracle-xe/databases/gtr-db`), and you'd use `HMRC-GTS-GBD` etc. instead of `HMRC-MGD-ORG`.

**1. Seed the delegated enrolment** (agent → client) in enrolment-store-stub:

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

Check it took (should return the enrolment, not a 204):

```bash
curl "http://localhost:9595/enrolment-store-proxy/enrolment-store/users/2095019333011821/enrolments?type=delegated&service=HMRC-MGD-ORG&start-record=1&max-records=10"
```

**2. Pull the client list into the datacache** via client-exchange:

```bash
curl "http://localhost:9101/client-exchange-proxy/MGD/2095019333011821/12345/clientlist"
```

The client-exchange-proxy log should say `Updating 1 clients ... in onprem` (if it says 0, step 1 didn't take).

**3. Sign in as the agent** in auth-login-stub (Authority Wizard):

- CredID: `2095019333011821` (same as above)
- Affinity Group: `Agent`
- Redirect URL: `http://localhost:10403/manage-gambling-tax/statement/mgd/XWM00000001770/current`
- Add one enrolment: `HMRC-MGD-AGNT`, identifier `HMRCMGDAGENTREF` = `2095019333011821`, Activated

Submit, and you should land on the client's statement page.

### If it doesn't work

- **Access denied** means `hasClient` came back false. Check the data is really there:
  ```sql
  select count(*) from mgd_data.mgd_operator_details where mgd_reg_number = 'XWM00000001770';   -- expect 1
  ```
  If that's 0, the agent-auth data hasn't been loaded into Oracle.
- **"There is a problem"** means the client list was still downloading (or failed) when the page checked — give it a few seconds and refresh, or re-run step 2.
- The backend caches a `hasClient` result for about 2 minutes in memory, so if you changed the data and it's still wrong, restart the gambling backend.
- If in doubt, make sure the same credential id is used in the seed, the client-exchange call, and the login.

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

