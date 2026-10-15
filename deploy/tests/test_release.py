import hashlib
import importlib.util
import io
import json
from pathlib import Path
import sys
import tarfile
import tempfile
import unittest
from unittest.mock import patch

# fcntl is used only by the Linux command entrypoint; permit payload tests on Windows.
if sys.platform == "win32":
    import types
    sys.modules.setdefault("fcntl", types.SimpleNamespace())
spec = importlib.util.spec_from_file_location("receiver", Path(__file__).resolve().parents[1] / "receive-release.py")
receiver = importlib.util.module_from_spec(spec)
spec.loader.exec_module(receiver)
SHA = "a" * 40


class ReleaseTests(unittest.TestCase):
    def bundle(self, folder, extra=None, wrong_hash=False):
        marker = json.dumps({"sha": SHA, "runNumber": 2, "branch": "dev"}).encode()
        content = {"backend.jar": b"jar", "web/index.html": b"<html>app</html>", "web/deployment.json": marker}
        files = {k: hashlib.sha256(v).hexdigest() for k, v in content.items()}
        if wrong_hash:
            files["backend.jar"] = "0" * 64
        content["release.json"] = json.dumps({"sha": SHA, "runNumber": 2, "branch": "dev", "files": files}).encode()
        bundle = folder / "release.tgz"
        with tarfile.open(bundle, "w:gz") as archive:
            for name, data in content.items():
                member = tarfile.TarInfo(name); member.size = len(data)
                archive.addfile(member, io.BytesIO(data))
            if extra:
                archive.addfile(extra, io.BytesIO(b""))
        destination = folder / "unpacked"; destination.mkdir()
        return bundle, destination

    def test_valid_payload_and_file_only_directories(self):
        with tempfile.TemporaryDirectory() as folder:
            bundle, destination = self.bundle(Path(folder))
            result = receiver.validate_archive(bundle, destination)
            self.assertEqual(SHA, result["sha"])
            self.assertEqual(b"jar", (destination / "backend.jar").read_bytes())

    def test_hash_mismatch_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            bundle, destination = self.bundle(Path(folder), wrong_hash=True)
            with self.assertRaises(ValueError): receiver.validate_archive(bundle, destination)

    def test_path_traversal_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            bundle, destination = self.bundle(Path(folder), tarfile.TarInfo("../escape"))
            with self.assertRaises(ValueError): receiver.validate_archive(bundle, destination)
            self.assertFalse((Path(folder) / "escape").exists())

    def test_symlink_rejected(self):
        link = tarfile.TarInfo("web/link"); link.type = tarfile.SYMTYPE; link.linkname = "/etc/passwd"
        with tempfile.TemporaryDirectory() as folder:
            bundle, destination = self.bundle(Path(folder), link)
            with self.assertRaises(ValueError): receiver.validate_archive(bundle, destination)

    def test_unexpected_file_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            bundle, destination = self.bundle(Path(folder), tarfile.TarInfo("backend.env"))
            with self.assertRaises(ValueError): receiver.validate_archive(bundle, destination)

    def test_missing_dependency_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder); unpacked = root / "app"; unpacked.mkdir()
            with patch.object(receiver, "ROOT", root):
                with self.assertRaises(ValueError):
                    receiver.prepare_libraries(unpacked, {"libraries": [{"name": "missing.jar", "sha256": "a" * 64}]})

    def test_dependency_path_traversal_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder); unpacked = root / "app"; unpacked.mkdir()
            with patch.object(receiver, "ROOT", root):
                with self.assertRaises(ValueError):
                    receiver.prepare_libraries(unpacked, {"libraries": [{"name": "../bad.jar", "sha256": "a" * 64}]})

    def test_corrupted_cached_dependency_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder); unpacked = root / "app"; unpacked.mkdir(); (root / "libraries").mkdir()
            (root / "libraries" / ("a" * 64 + ".jar")).write_bytes(b"corrupted")
            with patch.object(receiver, "ROOT", root):
                with self.assertRaises(ValueError):
                    receiver.prepare_libraries(unpacked, {"libraries": [{"name": "dependency.jar", "sha256": "a" * 64}]})

    @unittest.skipIf(sys.platform == "win32", "Dependency links are verified on Linux")
    def test_valid_cached_dependency_is_linked_into_release(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder); unpacked = root / "app"; unpacked.mkdir(); (root / "libraries").mkdir()
            digest = hashlib.sha256(b"library").hexdigest(); cached = root / "libraries" / (digest + ".jar")
            cached.write_bytes(b"library")
            with patch.object(receiver, "ROOT", root):
                receiver.prepare_libraries(unpacked, {"libraries": [{"name": "dependency.jar", "sha256": digest}]})
            self.assertEqual(cached, (unpacked / "lib/dependency.jar").resolve())

    @unittest.skipIf(sys.platform == "win32", "Real symlink activation is verified on Linux")
    def test_unhealthy_release_restores_program_without_replacing_database(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder); (root / "releases").mkdir(); (root / "backups").mkdir()
            data = root / "shared/data"; data.mkdir(parents=True)
            (data / "pandora.mv.db").write_bytes(b"published-data")
            old = root / "releases/old"; old.mkdir(); (old / "release.json").write_text('{"runNumber":1}')
            new = root / "releases/new"; new.mkdir(); (root / "current").symlink_to(old)
            with patch.object(receiver, "ROOT", root), patch.object(receiver, "service") as service, patch.object(receiver, "healthy", side_effect=[False, True]):
                with self.assertRaises(RuntimeError): receiver.activate(new, {"sha": SHA, "runNumber": 2})
                self.assertEqual(old, (root / "current").resolve())
                self.assertEqual(b"published-data", (data / "pandora.mv.db").read_bytes())
                self.assertEqual(1, len(list((root / "backups").glob("*/pandora.mv.db"))))
                self.assertIn(unittest.mock.call("restart"), service.call_args_list)

    @unittest.skipIf(sys.platform == "win32", "Real symlink activation is verified on Linux")
    def test_older_release_cannot_stop_current_service(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder); old = root / "releases/old"; old.mkdir(parents=True)
            (old / "release.json").write_text('{"runNumber":3}')
            (root / "current").symlink_to(old)
            with patch.object(receiver, "ROOT", root), patch.object(receiver, "service") as service:
                with self.assertRaises(ValueError): receiver.activate(root / "releases/new", {"sha": SHA, "runNumber": 2})
                service.assert_not_called()


if __name__ == "__main__":
    unittest.main()
