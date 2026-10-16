#!/usr/bin/python3
"""Root-owned forced SSH command. Accepts only a bounded validated release on stdin."""
import datetime
import fcntl
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import shutil
import signal
import subprocess
import sys
import tarfile
import tempfile
import time
import urllib.request

ROOT = Path("/opt/Pandora")
MAX_ARCHIVE = 128 * 1024 * 1024
MAX_UNPACKED = 256 * 1024 * 1024
SERVICE = "pandora-backend.service"


def validate_archive(archive_path, destination):
    with tarfile.open(archive_path, "r:gz") as archive:
        members = archive.getmembers()
        seen = set()
        if len(members) > 5000 or sum(m.size for m in members) > MAX_UNPACKED:
            raise ValueError("Archive limits exceeded")
        for member in members:
            path = PurePosixPath(member.name)
            name = member.name.rstrip("/")
            if (path.is_absolute() or ".." in path.parts or "\\" in member.name
                    or name in seen or not (member.isfile() or member.isdir())
                    or not (name in {"backend.jar", "release.json", "web", "libraries"}
                            or name.startswith("web/") or re.fullmatch(r"libraries/[0-9a-f]{64}\.jar", name))):
                raise ValueError("Unsafe archive member")
            seen.add(name)
            target = destination / name
            if member.isdir():
                target.mkdir(parents=True, exist_ok=True)
                target.chmod(0o755)
            else:
                target.parent.mkdir(parents=True, exist_ok=True)
                with archive.extractfile(member) as source, target.open("wb") as output:
                    shutil.copyfileobj(source, output)
                target.chmod(0o644)
    manifest = json.loads((destination / "release.json").read_text(encoding="utf-8"))
    if (not re.fullmatch(r"[0-9a-f]{40}", manifest.get("sha", ""))
            or type(manifest.get("runNumber")) is not int or manifest["runNumber"] < 0
            or type(manifest.get("runAttempt", 1)) is not int or manifest.get("runAttempt", 1) < 1
            or manifest.get("branch") != "dev"):
        raise ValueError("Invalid release identity")
    files = manifest.get("files")
    actual = {p.relative_to(destination).as_posix() for p in destination.rglob("*") if p.is_file()}
    if not isinstance(files, dict) or set(files) != actual - {"release.json"}:
        raise ValueError("Manifest file set mismatch")
    if not {"backend.jar", "web/index.html", "web/deployment.json"}.issubset(files):
        raise ValueError("Required artifacts missing")
    for name, digest in files.items():
        if hashlib.sha256((destination / name).read_bytes()).hexdigest() != digest:
            raise ValueError("Artifact hash mismatch")
    destination.chmod(0o755)
    for directory in destination.rglob("*"):
        if directory.is_dir():
            directory.chmod(0o755)
    marker = json.loads((destination / "web/deployment.json").read_text(encoding="utf-8"))
    if any(marker.get(k) != manifest[k] for k in ("sha", "runNumber", "branch")):
        raise ValueError("Public revision marker mismatch")
    return manifest


def service(action):
    subprocess.run(["/usr/bin/systemctl", action, SERVICE], check=True, timeout=45)


def healthy():
    deadline = time.monotonic() + 90
    while time.monotonic() < deadline:
        try:
            with urllib.request.urlopen("http://127.0.0.1:18080/health", timeout=3) as response:
                if json.load(response).get("status") == "UP":
                    return True
        except (OSError, ValueError):
            pass
        time.sleep(1)
    return False


def switch(release):
    pending = ROOT / ".current-next"
    pending.unlink(missing_ok=True)
    pending.symlink_to(release)
    pending.replace(ROOT / "current")


def prepare_libraries(unpacked, manifest):
    cache = ROOT / "libraries"
    cache.mkdir(mode=0o755, exist_ok=True)
    cache.chmod(0o755)
    linkdir = unpacked / "lib"
    linkdir.mkdir(mode=0o755)
    linkdir.chmod(0o755)
    for library in manifest.get("libraries", []):
        name, digest = library.get("name", ""), library.get("sha256", "")
        if not re.fullmatch(r"[A-Za-z0-9_.+-]+\.jar", name) or not re.fullmatch(r"[0-9a-f]{64}", digest):
            raise ValueError("Invalid dependency identity")
        cached = cache / (digest + ".jar")
        uploaded = unpacked / "libraries" / (digest + ".jar")
        if not cached.exists() and uploaded.is_file():
            shutil.copyfile(uploaded, cached)
            cached.chmod(0o644)
        if not cached.is_file() or hashlib.sha256(cached.read_bytes()).hexdigest() != digest:
            raise ValueError("Missing or invalid cached dependency")
        (linkdir / name).symlink_to(cached)


def activate(release, manifest):
    current = ROOT / "current"
    old = current.resolve() if current.is_symlink() else None
    if old is not None and old.parent != ROOT / "releases":
        raise ValueError("Unexpected current release target")
    if old is not None:
        previous = json.loads((old / "release.json").read_text(encoding="utf-8"))
        if manifest["runNumber"] < previous["runNumber"]:
            raise ValueError("Stale workflow run rejected")
        if (manifest["runNumber"] == previous["runNumber"]
                and manifest.get("runAttempt", 1) < previous.get("runAttempt", 1)):
            raise ValueError("Stale workflow attempt rejected")
        if old == release:
            print("Release already active")
            return
    service("stop")
    try:
        stamp = datetime.datetime.now(datetime.timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
        backup = ROOT / "backups" / (stamp + "-" + manifest["sha"][:12])
        backup.mkdir(mode=0o700)
        for path in (ROOT / "shared/data").glob("*.mv.db"):
            target = backup / path.name
            shutil.copyfile(path, target)
            target.chmod(0o600)
        switch(release)
        service("start")
        if not healthy():
            raise RuntimeError("New backend failed health verification")
    except Exception:
        if old is not None:
            switch(old)
            service("restart")
            if not healthy():
                print("Previous program also failed health verification", file=sys.stderr)
        else:
            service("stop")
        raise
    print(json.dumps({"deployed": True, "sha": manifest["sha"], "runNumber": manifest["runNumber"]}))


def main():
    if os.geteuid() != 0:
        raise SystemExit("This receiver must run through the constrained sudo command")
    os.umask(0o077)
    # A disconnected uploader must not retain the deployment lock or wait indefinitely.
    def timed_out(*_):
        raise TimeoutError("Release upload/activation timed out")

    signal.signal(signal.SIGALRM, timed_out)
    signal.signal(signal.SIGTERM, timed_out)
    signal.signal(signal.SIGHUP, timed_out)
    signal.alarm(600)
    if sys.stdin.buffer.peek(1).startswith(b"{"):
        query = json.loads(sys.stdin.buffer.read(65537))
        hashes = query.get("hashes", [])
        if query.get("operation") != "probe" or not isinstance(hashes, list) or len(hashes) > 500:
            raise ValueError("Invalid dependency query")
        if any(not isinstance(h, str) or not re.fullmatch(r"[0-9a-f]{64}", h) for h in hashes):
            raise ValueError("Invalid dependency digest")
        print(json.dumps({"missing": [h for h in hashes if not (ROOT / "libraries" / (h + ".jar")).is_file()]}))
        return
    with tempfile.TemporaryDirectory(prefix="incoming-", dir=ROOT / "releases") as scratch:
        scratch = Path(scratch)
        bundle = scratch / "upload.tgz"
        size = 0
        with bundle.open("wb") as output:
            while chunk := sys.stdin.buffer.read(65536):
                size += len(chunk)
                if size > MAX_ARCHIVE:
                    raise ValueError("Upload limit exceeded")
                output.write(chunk)
        unpacked = scratch / "content"
        unpacked.mkdir(mode=0o755)
        manifest = validate_archive(bundle, unpacked)
        prepare_libraries(unpacked, manifest)
        release = ROOT / "releases" / f"{manifest['sha']}-{manifest['runNumber']}-{manifest.get('runAttempt', 1)}"
        if release.exists():
            if (release / "release.json").read_bytes() != (unpacked / "release.json").read_bytes():
                raise ValueError("Existing release identity has different artifacts")
        else:
            unpacked.rename(release)
        with (ROOT / "deploy.lock").open("a") as lock:
            fcntl.flock(lock, fcntl.LOCK_EX)
            activate(release, manifest)
    signal.alarm(0)


if __name__ == "__main__":
    main()
