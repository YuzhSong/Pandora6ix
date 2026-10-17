"""Real released-format backend + operations launcher, isolated file DB and HTTP Console."""
import importlib.util
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
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('verify_console', HERE / 'verify_console.py')
verify = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verify)
java = str(Path(os.environ['JAVA_HOME']) / 'bin' / ('java.exe' if os.name == 'nt' else 'java')) if os.environ.get('JAVA_HOME') else shutil.which('java')


def free_port():
    with socket.socket() as probe:
        probe.bind(('127.0.0.1', 0))
        return probe.getsockname()[1]


def main():
    run = Path(tempfile.mkdtemp(prefix='pandora-console-runtime-'))
    bundle = run / 'release.tgz'
    subprocess.run([sys.executable, str(ROOT / 'deploy/package-release.py'), '--sha', '0' * 40,
                    '--run-number', '0', '--output', str(bundle)], check=True)
    with tarfile.open(bundle) as archive:
        archive.extractall(run / 'app', filter='data')
    app = run / 'app'
    manifest = json.loads((app / 'release.json').read_text())
    libraries = [str(app / 'libraries' / (x['sha256'] + '.jar')) for x in manifest['libraries']]
    classes = ROOT / 'backend/target/db-console-classes'
    classpath = os.pathsep.join([str(classes), str(app / 'backend.jar'), *libraries])
    api_port, console_port = free_port(), free_port()
    db_url = 'jdbc:h2:file:' + str(run / 'database') + ';DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0'
    password, reader = str(uuid.uuid4()), str(uuid.uuid4())
    env = {**os.environ, 'PANDORA_DB_URL': db_url, 'PANDORA_DB_USER': 'sa',
           'PANDORA_DB_PASSWORD': '', 'PANDORA_BOOTSTRAP_TOKEN': str(uuid.uuid4())}
    process = None
    logs = []
    def stop():
        if process and process.poll() is None:
            process.terminate()
            process.wait(timeout=25)
    def start(entry, console=False):
        nonlocal process
        log = (run / ('server-' + str(len(logs)) + '.log')).open('wb')
        logs.append(log)
        process = subprocess.Popen([java, '-Xmx384m', '-Dh2.bindAddress=127.0.0.1', '-cp', classpath,
                    entry, '--server.address=127.0.0.1', '--server.port=' + str(api_port)], cwd=run,
                    env=env, stdout=log, stderr=subprocess.STDOUT,
                    creationflags=subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0)
        for _ in range(180):
            if process.poll() is not None:
                raise RuntimeError('Test process exited; log directory=' + str(run))
            try:
                with urllib.request.urlopen(f'http://127.0.0.1:{api_port}/health', timeout=2) as response:
                    assert json.load(response)['status'] == 'UP'
                if console:
                    verify.read(f'http://127.0.0.1:{console_port}', '/')
                return
            except (OSError, AssertionError):
                time.sleep(.25)
        raise RuntimeError('Test startup timed out; logs=' + str(run))
    try:
        start('com.pandora6ix.backend.Pandora6ixBackendApplication')
        stop()
        setup_env = {**env, 'PANDORA_VIEWER_PASSWORD': reader, 'PANDORA_NEW_DB_PASSWORD': password}
        subprocess.run([java, '-cp', classpath, 'com.pandora6ix.ops.PandoraDatabaseConsole', 'prepare'], env=setup_env, check=True)
        env.update(PANDORA_DB_PASSWORD=password, PANDORA_CONSOLE_ENABLED='true',
                   PANDORA_VIEWER_PASSWORD=reader, PANDORA_CONSOLE_ADMIN_PASSWORD=str(uuid.uuid4()),
                   PANDORA_CONSOLE_PORT=str(console_port), PANDORA_CONSOLE_STATE=str(run / 'state'))
        for _ in range(2):
            start('com.pandora6ix.ops.PandoraDatabaseConsole', console=True)
            result = verify.verify(f'http://127.0.0.1:{console_port}',
                                  db_url.split(";", 1)[0] + ';IFEXISTS=TRUE;SCHEMA=PANDORA_VIEW', reader)
            assert result['peopleCount'] == 0
            stop()
            try:
                verify.read(f'http://127.0.0.1:{console_port}', '/')
                raise AssertionError('Console remained after backend exit')
            except OSError:
                pass
        print('Console runtime passed: released backend format, real HTTP reader login, permission denials, admin password and two restarts.')
    finally:
        stop()
        for log in logs:
            log.close()


if __name__ == '__main__':
    main()
