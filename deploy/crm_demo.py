"""Start an isolated localhost backend and provision fictional CRM users."""
import json
import os
from pathlib import Path
import secrets
import subprocess
import time
from urllib.error import HTTPError, URLError
from urllib.request import ProxyHandler, Request, build_opener
from uuid import uuid4

API = "http://127.0.0.1:19082"
REPO = Path(__file__).resolve().parents[1]
RUNTIME = Path.home() / ".pandora6ix-dev2" / "run"
HTTP = build_opener(ProxyHandler({}))


def request(path, data=None, headers=None):
    body = json.dumps(data).encode("utf-8") if data is not None else None
    req = Request(API + path, data=body, headers={"Content-Type": "application/json", **(headers or {})})
    with HTTP.open(req, timeout=5) as response:
        if response.status == 204:
            return None
        result = json.load(response)
        return result.get("data", result)


def demo_access():
    path = RUNTIME / "demo-access.json"
    if not path.exists():
        access = {
            "domain": "crm-demo-" + uuid4().hex[:8],
            "handoff": secrets.token_bytes(32).hex(),
            "bootstrap": secrets.token_hex(32),
            "accounts": {name: secrets.token_urlsafe(24) for name in ("admin", "staff", "observer")},
        }
        with path.open("x", encoding="utf-8") as output:
            json.dump(access, output, indent=2)
    return json.loads(path.read_text(encoding="utf-8"))


def provision(access, bootstrap):
    domain, accounts = access["domain"], access["accounts"]
    login = {"accountIdentifier": "admin@" + domain, "password": accounts["admin"]}
    try:
        admin = request("/api/v1/auth/login", login)
    except HTTPError as error:
        if error.code != 401 or not bootstrap:
            raise
        request("/api/v1/admin/enterprise-initializations", {
            "loginDomain": domain, "name": "星澜演示企业", "businessTimezone": "Asia/Taipei",
            "adminLoginName": "admin", "adminPassword": accounts["admin"],
            "adminDisplayName": "CRM 演示管理员",
        }, {"X-Bootstrap-Token": bootstrap, "Idempotency-Key": uuid4().hex})
        admin = request("/api/v1/auth/login", login)

    headers = {"Authorization": "Bearer " + admin["accessToken"]}
    root_org = admin["user"]["organization"]["id"]
    orgs = request("/api/v1/admin/org-units?limit=100", headers=headers)["items"]

    def org(name, kind, parent):
        return next((row for row in orgs if row["name"] == name and row.get("parentId") == parent), None) or request(
            "/api/v1/admin/org-units", {"name": name, "kind": kind, "parentId": parent}, headers)

    department = org("演示运营部", "DEPARTMENT", root_org)
    team = org("演示服务组", "TEAM", department["id"])
    for index, (name, display) in enumerate((("staff", "虚构员工林晨"), ("observer", "虚构员工周禾")), 1):
        users = request("/api/v1/admin/users?q=" + name, headers=headers)["items"]
        if not any(row["loginName"] == name for row in users):
            request("/api/v1/admin/users", {
                "loginName": name, "password": accounts[name], "displayName": display,
                "orgUnitId": team["id"], "roles": ["EMPLOYEE"], "jobTitle": "演示运营",
                "gender": "FEMALE", "idNumber": str(index).zfill(18), "phoneNumber": str(index).zfill(6),
            }, headers)
    request("/api/v1/auth/logout", {}, headers)


def run_backend(jar, log, access, bootstrap):
    from base64 import b64encode
    env = dict(os.environ, PANDORA_CRM_BOOTSTRAP_TOKEN=bootstrap,
               PANDORA_CRM_HANDOFF_KEY=b64encode(bytes.fromhex(access["handoff"])).decode("ascii"))
    return subprocess.Popen(["java", "-jar", str(jar), "--spring.profiles.active=dev2"],
                            cwd=RUNTIME, env=env, stdout=log, stderr=subprocess.STDOUT)


def wait_ready(process):
    deadline = time.monotonic() + 45
    while time.monotonic() < deadline and process.poll() is None:
        try:
            if request("/health")["status"] == "UP":
                return
        except URLError:
            time.sleep(.5)
    raise RuntimeError("无法启动 CRM 后端，请查看 " + str(RUNTIME / "backend.log"))


def stop(process):
    if process.poll() is None:
        process.terminate()
        process.wait(timeout=15)


def main():
    import shutil
    import socket
    with socket.socket() as probe:
        if probe.connect_ex(("127.0.0.1", 19082)) == 0:
            raise RuntimeError("本机端口 19082 已被占用。")
    for path in (RUNTIME.parent, RUNTIME):
        if path.is_symlink() or path.is_junction():
            raise RuntimeError("演示路径不能指向其他目录。")
    RUNTIME.mkdir(parents=True, exist_ok=True)
    source = REPO / "backend" / "target" / "backend-0.0.1-SNAPSHOT.jar"
    jar = RUNTIME / "backend.jar"
    shutil.copyfile(source, jar)
    access = demo_access()
    with (RUNTIME / "backend.log").open("ab") as log:
        marker = RUNTIME / "initialized"
        bootstrap = "" if marker.exists() else access["bootstrap"]
        process = run_backend(jar, log, access, bootstrap)
        try:
            wait_ready(process)
            provision(access, bootstrap)
            marker.touch(exist_ok=True)
            print(f"CRM 后端 {API}（PID {process.pid}）", flush=True)
            print(f"本机演示账号文件 {RUNTIME / 'demo-access.json'}", flush=True)
            process.wait()
        except KeyboardInterrupt:
            # Java shares this console and receives the same Ctrl-C for a clean H2 shutdown.
            process.wait(timeout=20)
        finally:
            stop(process)


if __name__ == "__main__":
    main()
