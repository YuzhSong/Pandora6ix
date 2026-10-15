"""Start the exact thin application format before it can be deployed."""
import json
import os
from pathlib import Path
import shutil
import socket
import subprocess
import sys
import tarfile
import tempfile
import time
import urllib.error
import urllib.request

root = Path(__file__).resolve().parents[1]
java = str(Path(os.environ["JAVA_HOME"]) / "bin" / ("java.exe" if os.name == "nt" else "java")) if os.environ.get("JAVA_HOME") else shutil.which("java")
with tempfile.TemporaryDirectory() as scratch:
    scratch = Path(scratch)
    bundle = scratch / "release.tgz"
    subprocess.run([sys.executable, str(root / "deploy/package-release.py"), "--sha", "0" * 40,
                    "--run-number", "0", "--output", str(bundle)], check=True)
    with tarfile.open(bundle) as archive:
        archive.extractall(scratch / "app", filter="data")
    app = scratch / "app"
    manifest = json.loads((app / "release.json").read_text())
    libraries = [str(app / "libraries" / (x["sha256"] + ".jar")) for x in manifest["libraries"]]
    classpath = os.pathsep.join([str(app / "backend.jar"), *libraries])
    with socket.socket() as probe:
        probe.bind(("127.0.0.1", 0));port = probe.getsockname()[1]
    env = {**os.environ, "PANDORA_DB_URL": "jdbc:h2:mem:packaged-runtime", "PANDORA_DB_USER": "sa",
           "PANDORA_DB_PASSWORD": "", "PANDORA_BOOTSTRAP_TOKEN": ""}
    with (scratch / "runtime.log").open("wb") as log:
        process = subprocess.Popen([java, "-Xmx384m", "-cp", classpath,
                    "com.pandora6ix.backend.Pandora6ixBackendApplication", "--server.address=127.0.0.1", f"--server.port={port}"],
                    cwd=scratch, env=env, stdout=log, stderr=subprocess.STDOUT,
                    creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0)
        try:
            deadline = time.monotonic() + 60
            while time.monotonic() < deadline:
                if process.poll() is not None:
                    raise RuntimeError("Packaged backend exited")
                try:
                    with urllib.request.urlopen(f"http://127.0.0.1:{port}/health", timeout=2) as response:
                        if json.load(response).get("status") == "UP":break
                except (OSError, ValueError):pass
                time.sleep(.25)
            else:raise RuntimeError("Packaged backend has no working /health")
            try:
                urllib.request.urlopen(f"http://127.0.0.1:{port}/api/v1/me", timeout=3)
                raise AssertionError("Packaged backend exposed unauthenticated profile")
            except urllib.error.HTTPError as error:
                assert error.code == 401
                body=json.load(error);assert body["requestId"] and body["error"]["code"]=="UNAUTHENTICATED"
            print("Packaged runtime passed: real health controller, component scan, database migration and protected profile.")
        except Exception:
            log.flush();print((scratch / "runtime.log").read_text(errors="replace")[-6000:])
            raise
        finally:
            if process.poll() is None:
                process.terminate()
                try:process.wait(timeout=15)
                except subprocess.TimeoutExpired:process.kill();process.wait(timeout=10)
