#!/usr/bin/env python3
"""Run Gradle with the cloud's HTTPS proxy and trusted system CA store.

No credentials or environment values are printed or written into the repository.
On a personal machine use ./gradlew directly instead.
"""
import os
from pathlib import Path
import shlex
import subprocess
import sys
from urllib.parse import urlsplit

task_env = os.environ.copy()
task_flags = []
for scheme in ("http", "https"):
    task_proxy = task_env.get(scheme.upper() + "_PROXY") or task_env.get(scheme + "_proxy")
    if task_proxy:
        task_url = urlsplit(task_proxy)
        if task_url.username or task_url.password:
            raise SystemExit("Authenticated proxy URL: configure a supported Gradle proxy binding outside the checkout.")
        task_flags += [f"-D{scheme}.proxyHost={task_url.hostname}", f"-D{scheme}.proxyPort={task_url.port or 80}"]
task_flags += ["-Dhttp.nonProxyHosts=localhost|127.*|[::1]"]
task_trust = Path('/etc/ssl/certs/java/cacerts')
if task_trust.is_file():
    task_flags += [f"-Djavax.net.ssl.trustStore={task_trust}"]
task_env['JAVA_TOOL_OPTIONS'] = task_env.get('JAVA_TOOL_OPTIONS', '') + ' ' + ' '.join(shlex.quote(x) for x in task_flags)
task_root = Path(__file__).resolve().parents[1]
task_env.setdefault('GRADLE_USER_HOME', '/workspace/.gradle' if Path('/workspace').is_dir() else str(Path.home() / '.gradle'))
raise SystemExit(subprocess.call([str(task_root / 'gradlew'), *sys.argv[1:]], cwd=task_root, env=task_env))
