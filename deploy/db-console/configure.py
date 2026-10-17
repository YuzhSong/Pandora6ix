"""Run as root on the cloud after staging the reviewed source and Java-21 classes.

Credentials are generated on the server. The sole argument is a public key file;
private keys, passwords and production rows are never accepted as CLI arguments.
"""
import datetime
import fcntl
import json
import os
from pathlib import Path
import pwd
import re
import secrets
import shutil
import subprocess
import sys
import time
import urllib.request

ROOT = Path('/opt/Pandora')
TOOLS = ROOT / 'tools/db-console'
CONFIG = ROOT / 'config'
DROPIN = Path('/etc/systemd/system/pandora-backend.service.d/40-db-console.conf')
SSH_CONFIG = Path('/etc/ssh/sshd_config.d/60-pandora-db-view.conf')


def run(args, **kwargs):
    result = subprocess.run(args, capture_output=True, text=True, **kwargs)
    if result.returncode:
        # Deliberately do not include environment, input or arbitrary SQL in diagnostics.
        raise RuntimeError('Command failed: ' + ' '.join(args[:3]))
    return result.stdout


def load_env(path):
    result = {}
    for line in path.read_text().splitlines():
        if '=' in line and not line.lstrip().startswith('#'):
            key, value = line.split('=', 1)
            result[key] = value.strip().strip('"').strip("'")
    return result


def private_write(path, content):
    temporary = path.with_suffix(path.suffix + '.new')
    temporary.write_text(content)
    temporary.chmod(0o600)
    temporary.replace(path)


def wait_ready(console=True):
    deadline = time.monotonic() + 60
    while time.monotonic() < deadline:
        try:
            with urllib.request.urlopen('http://127.0.0.1:18080/health', timeout=2) as response:
                assert json.load(response)['status'] == 'UP'
            if console:
                with urllib.request.urlopen('http://127.0.0.1:18082/', timeout=2) as response:
                    assert b'H2 Console' in response.read()
            return
        except (OSError, ValueError, AssertionError):
            time.sleep(.3)
    raise RuntimeError('Application/console readiness timed out')


def main():
    if os.geteuid() != 0:
        raise SystemExit('Run as root')
    os.umask(0o077)
    public_key = Path(sys.argv[1]).read_text().strip()
    if not re.fullmatch(r'ssh-ed25519 [A-Za-z0-9+/=]+(?: [A-Za-z0-9@._-]+)?', public_key):
        raise ValueError('Expected one Ed25519 public key')
    if not (TOOLS / 'classes/com/pandora6ix/ops/PandoraDatabaseConsole.class').is_file():
        raise ValueError('Stage the exact-version verified Java classes first')
    jars = list((ROOT / 'current/lib').glob('h2-*.jar'))
    if len(jars) != 1:
        raise ValueError('Expected exactly one released H2 driver')
    backend_env = CONFIG / 'backend.env'
    values = load_env(backend_env)
    if values.get('PANDORA_DB_URL') != 'jdbc:h2:file:/opt/Pandora/shared/data/pandora;DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0':
        raise ValueError('Review the exact embedded database path before configuring this host')
    console_env = CONFIG / 'db-console.env'
    if console_env.exists():
        console_values = load_env(console_env)
    else:
        console_values = {
            'PANDORA_CONSOLE_ENABLED': 'true', 'PANDORA_CONSOLE_PORT': '18082',
            'PANDORA_CONSOLE_STATE': '/opt/Pandora/shared/db-console',
            'PANDORA_VIEWER_PASSWORD': secrets.token_urlsafe(32),
            'PANDORA_CONSOLE_ADMIN_PASSWORD': secrets.token_urlsafe(32),
        }
        private_write(console_env, ''.join(f'{k}={v}\n' for k, v in console_values.items()))
    try:
        account = pwd.getpwnam('pandora-db-view')
    except KeyError:
        run(['useradd', '--create-home', '--shell', '/usr/sbin/nologin', '--user-group', 'pandora-db-view'])
        account = pwd.getpwnam('pandora-db-view')
    ssh_home = Path(account.pw_dir) / '.ssh'
    ssh_home.mkdir(mode=0o700, exist_ok=True)
    os.chown(ssh_home, account.pw_uid, account.pw_gid)
    authorized = ssh_home / 'authorized_keys'
    expected_key = 'restrict,port-forwarding,permitopen="127.0.0.1:18082" ' + public_key + '\n'
    if authorized.exists() and authorized.read_text() != expected_key:
        raise ValueError('Refusing to replace unrelated authorized keys')
    private_write(authorized, expected_key)
    os.chown(authorized, account.pw_uid, account.pw_gid)
    expected_ssh = '''# Managed Pandora database view account only
Match User pandora-db-view
    AuthenticationMethods publickey
    PasswordAuthentication no
    KbdInteractiveAuthentication no
    AllowTcpForwarding local
    PermitOpen 127.0.0.1:18082
    AllowAgentForwarding no
    X11Forwarding no
    PermitTTY no
    PermitTunnel no
    ForceCommand /usr/sbin/nologin
Match all
'''
    if SSH_CONFIG.exists() and SSH_CONFIG.read_text() != expected_ssh:
        raise ValueError('Refusing to replace unrelated SSH account configuration')
    SSH_CONFIG.write_text(expected_ssh)
    SSH_CONFIG.chmod(0o644)
    run(['sshd', '-t'])
    run(['systemctl', 'reload', 'ssh'])
    app_account = pwd.getpwnam('pandora-app')
    state = Path(console_values['PANDORA_CONSOLE_STATE'])
    state.mkdir(mode=0o700, exist_ok=True)
    os.chown(state, app_account.pw_uid, app_account.pw_gid)
    backup = ROOT / 'backups' / ('db-console-' + datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ'))
    previous_dropin = DROPIN.read_text() if DROPIN.exists() else None
    # Coordinate with the actual release receiver's activation/backup lock.
    with (ROOT / 'deploy.lock').open('a') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        run(['systemctl', 'stop', 'pandora-backend'])
        try:
            backup.mkdir(mode=0o700)
            shutil.copyfile(ROOT / 'shared/data/pandora.mv.db', backup / 'pandora.mv.db')
            (backup / 'pandora.mv.db').chmod(0o600)
            shutil.copyfile(backend_env, backup / 'backend.env.before')
            new_password = values.get('PANDORA_DB_PASSWORD') or secrets.token_urlsafe(32)
            # Preserve the proposed credential privately even if the installer is interrupted.
            private_write(CONFIG / 'db-console-admin.pending', new_password + '\n')
            setup_env = {**os.environ, **values, **console_values, 'PANDORA_NEW_DB_PASSWORD': new_password}
            output = run(['runuser', '-u', 'pandora-app', '--', '/usr/bin/java',
                          '-Dh2.bindAddress=127.0.0.1', '-cp', str(TOOLS / 'classes') + ':' + str(jars[0]),
                          'com.pandora6ix.ops.PandoraDatabaseConsole', 'prepare'], env=setup_env)
            # This file is written immediately after successful password rotation, before enabling the console.
            lines = [line for line in backend_env.read_text().splitlines() if not line.startswith('PANDORA_DB_PASSWORD=')]
            lines.append('PANDORA_DB_PASSWORD=' + new_password)
            private_write(backend_env, '\n'.join(lines) + '\n')
            (backup / 'preparation.txt').write_text(output)
            DROPIN.parent.mkdir(mode=0o755, exist_ok=True)
            DROPIN.write_text('''# Optional same-JVM operations console; released application jar remains unchanged
[Service]
EnvironmentFile=/opt/Pandora/config/db-console.env
ExecStart=
ExecStart=/usr/bin/java -Xms128m -Xmx512m -XX:MaxMetaspaceSize=192m -Dh2.bindAddress=127.0.0.1 -cp /opt/Pandora/tools/db-console/classes:/opt/Pandora/current/backend.jar:/opt/Pandora/current/lib/* com.pandora6ix.ops.PandoraDatabaseConsole --server.address=127.0.0.1 --server.port=18080
''')
            DROPIN.chmod(0o644)
            run(['systemctl', 'daemon-reload'])
            run(['systemctl', 'start', 'pandora-backend'])
            wait_ready()
            (CONFIG / 'db-console-admin.pending').unlink(missing_ok=True)
        except BaseException:
            run(['systemctl', 'stop', 'pandora-backend'])
            # Resolve the committed credential after a partial installation without restoring data.
            for candidate in dict.fromkeys([locals().get('new_password'), values.get('PANDORA_DB_PASSWORD', '')]):
                if candidate is None:
                    continue
                probe_env = {**os.environ, **values, 'PANDORA_DB_PASSWORD': candidate}
                probe = subprocess.run(['runuser', '-u', 'pandora-app', '--', '/usr/bin/java',
                          '-cp', str(TOOLS / 'classes') + ':' + str(jars[0]),
                          'com.pandora6ix.ops.PandoraDatabaseConsole', 'check-admin'],
                          env=probe_env, capture_output=True, text=True)
                if probe.returncode == 0:
                    lines = [line for line in backend_env.read_text().splitlines() if not line.startswith('PANDORA_DB_PASSWORD=')]
                    lines.append('PANDORA_DB_PASSWORD=' + candidate)
                    private_write(backend_env, '\n'.join(lines) + '\n')
                    break
            if previous_dropin is None:
                DROPIN.unlink(missing_ok=True)
            else:
                DROPIN.write_text(previous_dropin)
            run(['systemctl', 'daemon-reload'])
            run(['systemctl', 'restart', 'pandora-backend'])
            wait_ready(console=False)
            raise
    print(json.dumps({'configured': True, 'backup': str(backup), 'webPort': 18082,
                      'reader': 'PANDORA_VIEWER', 'sshUser': 'pandora-db-view'}))


if __name__ == '__main__':
    main()
