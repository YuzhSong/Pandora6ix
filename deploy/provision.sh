#!/usr/bin/env bash
# Run as root from a directory containing this reviewed deployment source.
set -euo pipefail
cd -- "$(dirname -- "$0")"
root=/opt/Pandora
id pandora-app >/dev/null 2>&1 || useradd --system --home-dir "$root/shared" --no-create-home --shell /usr/sbin/nologin pandora-app
id pandora-deploy >/dev/null 2>&1 || useradd --system --home-dir "$root/deploy-home" --create-home --shell /bin/bash pandora-deploy
install -d -m 755 "$root" "$root/releases"
install -d -m 750 -o pandora-app -g pandora-app "$root/shared"
install -d -m 700 -o pandora-app -g pandora-app "$root/shared/data"
install -d -m 700 "$root/backups" "$root/config"
if [[ ! -f "$root/config/backend.env" ]]; then
    umask 077
    cat > "$root/config/backend.env" <<'ENV'
PANDORA_DB_URL=jdbc:h2:file:/opt/Pandora/shared/data/pandora;DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0
PANDORA_DB_USER=sa
PANDORA_DB_PASSWORD=
PANDORA_BOOTSTRAP_TOKEN=
ENV
fi
install -m 755 receive-release.py /usr/local/sbin/pandora-receive-release
install -m 644 pandora-backend.service /etc/systemd/system/pandora-backend.service
printf '%s\n' 'pandora-deploy ALL=(root) NOPASSWD: /usr/local/sbin/pandora-receive-release' > /etc/sudoers.d/pandora-deploy
chmod 440 /etc/sudoers.d/pandora-deploy
visudo -cf /etc/sudoers.d/pandora-deploy
if [[ ! -f /etc/nginx/sites-available/pandora ]]; then
    install -m 644 pandora.nginx.conf /etc/nginx/sites-available/pandora
fi
ln -sfn /etc/nginx/sites-available/pandora /etc/nginx/sites-enabled/pandora
systemctl daemon-reload
systemctl enable pandora-backend.service nginx.service
nginx -t
systemctl reload nginx
echo 'Pandora service and HTTP virtual host provisioned; existing database/configuration retained.'
