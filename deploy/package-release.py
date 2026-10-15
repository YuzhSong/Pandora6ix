"""Package only built public artifacts; no source secrets or databases enter a release."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import tarfile
import tempfile
import shutil


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--sha", required=True)
    parser.add_argument("--run-number", type=int, required=True)
    parser.add_argument("--attempt", type=int, default=1)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    if not re.fullmatch(r"[0-9a-f]{40}", args.sha) or args.run_number < 0 or args.attempt < 1:
        parser.error("Expected a full commit SHA and non-negative run number")
    root = Path(__file__).resolve().parents[1]
    with tempfile.TemporaryDirectory() as scratch:
        release = Path(scratch)
        shutil.copyfile(root / "backend/target/backend-0.0.1-SNAPSHOT.jar", release / "backend.jar")
        shutil.copytree(root / "web/dist", release / "web")
        marker = {"sha": args.sha, "runNumber": args.run_number, "runAttempt": args.attempt, "branch": "dev"}
        (release / "web/deployment.json").write_text(json.dumps(marker) + "\n", encoding="utf-8")
        files = {p.relative_to(release).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
                 for p in release.rglob("*") if p.is_file()}
        manifest = {**marker, "files": files}
        (release / "release.json").write_text(json.dumps(manifest, sort_keys=True) + "\n", encoding="utf-8")
        with tarfile.open(args.output, "w:gz") as archive:
            for name in ("backend.jar", "web", "release.json"):
                archive.add(release / name, arcname=name)
    print(f"Packaged dev {args.sha}, run {args.run_number}")


if __name__ == "__main__":
    main()
