"""Package only built public artifacts; no source secrets or databases enter a release."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import tarfile
import tempfile
import shutil
import zipfile


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--sha", required=True)
    parser.add_argument("--run-number", type=int, required=True)
    parser.add_argument("--attempt", type=int, default=1)
    parser.add_argument("--output", required=True)
    parser.add_argument("--inventory", action="store_true")
    parser.add_argument("--missing-file")
    args = parser.parse_args()
    if not re.fullmatch(r"[0-9a-f]{40}", args.sha) or args.run_number < 0 or args.attempt < 1:
        parser.error("Expected a full commit SHA and non-negative run number")
    root = Path(__file__).resolve().parents[1]
    fatjar = root / "backend/target/backend-0.0.1-SNAPSHOT.jar"
    with zipfile.ZipFile(fatjar) as jar:
        libraries = [{"name": Path(name).name, "sha256": hashlib.sha256(jar.read(name)).hexdigest(), "source": name}
                     for name in jar.namelist() if name.startswith("BOOT-INF/lib/") and name.endswith(".jar")]
    if args.inventory:
        Path(args.output).write_text(json.dumps({"operation": "probe", "hashes": [x["sha256"] for x in libraries]}), encoding="utf-8")
        return
    missing = set(json.loads(Path(args.missing_file).read_text())['missing']) if args.missing_file else {x["sha256"] for x in libraries}
    with tempfile.TemporaryDirectory() as scratch:
        release = Path(scratch)
        with zipfile.ZipFile(fatjar) as jar, zipfile.ZipFile(release / "backend.jar", "w", zipfile.ZIP_DEFLATED) as app:
            for name in jar.namelist():
                if name.startswith("BOOT-INF/classes/"):
                    relative = name.removeprefix("BOOT-INF/classes/")
                    if relative:
                        # Spring package scanning also needs the directory entries in a plain jar.
                        app.writestr(relative, jar.read(name))
            for library in libraries:
                if library['sha256'] in missing:
                    (release / "libraries").mkdir(exist_ok=True)
                    (release / "libraries" / (library['sha256'] + '.jar')).write_bytes(jar.read(library['source']))
        shutil.copytree(root / "web/dist", release / "web")
        marker = {"sha": args.sha, "runNumber": args.run_number, "runAttempt": args.attempt, "branch": "dev"}
        (release / "web/deployment.json").write_text(json.dumps(marker) + "\n", encoding="utf-8")
        files = {p.relative_to(release).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
                 for p in release.rglob("*") if p.is_file()}
        manifest = {**marker, "files": files, "libraries": [{k:v for k,v in x.items() if k != 'source'} for x in libraries]}
        (release / "release.json").write_text(json.dumps(manifest, sort_keys=True) + "\n", encoding="utf-8")
        with tarfile.open(args.output, "w:gz") as archive:
            for name in ("backend.jar", "web", "release.json"):
                archive.add(release / name, arcname=name)
            if (release / "libraries").exists():
                archive.add(release / "libraries", arcname="libraries")
    print(f"Packaged dev {args.sha}, run {args.run_number}")


if __name__ == "__main__":
    main()
