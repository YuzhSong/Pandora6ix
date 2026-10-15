"""Real HTTP acceptance for Android contracts; isolated file H2, no SQL approval fixtures.

Build the backend first, then run with Python 3: python scripts/android-smoke.py.
JAVA_HOME selects Java 21. Credentials exist only in this process and are never printed.
"""
import json
import os
from pathlib import Path
import shutil
import socket
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid


ROOT = Path(__file__).resolve().parents[1]
JAR = ROOT / "target/backend-0.0.1-SNAPSHOT.jar"


def main():
    if not JAR.is_file():
        raise SystemExit("Build backend with mvnw package first.")
    java = str(Path(os.environ["JAVA_HOME"]) / "bin/java.exe") if os.name == "nt" and os.environ.get("JAVA_HOME") else (
        str(Path(os.environ["JAVA_HOME"]) / "bin/java") if os.environ.get("JAVA_HOME") else shutil.which("java"))
    if not java:
        raise SystemExit("Java 21 is required.")
    run = ROOT / "target/android-smoke" / str(uuid.uuid4())
    run.mkdir(parents=True)
    with socket.socket() as probe:
        probe.bind(("127.0.0.1", 0))
        port = probe.getsockname()[1]
    origin = f"http://127.0.0.1:{port}"
    bootstrap, password = str(uuid.uuid4()), str(uuid.uuid4())
    env = {**os.environ, "PANDORA_DB_URL": "jdbc:h2:file:./pandora;DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0",
           "PANDORA_DB_USER": "sa", "PANDORA_DB_PASSWORD": "", "PANDORA_BOOTSTRAP_TOKEN": bootstrap}
    server = None
    logs = []
    calls = 0

    def start(v1=False):
        nonlocal server
        log = (run / f"server-{len(logs) + 1}.log").open("wb")
        logs.append(log)
        args = [java, "-jar", str(JAR), "--server.address=127.0.0.1", f"--server.port={port}"]
        if v1:
            args.append("--spring.flyway.target=1")
        server = subprocess.Popen(args, cwd=run, env=env, stdout=log, stderr=subprocess.STDOUT,
                                  creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0)
        deadline = time.monotonic() + 60
        while time.monotonic() < deadline:
            if server.poll() is not None:
                raise RuntimeError(f"Backend exited; inspect {run}")
            try:
                with urllib.request.urlopen(origin + "/health", timeout=2) as response:
                    if json.load(response)["status"] == "UP":
                        return
            except (OSError, ValueError):
                pass
            time.sleep(.25)
        raise RuntimeError(f"Backend startup timeout; inspect {run}")

    def stop():
        nonlocal server
        if server is not None and server.poll() is None:
            server.terminate()
            try:
                server.wait(timeout=15)
            except subprocess.TimeoutExpired:
                server.kill()
                server.wait(timeout=10)
        server = None

    def api(method, path, token=None, body=None, expected=200, headers=None):
        nonlocal calls
        calls += 1
        request_headers = {"Content-Type": "application/json", **(headers or {})}
        if token:
            request_headers["Authorization"] = "Bearer " + token
        request = urllib.request.Request(origin + "/api/v1" + path, method=method, headers=request_headers,
                                         data=None if body is None else json.dumps(body).encode())
        try:
            with urllib.request.urlopen(request, timeout=15) as response:
                status, raw = response.status, response.read()
        except urllib.error.HTTPError as error:
            status, raw = error.code, error.read()
        parsed = json.loads(raw) if raw else {}
        assert status == expected, f"{method} {path}: expected {expected}, got {status}, code={parsed.get('error', {}).get('code')}"
        if expected == 204:
            return None
        assert parsed.get("requestId"), "Missing requestId"
        return parsed["data" if status < 400 else "error"]

    def pages(path, token, key="items", limit=2):
        rows, seen = [], set()
        cursor = None
        while True:
            suffix = "&" if "?" in path else "?"
            page = api("GET", path + suffix + f"limit={limit}" + ("&cursor=" + urllib.parse.quote(cursor) if cursor else ""), token)
            rows.extend(page[key])
            cursor = page.get("nextCursor")
            if cursor is None:
                return rows
            assert cursor not in seen, "Repeated cursor"
            seen.add(cursor)

    try:
        # Persist real business data in a genuine V1-only database before applying V2.
        start(v1=True)
        domain = "android-" + str(uuid.uuid4())
        initial = api("POST", "/admin/enterprise-initializations", body={"loginDomain": domain,
                      "name": "Android HTTP acceptance", "businessTimezone": "Asia/Shanghai",
                      "adminLoginName": "admin", "adminPassword": password, "adminDisplayName": "Admin"},
                      headers={"X-Bootstrap-Token": bootstrap}, expected=201)

        def login(name, secret=password):
            return api("POST", "/auth/login", body={"enterpriseDomain": domain, "loginName": name, "password": secret})["accessToken"]

        admin = login("admin")
        root = initial["rootOrgUnitId"]
        department = api("POST", "/admin/org-units", admin,
                         {"name": "Department", "kind": "DEPARTMENT", "parentId": root}, 201)["id"]
        team = api("POST", "/admin/org-units", admin,
                   {"name": "Team", "kind": "TEAM", "parentId": department}, 201)["id"]

        def user(name, role, organization, supervisor=None):
            return api("POST", "/admin/users", admin, {"loginName": name, "password": password,
                       "displayName": name, "orgUnitId": organization, "supervisorUserId": supervisor,
                       "jobTitle": "Developer", "roles": [role]}, 201)["id"]

        founder_id = user("founder", "FOUNDER", root)
        manager_id = user("manager", "DEPARTMENT_MANAGER", department, founder_id)
        lead_id = user("lead", "TEAM_LEAD", team, manager_id)
        employee_id = user("employee", "EMPLOYEE", team, lead_id)
        peer_id = user("peer", "EMPLOYEE", team, lead_id)
        manager, lead, employee, peer = [login(name) for name in ("manager", "lead", "employee", "peer")]
        profile = api("GET", "/me", employee)
        assert profile["id"] == employee_id and profile["businessTimezone"] == "Asia/Shanghai"
        assert profile["capabilities"]["createPersonalTasks"] and not profile["capabilities"]["createDelegatedTasks"]
        assert profile["department"] == "Department" and profile["organization"]["id"] == team
        tag1 = api("POST", "/tags", manager, {"name": "Android", "colorHex": "#123456"}, 201)["id"]
        tag2 = api("POST", "/tags", manager, {"name": "Review", "colorHex": "#654321"}, 201)["id"]
        api("POST", "/tags", employee, {"name": "Denied", "colorHex": "#123456"}, 403)
        assert {x["id"] for x in pages("/tags", employee)} == {tag1, tag2}
        today = api("GET", "/calendar?mode=day&anchorDate=2000-01-01", employee)["today"]
        company = api("POST", "/company-important-items", admin,
                      {"title": "Company", "content": "Full company content", "displayOrder": 1}, 201)
        journal_body = {"workDate": today, "entries": [{"clientEntryKey": f"entry-{i}", "content": f"Full work entry {i}"}
                        for i in range(11)], "tagIds": [tag1], "primaryTagId": tag1}
        journal = api("POST", "/work-logs", employee, journal_body, 201, {"Idempotency-Key": "journal"})
        assert api("POST", "/work-logs", employee, journal_body, 201, {"Idempotency-Key": "journal"}) == journal
        duplicate = api("POST", "/work-logs", employee, journal_body, 409)
        assert duplicate["existingJournalId"] == journal["id"]
        assert api("GET", "/me/important-entries", employee)["requiresSelection"]
        api("PUT", "/me/important-entries", employee, {"expectedVersion": 0, "entryIds": []})

        def task(kind, recipients, tags):
            return {"kind": kind, "title": kind + " real task", "description": "Full task content",
                    "priorityLevel": 3, "timeKind": "ALL_DAY", "startDate": today, "endDate": today,
                    "recipientIds": recipients, "tagIds": tags, "primaryTagId": tags[0] if tags else None}

        personal = api("POST", "/tasks", employee, task("PERSONAL", [], [tag1]), 201)
        delegated = api("POST", "/tasks", lead, task("DELEGATED", [employee_id, peer_id], [tag1, tag2]), 201)
        rejected = api("POST", "/tasks", employee, task("PERSONAL", [], []), 201)
        assert personal["reviewStatus"] == "PENDING" and pages("/tasks", employee) == []
        stop()
        start()  # Flyway upgrades persisted V1 data to V2; approved states come only from APIs below.
        assert api("GET", "/work-logs/" + journal["id"], employee)["entries"][0]["entryId"] == journal["entries"][0]["entryId"]
        assert api("GET", "/me/important-entries", employee)["hasManualSelection"]
        assert len(pages("/task-reviews", lead)) == 2
        assert len(pages("/task-reviews", manager)) == 1
        original_review = personal["reviewId"]
        pending_patch = task("PERSONAL", [], [tag1])
        pending_patch.pop("kind")
        pending_patch.pop("recipientIds")
        pending_patch.update(expectedVersion=0, title="Edited pending personal task")
        personal = api("PATCH", "/tasks/" + personal["id"], employee, pending_patch)
        latest_review = api("GET", "/task-reviews/" + personal["reviewId"], lead)
        assert latest_review["submittedSnapshot"]["recipientIds"] == [employee_id]
        assert latest_review["submittedSnapshot"]["title"] == "Edited pending personal task"
        api("POST", "/task-reviews/" + original_review + "/decision", lead,
            {"expectedVersion": 0, "decision": "APPROVE"}, 409)
        assert len(pages("/task-reviews?scope=created", employee)) == 3

        def decision(task_row, reviewer, value):
            return api("POST", "/task-reviews/" + task_row["reviewId"] + "/decision", reviewer,
                       {"expectedVersion": 0, "decision": value, "comment": "HTTP reviewed"},
                       headers={"Idempotency-Key": "decision-" + task_row["reviewId"]})

        approved = decision(personal, lead, "APPROVE")
        assert decision(personal, lead, "APPROVE") == approved
        decision(delegated, manager, "APPROVE")
        decision(rejected, lead, "REJECT")
        assert api("GET", "/task-reviews/" + rejected["reviewId"], employee)["decisionNote"] == "HTTP reviewed"
        assert len(pages("/tasks", employee)) == 2
        assert len(pages("/tasks?from=" + today + "&to=" + today, employee)) == 2
        calendar = api("GET", "/calendar?mode=day&anchorDate=" + today + "&tagIds=" + tag1 + "," + tag2, employee)
        assert [x["id"] for x in calendar["tasks"]] == [delegated["id"]]
        for mode in ("day", "week", "month"):
            calendar_rows = pages(f"/calendar?mode={mode}&anchorDate={today}", employee, "tasks", limit=1)
            assert {x["id"] for x in calendar_rows} == {personal["id"], delegated["id"]}
        assert api("GET", "/tasks/" + delegated["id"], employee)["displayColor"] == "#123456"

        personal_view = api("GET", "/tasks/" + personal["id"], employee)
        assignment = personal_view["authorizedAssignments"][0]
        assert "COMPLETE_PERSONAL" in assignment["allowedActions"]
        progress = api("POST", "/task-assignments/" + assignment["id"] + "/progress-records", employee,
                       {"expectedVersion": assignment["version"], "progressPercent": 100}, 201)
        assert progress["assignment"]["executionStatus"] == "ACTIVE"
        assert api("GET", "/statistics", employee)["completed"] == 0
        completed = api("POST", "/tasks/" + personal["id"] + "/complete", employee,
                        {"expectedAssignmentVersion": progress["assignment"]["version"]})
        assert completed["completionSource"] == "SELF"
        relation = api("GET", "/tasks/" + delegated["id"], employee)["authorizedAssignments"][0]
        requested = api("POST", "/task-assignments/" + relation["id"] + "/completion-requests", employee,
                        {"expectedVersion": relation["version"], "note": "Ready"}, 201)
        request_id = requested["request"]["id"]
        api("POST", "/task-completion-requests/" + request_id + "/decision", manager,
            {"expectedVersion": 0, "decision": "ACCEPT"}, 404)
        refused = api("POST", "/task-completion-requests/" + request_id + "/decision", lead,
                      {"expectedVersion": 0, "decision": "REJECT", "comment": "One more check"})
        requested = api("POST", "/task-assignments/" + relation["id"] + "/completion-requests", employee,
                        {"expectedVersion": refused["assignment"]["version"]}, 201)
        accepted = api("POST", "/task-completion-requests/" + requested["request"]["id"] + "/decision", lead,
                       {"expectedVersion": requested["request"]["version"], "decision": "ACCEPT"})
        assert accepted["assignment"]["completionSource"] == "CREATOR_ACCEPTANCE"
        assert api("GET", "/tasks/" + delegated["id"], peer)["authorizedAssignments"][0]["executionStatus"] == "ACTIVE"

        first_id = journal["entries"][0]["entryId"]
        patch = {"expectedVersion": 0, "upsertEntries": [{"entryId": first_id, "content": "Edited full entry"},
                 {"clientEntryKey": "supplement", "content": "Supplemented work"}]}
        edited = api("PATCH", "/work-logs/" + journal["id"], employee, patch)
        assert len(edited["entries"]) == 12 and edited["entries"][0]["entryId"] == first_id
        api("PATCH", "/work-logs/" + journal["id"], employee, patch, 409)
        assert api("GET", "/work-log-entries/" + first_id, employee)["content"] == "Edited full entry"
        assert len(pages("/work-logs?date=" + today, employee)) == 1
        assert len(pages("/work-logs?scope=managed&date=" + today, lead)) == 1
        candidates = pages("/me/important-entry-candidates", employee)
        assert len(candidates) == 12
        selected = api("PUT", "/me/important-entries", employee,
                       {"expectedVersion": 1, "entryIds": [x["entryId"] for x in candidates[:10]]})
        assert len(selected["entries"]) == 10
        api("PUT", "/me/important-entries", employee,
            {"expectedVersion": 2, "entryIds": [x["entryId"] for x in candidates[:11]]}, 422)
        api("PUT", "/me/important-entries", employee, {"expectedVersion": 2, "entryIds": []})
        dashboard = api("GET", "/dashboard", employee)
        assert dashboard["companyImportantItems"]["items"][0]["itemId"] == company["id"]
        assert dashboard["companyTasks"]["items"][0]["taskId"] == delegated["id"]
        assert len(dashboard["workEntries"]["items"]) == 12 and dashboard["personalImportantEntries"]["entries"] == []
        assert api("GET", "/company-important-items/" + company["id"], employee)["content"] == "Full company content"
        assert len(pages("/dashboard/task-items", employee)) == 1
        stats = api("GET", "/statistics", employee)
        assert stats == {"total": 2, "completed": 2, "uncompleted": 0, "basis": "OWN_APPROVED_ASSIGNMENTS"}
        stop()
        start()  # Verify V2 decisions, completion facts, stable entries, sessions and manual empty state.
        assert api("GET", "/statistics", employee) == stats
        assert api("GET", "/task-reviews/" + personal["reviewId"], lead)["decision"] == "APPROVED"
        assert api("GET", "/work-logs/" + journal["id"], employee)["entries"][0]["entryId"] == first_id
        selection = api("GET", "/me/important-entries", employee)
        assert selection["hasManualSelection"] and selection["entries"] == []
        second_session = login("employee")
        new_password = str(uuid.uuid4())
        api("POST", "/me/password", employee, {"currentPassword": "wrong", "newPassword": new_password}, 403)
        api("POST", "/me/password", employee, {"currentPassword": password, "newPassword": new_password}, 204)
        api("GET", "/me", employee, expected=401)
        api("GET", "/me", second_session)
        api("POST", "/auth/logout", second_session, expected=204)
        api("GET", "/me", second_session, expected=401)
        changed_session = login("employee", new_password)
        assert api("GET", "/statistics", changed_session) == stats
        api("POST", "/auth/logout", changed_session, expected=204)
        report = {"passed": True, "httpCalls": calls, "modules": 10, "processStarts": 3,
                  "v1ToV2Upgrade": True, "sqlApprovalFixtures": False,
                  "checks": ["login/profile/password/logout", "dashboard/full details", "real recipients and multi-recipient submission",
                             "direct-supervisor approval/rejection", "calendar day/week/month, ranges, AND tags, pagination",
                             "journal publish/supplement/edit/conflict", "entry selection <=10 and persistent empty set",
                             "progress 100 stays active", "personal completion and per-recipient acceptance",
                             "tag IDs/primary color/role checks", "own completed facts", "restart persistence"]}
        # Confirm candidate search returns real IDs, separate from task submission authorization.
        assert {x["id"] for x in pages("/tasks/recipient-candidates?q=employee", lead)} == {employee_id}
        report["httpCalls"] = calls
        (run / "result.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
        print(json.dumps(report))
    finally:
        stop()
        for log in logs:
            log.close()


if __name__ == "__main__":
    main()
