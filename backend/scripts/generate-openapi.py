"""Export backend's explicit command records/controller annotations without a server.

Run with --check in CI to detect route/command contract drift. Read model schemas are
maintained here because JDBC projection maps deliberately avoid persistence entities.
Only Python's standard library is required.
"""
from pathlib import Path
import argparse
import json
import re

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/com/pandora6ix/backend"


def split_fields(text):
    level = 0
    start = 0
    parts = []
    for i, char in enumerate(text):
        if char in "(<":
            level += 1
        elif char in ")>":
            level -= 1
        elif char == "," and level == 0:
            parts.append(text[start:i].strip())
            start = i + 1
    parts.append(text[start:].strip())
    return [p for p in parts if p]


def ref(name):
    return {"$ref": f"#/components/schemas/{name}"}


def array(item):
    return {"type": "array", "items": item}


def obj(properties, required=()):
    result = {"type": "object", "properties": properties}
    if required:
        result["required"] = list(required)
    return result


STRING = {"type": "string"}
ID = {"type": "string", "format": "uuid"}
INTEGER = {"type": "integer", "format": "int64"}
BOOLEAN = {"type": "boolean"}
DATE = {"type": "string", "format": "date"}
INSTANT = {"type": "string", "format": "date-time"}


def nullable(schema):
    return {"anyOf": [schema, {"type": "null"}]}


def java_type(name):
    name = name.replace("Contracts.", "")
    if name.startswith("List<"):
        return array(java_type(name[5:-1]))
    if name == "String":
        return dict(STRING)
    if name in ("int", "Integer", "Long", "long"):
        return dict(INTEGER)
    if name == "LocalDate":
        return dict(DATE)
    if name in ("TaskKind", "TimeKind"):
        enum_source = (JAVA / f"domain/task/{name}.java").read_text(encoding="utf-8")
        return {"type": "string", "enum": re.findall(r"\b[A-Z][A-Z_]+\b", enum_source)}
    return ref(name)


def command_schemas():
    source = (JAVA / "api/Contracts.java").read_text(encoding="utf-8")
    schemas = {}
    for match in re.finditer(r"public record (\w+)\(", source):
        start = match.end()
        end = start
        level = 1
        while level:
            if source[end] == "(":
                level += 1
            elif source[end] == ")":
                level -= 1
            end += 1
        properties = {}
        required = []
        for field in split_fields(source[start:end - 1]):
            cleaned = re.sub(r'@\w+(?:\((?:[^"()]|"(?:\\.|[^"\\])*")*\))?\s*', "", field).strip()
            field_type, field_name = cleaned.rsplit(" ", 1)
            schema = java_type(field_type)
            outer_annotations = field.split("List<", 1)[0]
            mandatory = any(annotation in outer_annotations for annotation in ("@NotNull", "@NotBlank", "@NotEmpty"))
            if "@NotBlank" in outer_annotations:
                schema.update(minLength=1, pattern=r"\S")
            if "@NotEmpty" in outer_annotations and field_type.startswith("List<"):
                schema["minItems"] = 1
            if "List<@NotBlank" in field:
                schema["items"].update(minLength=1, pattern=r"\S")
            for annotation, keyword in (("Min", "minimum"), ("Max", "maximum")):
                value = re.search(rf"@{annotation}\((\d+)\)", field)
                if value:
                    schema[keyword] = int(value[1])
            size = re.search(r"@Size\(([^)]+)\)", field)
            if size:
                for side, amount in re.findall(r"(min|max)\s*=\s*(\d+)", size[1]):
                    schema[f'{side}{"Items" if field_type.startswith("List<") else "Length"}'] = int(amount)
            pattern = re.search(r'@Pattern\(regexp\s*=\s*"([^"]+)"\)', field)
            if pattern:
                schema["pattern"] = "^(?:" + pattern[1] + ")$"
            if "password" in field_name.lower():
                schema["writeOnly"] = True
            properties[field_name] = schema if mandatory else nullable(schema)
            if mandatory:
                required.append(field_name)
        schemas[match[1]] = obj(properties, required)
        schemas[match[1]]["additionalProperties"] = False
    return schemas


def read_schemas():
    versioned = {"id": ID, "version": INTEGER}
    tag = obj({**versioned, "name": STRING, "colorHex": STRING, "creatorUserId": ID})
    assignment = obj({**versioned, "taskId": ID, "recipientUserId": ID,
        "executionStatus": {"type": "string", "enum": ["PENDING_APPROVAL", "ACTIVE", "AWAITING_ACCEPTANCE", "COMPLETED"]},
        "progressPercent": {"type": "integer", "minimum": 0, "maximum": 100},
        "activatedAt": nullable(INSTANT), "completionSource": nullable({"type": "string", "enum": ["SELF", "CREATOR_ACCEPTANCE"]}),
        "completedByUserId": nullable(ID), "completedAt": nullable(INSTANT), "allowedActions": array(STRING)})
    progress = obj({"id": ID, "assignmentId": ID, "actorUserId": ID, "progressPercent": INTEGER, "note": nullable(STRING), "recordedAt": INSTANT})
    completion = obj({**versioned, "assignmentId": ID, "requestedByUserId": ID, "submittedNote": nullable(STRING), "requestedAt": INSTANT,
        "decision": {"type": "string", "enum": ["PENDING", "ACCEPTED", "REJECTED"]}, "decidedByUserId": nullable(ID), "decisionNote": nullable(STRING), "decidedAt": nullable(INSTANT)})
    task = obj({**versioned, "taskId": ID, "creatorUserId": ID, "kind": java_type("TaskKind"), "title": STRING, "description": nullable(STRING),
        "priorityLevel": {"type": "integer", "minimum": 1, "maximum": 5}, "startDate": DATE, "endDate": DATE,
        "timeKind": java_type("TimeKind"), "startTime": nullable(STRING), "endTime": nullable(STRING), "reviewStatus": {"type": "string", "enum": ["PENDING", "APPROVED", "REJECTED"]},
        "primaryTagId": nullable(ID), "tags": array(ref("Tag")), "tagIds": array(ID), "displayColor": nullable(STRING), "createdAt": INSTANT,
        "authorizedAssignments": array(ref("Assignment")), "allowedActions": array(STRING), "reviewId": nullable(ID),
        "progressRecords": array(ref("ProgressRecord")), "completionRequests": array(ref("CompletionRequestView"))})
    entry = obj({**versioned, "entryId": ID, "journalId": ID, "workLogId": ID, "workDate": DATE, "authorUserId": ID,
        "clientEntryKey": STRING, "content": STRING, "displayOrder": INTEGER, "recordedAt": INSTANT, "eligible": BOOLEAN})
    journal = obj({**versioned, "journalId": ID, "authorUserId": ID, "workDate": DATE, "publishedAt": INSTANT,
        "businessToday": DATE, "allowedActions": array({"type": "string", "enum": ["EDIT", "DELETE"]}),
        "primaryTagId": nullable(ID), "entries": array(ref("WorkEntry")), "tags": array(ref("Tag")), "tagIds": array(ID)})
    enterprise = obj({**versioned, "name": STRING, "loginDomain": STRING, "businessTimezone": nullable(STRING), "status": STRING,
        "createdAt": INSTANT, "rootOrgUnitId": ID, "adminUserId": ID})
    org = obj({**versioned, "name": STRING, "kind": {"type": "string", "enum": ["COMPANY", "DEPARTMENT", "TEAM"]}, "parentId": nullable(ID)})
    user = obj({**versioned, "displayName": STRING, "loginName": STRING, "orgUnitId": nullable(ID), "supervisorUserId": nullable(ID),
        "jobTitle": nullable(STRING), "status": STRING, "roles": array(STRING),
        "gender": nullable({"type": "string", "enum": ["MALE", "FEMALE"]}), "idNumber": nullable(STRING), "phoneNumber": nullable(STRING)},
        ("id", "version", "displayName", "loginName", "roles", "gender", "idNumber", "phoneNumber"))
    profile = obj({**versioned, "displayName": STRING, "name": STRING, "orgUnitId": nullable(ID), "supervisorUserId": nullable(ID),
        "jobTitle": nullable(STRING), "status": STRING, "roles": array(STRING), "businessTimezone": nullable(STRING), "department": nullable(STRING),
        "organization": nullable(ref("OrgUnit")), "enterprise": ref("Enterprise"),
        "capabilities": obj({name: BOOLEAN for name in ("admin", "business", "manage", "createPersonalTasks", "createDelegatedTasks", "reviewTasks", "createTags", "publishCompanyItems")})})
    item = obj({**versioned, "itemId": ID, "publisherUserId": ID, "title": STRING, "content": STRING, "displayOrder": INTEGER, "createdAt": INSTANT})
    selection = obj({"entries": array(ref("WorkEntry")), "hasManualSelection": BOOLEAN, "requiresSelection": BOOLEAN, "candidateCount": INTEGER, "version": INTEGER})
    stats = obj({"total": INTEGER, "completed": INTEGER, "uncompleted": INTEGER, "basis": {"type": "string", "const": "OWN_APPROVED_ASSIGNMENTS"}})
    journalDeleted = obj({"journalId": ID})
    tagDeleted = obj({"tagId": ID})
    review = obj({**versioned, "reviewId": ID, "taskId": ID, "reviewerUserId": ID, "submittedByUserId": ID,
        "taskVersion": INTEGER, "currentTaskVersion": INTEGER, "attemptNo": INTEGER, "submittedAt": INSTANT,
        "submittedSnapshot": {"type": "object", "additionalProperties": True},
        "decision": {"type": "string", "enum": ["PENDING", "APPROVED", "REJECTED"]},
        "reviewStatus": {"type": "string", "enum": ["PENDING", "APPROVED", "REJECTED"]},
        "decidedByUserId": nullable(ID), "decisionNote": nullable(STRING), "decidedAt": nullable(INSTANT),
        "isCurrent": BOOLEAN, "allowedActions": array({"type": "string", "enum": ["APPROVE", "REJECT"]})})
    schemas = {"Tag": tag, "Assignment": assignment, "ProgressRecord": progress, "CompletionRequestView": completion, "Task": task,
        "WorkEntry": entry, "Journal": journal, "Enterprise": enterprise, "OrgUnit": org, "User": user, "Profile": profile, "CompanyItem": item, "ImportantSelection": selection, "Statistics": stats,
        "JournalDeleted": journalDeleted, "TagDeleted": tagDeleted,
        "TaskReview": review, "ReviewDecisionResult": obj({"review": ref("TaskReview"), "task": ref("Task")}),
        "Recipient": obj({"id": ID, "displayName": STRING, "orgUnitId": nullable(ID), "jobTitle": nullable(STRING), "organizationName": nullable(STRING), "departmentId": nullable(ID), "departmentName": nullable(STRING), "relationship": STRING}),
        "LoginResult": obj({"accessToken": STRING, "tokenType": {"type": "string", "const": "Bearer"}, "expiresAt": INSTANT, "user": ref("Profile")}),
        "ProgressResult": obj({"record": ref("ProgressRecord"), "assignment": ref("Assignment")}),
        "CompletionResult": obj({"request": ref("CompletionRequestView"), "assignment": ref("Assignment")}),
        "Calendar": obj({"mode": {"type": "string", "enum": ["day", "week", "month"]}, "anchorDate": DATE, "rangeStart": DATE, "rangeEnd": DATE,
            "today": DATE, "businessTimezone": STRING, "tasks": array(ref("Task")), "nextCursor": nullable(STRING), "totalCount": INTEGER}),
        "Error": obj({"error": obj({"code": STRING, "message": STRING, "fieldErrors": array(obj({"field": STRING, "message": STRING})), "existingJournalId": ID}, ("code", "message", "fieldErrors")),
            "requestId": STRING, "timestamp": INSTANT, "status": INTEGER, "code": STRING, "message": STRING, "path": STRING}, ("error", "requestId"))}
    for name in ("Tag", "Task", "Journal", "WorkEntry", "CompanyItem", "OrgUnit", "User", "Recipient", "TaskReview"):
        schemas[name + "Page"] = obj({"items": array(ref(name)), "nextCursor": nullable(STRING), "moreUrl": STRING}, ("items", "nextCursor"))
    schemas["Dashboard"] = obj({"companyImportantItems": ref("CompanyItemPage"), "companyTasks": ref("TaskPage"), "personalImportantEntries": ref("ImportantSelection"), "workEntries": ref("WorkEntryPage"), "statistics": ref("Statistics")})
    return schemas


RESPONSES = {
    ("POST", "/auth/login"): "LoginResult", ("GET", "/me"): "Profile", ("POST", "/admin/enterprise-initializations"): "Enterprise",
    ("GET", "/admin/enterprise"): "Enterprise", ("PATCH", "/admin/enterprise"): "Enterprise",
    ("GET", "/admin/org-units"): "OrgUnitPage", ("POST", "/admin/org-units"): "OrgUnit", ("PATCH", "/admin/org-units/{id}"): "OrgUnit",
    ("GET", "/admin/users"): "UserPage", ("POST", "/admin/users"): "User", ("PATCH", "/admin/users/{id}"): "User",
    ("POST", "/admin/users/{id}/password-reset"): "User",
    ("GET", "/tags"): "TagPage", ("POST", "/tags"): "Tag", ("PATCH", "/tags/{id}"): "Tag",
    ("GET", "/tasks/recipient-candidates"): "RecipientPage", ("GET", "/tasks"): "TaskPage", ("POST", "/tasks"): "Task", ("GET", "/tasks/{id}"): "Task", ("PATCH", "/tasks/{id}"): "Task",
    ("POST", "/tasks/{id}/complete"): "Assignment", ("POST", "/task-assignments/{id}/progress-records"): "ProgressResult",
    ("POST", "/task-assignments/{id}/completion-requests"): "CompletionResult", ("POST", "/task-completion-requests/{id}/decision"): "CompletionResult",
    ("GET", "/task-reviews"): "TaskReviewPage", ("GET", "/task-reviews/{id}"): "TaskReview", ("POST", "/task-reviews/{id}/decision"): "ReviewDecisionResult",
    ("GET", "/calendar"): "Calendar", ("GET", "/work-logs"): "JournalPage", ("POST", "/work-logs"): "Journal", ("GET", "/work-logs/{id}"): "Journal", ("PATCH", "/work-logs/{id}"): "Journal", ("DELETE", "/work-logs/{id}"): "JournalDeleted",
    ("DELETE", "/tags/{id}"): "TagDeleted",
    ("GET", "/work-log-entries/{id}"): "WorkEntry", ("GET", "/me/important-entry-candidates"): "WorkEntryPage", ("GET", "/me/important-entries"): "ImportantSelection", ("PUT", "/me/important-entries"): "ImportantSelection",
    ("GET", "/company-important-items"): "CompanyItemPage", ("POST", "/company-important-items"): "CompanyItem", ("GET", "/company-important-items/{id}"): "CompanyItem", ("PATCH", "/company-important-items/{id}"): "CompanyItem",
    ("GET", "/dashboard"): "Dashboard", ("GET", "/dashboard/task-items"): "TaskPage", ("GET", "/statistics"): "Statistics",
}


def generate():
    document = {"openapi": "3.1.0", "info": {"title": "Pandora6ix confirmed backend APIs", "version": "0.2.0",
        "description": "Implementation on backend-syz, including confirmed direct-supervisor task review. Reserved inbox and unresolved Q-01–09 are excluded. Read backend/ANDROID_INTEGRATION.md for current Android contracts."},
        "servers": [{"url": "/api/v1"}], "security": [{"bearerAuth": []}], "paths": {},
        "components": {"securitySchemes": {"bearerAuth": {"type": "http", "scheme": "bearer"}, "bootstrapSecret": {"type": "apiKey", "in": "header", "name": "X-Bootstrap-Token"}},
            "schemas": {**command_schemas(), **read_schemas()}}}
    count = 0
    for file in sorted((JAVA / "controller").glob("*Controller.java")):
        if file.name == "HealthController.java":
            continue
        source = file.read_text(encoding="utf-8")
        for method in re.finditer(r'@(Get|Post|Patch|Put|Delete)Mapping\("([^"]+)"\)\s*(?:@ResponseStatus\(([^)]+)\)\s*)?public\s+\S+\s+(\w+)\((.*?)\)\s*\{', source, re.S):
            verb, path, status, name, params = method.groups()
            verb = verb.upper()
            no_body = (verb, path) in (("POST", "/auth/logout"), ("POST", "/me/password"), ("DELETE", "/admin/org-units/{id}"))
            status_code = "204" if no_body else "201" if status and "CREATED" in status else "200"
            operation = {"operationId": file.stem + "_" + name, "tags": [file.stem.replace("Controller", "")], "parameters": [], "responses": {}}
            if path == "/auth/login":
                operation["security"] = []
            if path == "/admin/enterprise-initializations":
                operation["security"] = [{"bootstrapSecret": []}]
            for parameter in split_fields(params):
                command = re.search(r"@RequestBody\s+Contracts\.(\w+)", parameter)
                if command:
                    operation["requestBody"] = {"required": True, "content": {"application/json": {"schema": ref(command[1])}}}
                    continue
                if "@PathVariable" in parameter:
                    field_name = parameter.rsplit(" ", 1)[1]
                    operation["parameters"].append({"name": field_name, "in": "path", "required": True, "schema": STRING})
                elif "@RequestParam" in parameter or "@RequestHeader" in parameter:
                    header = "@RequestHeader" in parameter
                    cleaned = re.sub(r"@\w+(?:\([^)]*\))?\s*", "", parameter)
                    field_type, field_name = cleaned.strip().rsplit(" ", 1)
                    override = re.search(r'value\s*=\s*"([^"]+)"', parameter)
                    default = re.search(r'defaultValue\s*=\s*"([^"]+)"', parameter)
                    schema = java_type(field_type)
                    if default:
                        schema["default"] = int(default[1]) if field_type == "int" else default[1]
                    if field_name == "limit":
                        schema.update(minimum=1, maximum=100)
                    operation["parameters"].append({"name": override[1] if override else field_name, "in": "header" if header else "query",
                        "required": re.search(r"required\s*=\s*false", parameter) is None and default is None, "schema": schema})
            success = {"description": "Successful command/read; 201 task submission remains PENDING."}
            if not no_body:
                response = RESPONSES[(verb, path)]
                success["content"] = {"application/json": {"schema": obj({"data": ref(response), "requestId": STRING}, ("data", "requestId"))}}
            operation["responses"][status_code] = success
            for error in (400, 401, 403, 404, 409, 422, 500):
                operation["responses"][str(error)] = {"description": "Structured error; unavailable private objects use uniform 404.", "content": {"application/json": {"schema": ref("Error")}}}
            document["paths"].setdefault(path, {})[verb.lower()] = operation
            count += 1
    if count != len(RESPONSES) + 3:
        raise RuntimeError(f"Controller parser omitted routes: {count} vs {len(RESPONSES) + 3}")
    # Resolve every local schema reference before exporting; JSON syntax alone is insufficient.
    stack = [document]
    while stack:
        value = stack.pop()
        if isinstance(value, dict):
            if "$ref" in value:
                reference = value["$ref"]
                target = document
                if not reference.startswith("#/"):
                    raise RuntimeError(f"Unexpected external schema reference: {reference}")
                for key in reference[2:].split("/"):
                    target = target[key]
            stack.extend(value.values())
        elif isinstance(value, list):
            stack.extend(value)
    return json.dumps(document, ensure_ascii=False, indent=2) + "\n", count


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    text, count = generate()
    destination = ROOT / "openapi.json"
    if args.check:
        if not destination.exists() or destination.read_text(encoding="utf-8") != text:
            raise SystemExit("openapi.json is stale; run scripts/generate-openapi.py")
    else:
        destination.write_text(text, encoding="utf-8")
    print(f"OpenAPI contract: {count} operations; {'verified' if args.check else 'exported'}.")
