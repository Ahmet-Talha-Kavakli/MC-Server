#!/usr/bin/env bash
# =============================================================
# ReinaCraft - Unix/macOS setup script
# =============================================================
# Downloads Velocity + Paper + Via plugins, generates forwarding
# secret, first-boots Paper to create configs, injects secret,
# builds and deploys reina-hub plugin.
#
# Run from project root: ./scripts/setup-unix.sh
# =============================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

# --- Color helpers --------------------------------------------
C_RST=$'\033[0m'; C_BOLD=$'\033[1m'
C_RED=$'\033[31m'; C_GRN=$'\033[32m'; C_YLW=$'\033[33m'; C_CYN=$'\033[36m'
step()  { echo -e "${C_CYN}${C_BOLD}==>${C_RST}${C_BOLD} $*${C_RST}"; }
ok()    { echo -e "  ${C_GRN}✓${C_RST} $*"; }
warn()  { echo -e "  ${C_YLW}!${C_RST} $*"; }
fail()  { echo -e "  ${C_RED}✗${C_RST} $*"; exit 1; }

# --- Prerequisites --------------------------------------------
step "Checking prerequisites"

if ! command -v java >/dev/null 2>&1; then
    fail "Java not found. Install JDK 21 first: brew install --cask temurin@21"
fi
JAVA_MAJOR=$(java -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+).*/\1/')
if [ "$JAVA_MAJOR" -lt 21 ]; then
    fail "Java $JAVA_MAJOR found, need 21+. Install: brew install --cask temurin@21"
fi
ok "Java $JAVA_MAJOR detected ($(java -version 2>&1 | head -1))"

if ! command -v mvn >/dev/null 2>&1; then
    fail "Maven not found. Install: brew install maven"
fi
ok "Maven detected: $(mvn -v | head -1)"

for tool in curl unzip openssl; do
    command -v "$tool" >/dev/null 2>&1 || fail "$tool not found"
done
ok "curl, unzip, openssl present"

# --- PaperMC versions -----------------------------------------
step "Resolving latest Velocity + Paper builds"

VELOCITY_VERSION="3.3.0-SNAPSHOT"
PAPER_VERSION="1.21.11"

VELOCITY_BUILD=$(curl -s "https://api.papermc.io/v2/projects/velocity/versions/${VELOCITY_VERSION}" | python3 -c "import sys,json;print(json.load(sys.stdin)['builds'][-1])")
PAPER_BUILD=$(curl -s "https://api.papermc.io/v2/projects/paper/versions/${PAPER_VERSION}" | python3 -c "import sys,json;print(json.load(sys.stdin)['builds'][-1])")
ok "Velocity ${VELOCITY_VERSION} build ${VELOCITY_BUILD}"
ok "Paper ${PAPER_VERSION} build ${PAPER_BUILD}"

# --- Download server jars -------------------------------------
step "Downloading server jars (skipped if present)"

mkdir -p network/proxy network/hub network/bedwars network/proxy/plugins

if [ ! -f network/proxy/velocity.jar ]; then
    curl -sL -o network/proxy/velocity.jar \
        "https://api.papermc.io/v2/projects/velocity/versions/${VELOCITY_VERSION}/builds/${VELOCITY_BUILD}/downloads/velocity-${VELOCITY_VERSION}-${VELOCITY_BUILD}.jar"
    ok "velocity.jar downloaded"
else
    ok "velocity.jar exists - skipped"
fi

if [ ! -f network/hub/paper.jar ]; then
    curl -sL -o network/hub/paper.jar \
        "https://api.papermc.io/v2/projects/paper/versions/${PAPER_VERSION}/builds/${PAPER_BUILD}/downloads/paper-${PAPER_VERSION}-${PAPER_BUILD}.jar"
    cp network/hub/paper.jar network/bedwars/paper.jar
    ok "paper.jar downloaded (hub + bedwars)"
else
    ok "paper.jar exists - skipped"
fi

# --- Via plugins ----------------------------------------------
step "Downloading Via plugins"

dl_via() {
    local project="$1" name="$2"
    if [ -f "network/proxy/plugins/${name}.jar" ]; then ok "${name}.jar exists - skipped"; return; fi
    local info
    info=$(curl -s "https://api.modrinth.com/v2/project/${project}/version?loaders=%5B%22velocity%22%5D")
    local url
    url=$(echo "$info" | python3 -c "import sys,json;print(json.load(sys.stdin)[0]['files'][0]['url'])")
    curl -sL -o "network/proxy/plugins/${name}.jar" "$url"
    ok "${name}.jar downloaded"
}
dl_via viaversion   ViaVersion
dl_via viabackwards ViaBackwards
dl_via viarewind    ViaRewind

# --- Forwarding secret ----------------------------------------
step "Generating forwarding secret (if missing)"

if [ ! -f network/proxy/forwarding.secret ]; then
    openssl rand -base64 24 | tr -d '/+=' | head -c 32 > network/proxy/forwarding.secret
    ok "forwarding.secret generated"
else
    ok "forwarding.secret exists - skipped"
fi
SECRET=$(cat network/proxy/forwarding.secret)

# --- First-boot Paper backends to create configs --------------
boot_paper() {
    local dir="$1"
    if [ -f "${dir}/config/paper-global.yml" ]; then ok "${dir} configs exist - skipped boot"; return; fi
    step "First-booting ${dir} (creates configs, ~30s)"
    (cd "$dir" && (sleep 30 && echo "stop") | java -Xms512M -Xmx1G -jar paper.jar --nogui > /tmp/reinacraft-boot-$(basename "$dir").log 2>&1) || true
    if [ ! -f "${dir}/config/paper-global.yml" ]; then
        fail "${dir} first boot did not produce paper-global.yml - see /tmp/reinacraft-boot-$(basename "$dir").log"
    fi
    ok "${dir} first boot complete"
}
boot_paper network/hub
boot_paper network/bedwars

# --- Inject velocity secret into paper-global.yml -------------
step "Injecting velocity forwarding secret"

inject_secret() {
    local file="$1"
    python3 - <<PY
import re, pathlib
p = pathlib.Path("$file")
text = p.read_text(encoding="utf-8")
text = re.sub(r"(\n  velocity:\n    enabled: )false", r"\1true", text)
text = re.sub(r"(\n    secret: )''", r"\1'$SECRET'", text)
p.write_text(text, encoding="utf-8")
print("  patched", p)
PY
}
inject_secret network/hub/config/paper-global.yml
inject_secret network/bedwars/config/paper-global.yml
ok "velocity forwarding enabled with secret"

# --- Build plugin ---------------------------------------------
step "Building reina-hub plugin"

(cd plugins-src && mvn -q clean package)
cp plugins-src/reina-hub/target/reina-hub-0.1.0-SNAPSHOT.jar network/hub/plugins/
ok "reina-hub deployed to network/hub/plugins/"

# --- Done -----------------------------------------------------
echo
echo -e "${C_GRN}${C_BOLD}✓ Setup complete!${C_RST}"
echo
echo "Start everything with:"
echo "  ${C_BOLD}./scripts/start-all.sh${C_RST}"
echo
echo "Or individually:"
echo "  cd network/hub     && java -Xms1G -Xmx2G -jar paper.jar --nogui"
echo "  cd network/bedwars && java -Xms1G -Xmx2G -jar paper.jar --nogui"
echo "  cd network/proxy   && java -Xms512M -Xmx1G -jar velocity.jar"
echo
echo "Then connect to ${C_BOLD}localhost:25565${C_RST} from Minecraft (1.8 - 1.21.x)."
