#!/usr/bin/env python3
"""Install a verified Java 25 JDK in /workspace/.tools, then build the mod."""
import hashlib
import os
from pathlib import Path
import subprocess
import sys
import tarfile
import urllib.request

task_root = Path(__file__).resolve().parents[1]
task_tools = Path('/workspace/.tools')
task_tools.mkdir(parents=True, exist_ok=True)
task_jdks = sorted(task_tools.glob('jdk-25*/bin/javac'))
if not task_jdks:
    task_url = 'https://download.oracle.com/java/25/archive/jdk-25.0.5_linux-x64_bin.tar.gz'
    task_expected = '8748e3837bbceabfa17548564b6d814d123bd820e947e30dff885202b6f24f2c'
    task_archive = task_tools / 'aurel-jdk25.tar.gz'
    urllib.request.urlretrieve(task_url, task_archive)
    with task_archive.open('rb') as task_stream:
        task_actual = hashlib.file_digest(task_stream, 'sha256').hexdigest()
    if task_actual != task_expected:
        task_archive.unlink()
        raise SystemExit('JDK checksum mismatch; refusing to use the download')
    with tarfile.open(task_archive) as task_tar:
        task_tar.extractall(task_tools, filter='data')
    task_archive.unlink()
    task_jdks = sorted(task_tools.glob('jdk-25*/bin/javac'))
if not task_jdks:
    raise SystemExit('Java 25 compiler not found after installation')
task_env = os.environ.copy()
task_env['JAVA_HOME'] = str(task_jdks[-1].parents[1])
raise SystemExit(subprocess.call([sys.executable, str(task_root / 'scripts/gradle-cloud.py'),
                                 'build', '--no-daemon', '--console', 'plain'], cwd=task_root, env=task_env))
