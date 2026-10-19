#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TOOLS_DIR="${TOOLBOX_TOOLS_DIR:-/workspace/tools}"
mkdir -p "$TOOLS_DIR"
if ! command -v javac >/dev/null 2>&1 && [ ! -x "${JAVA_HOME:-/nonexistent}/bin/javac" ]; then
  if [ ! -x "$TOOLS_DIR/jdk-21.0.12.1/bin/javac" ]; then
    curl -fsSL https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.tar.gz -o "$TOOLS_DIR/jdk.tar.gz"
    curl -fsSL https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.tar.gz.sha256 -o "$TOOLS_DIR/jdk.sha256"
    python3 - "$TOOLS_DIR" <<'JDK'
import sys,pathlib,hashlib,tarfile
p=pathlib.Path(sys.argv[1]); archive=p/'jdk.tar.gz'
if hashlib.sha256(archive.read_bytes()).hexdigest() != (p/'jdk.sha256').read_text().strip(): raise SystemExit('Checksum JDK inválido')
with tarfile.open(archive) as t:
 root=t.getnames()[0].split('/')[0]
 t.extractall(p,filter='data')
(p/'jdk-current').write_text(root)
JDK
  fi
  if [ -f "$TOOLS_DIR/jdk-current" ]; then
    JAVA_HOME="$TOOLS_DIR/$(cat "$TOOLS_DIR/jdk-current")"
  else
    JAVA_HOME="$TOOLS_DIR/jdk-21.0.12.1"
  fi
  export JAVA_HOME
fi
if [ ! -x "$TOOLS_DIR/apache-maven-3.9.9/bin/mvn" ]; then
  curl -fsSL https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.tar.gz -o "$TOOLS_DIR/maven.tar.gz"
  curl -fsSL https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.tar.gz.sha512 -o "$TOOLS_DIR/maven.sha512"
  python3 - "$TOOLS_DIR" <<'PY'
import pathlib,sys,hashlib,tarfile
p=pathlib.Path(sys.argv[1]); archive=p/'maven.tar.gz'
if hashlib.sha512(archive.read_bytes()).hexdigest() != (p/'maven.sha512').read_text().strip(): raise SystemExit('Checksum Maven inválido')
with tarfile.open(archive) as t:t.extractall(p,filter='data')
PY
fi
python3 - "$TOOLS_DIR" <<'PY'
import os,sys,pathlib,urllib.parse
from xml.sax.saxutils import escape
proxy=urllib.parse.urlparse(os.environ.get('HTTPS_PROXY',''))
s='<settings>'
if proxy.hostname:
 s+='<proxies><proxy><id>cloud</id><active>true</active><protocol>http</protocol><host>'+escape(proxy.hostname)+'</host><port>'+str(proxy.port or 80)+'</port></proxy></proxies>'
s+='</settings>'
(pathlib.Path(sys.argv[1])/'maven-settings.xml').write_text(s)
PY
if [ -f /etc/ssl/certs/java/cacerts ]; then
  export MAVEN_OPTS="${MAVEN_OPTS:-} -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
fi
exec "$TOOLS_DIR/apache-maven-3.9.9/bin/mvn" -s "$TOOLS_DIR/maven-settings.xml" "-Dmaven.repo.local=$TOOLS_DIR/m2" "$@"
