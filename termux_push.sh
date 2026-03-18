#!/data/data/com.termux/files/usr/bin/bash
# ═══════════════════════════════════════════════════════════════
#  OMEGA PROTOCOL — Native Android Build & Push Script
#  Username : username051203
#  Email    : lewin.nick19@gmail.com
#  Run from : Termux on Android
# ═══════════════════════════════════════════════════════════════

set -e
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'
CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'
ok()  { echo -e "${GREEN}✓ $*${NC}"; }
warn(){ echo -e "${YELLOW}⚠ $*${NC}"; }
err() { echo -e "${RED}✗ $*${NC}"; exit 1; }
hdr() { echo -e "\n${CYAN}${BOLD}══ $* ══${NC}"; }

# ── CONFIG ────────────────────────────────────────────────────
GH_USER="username051203"
GH_EMAIL="lewin.nick19@gmail.com"
REPO_NAME="omega-protocol-native"
KEY_PASS="omega123"
KEY_ALIAS="omega"
PROJECT_DIR="$HOME/omega-native"
KEYSTORE="$HOME/omega.keystore"
ZIP_NAME="omega-native.zip"

# ── TOKEN ─────────────────────────────────────────────────────
if [ -z "$GH_TOKEN" ]; then
  echo -e "${YELLOW}Enter your GitHub Personal Access Token (repo + workflow scopes):${NC}"
  read -s GH_TOKEN && echo
fi
[ -z "$GH_TOKEN" ] && err "No token. Aborting."
grep -q "GH_TOKEN" ~/.bashrc 2>/dev/null || echo "export GH_TOKEN=\"$GH_TOKEN\"" >> ~/.bashrc
grep -q "GH_USER"  ~/.bashrc 2>/dev/null || echo "export GH_USER=\"$GH_USER\""   >> ~/.bashrc
export GH_TOKEN GH_USER

# ═══════════════════════════════════════════════════════════════
hdr "Step 1 — Install dependencies"
# ═══════════════════════════════════════════════════════════════
pkg update -y -q 2>/dev/null
pkg install -y git gh curl unzip python 2>/dev/null | grep -E "Downloading|already" || true
pip install Pillow --break-system-packages -q 2>/dev/null || true

if [ ! -d "$HOME/storage" ]; then
  warn "Run 'termux-setup-storage' and tap Allow if you haven't already, then re-run this script."
fi
ok "Dependencies ready"

# ═══════════════════════════════════════════════════════════════
hdr "Step 2 — Git configuration"
# ═══════════════════════════════════════════════════════════════
git config --global user.name  "$GH_USER"
git config --global user.email "$GH_EMAIL"
git config --global credential.helper store
git config --global init.defaultBranch main
echo "https://${GH_USER}:${GH_TOKEN}@github.com" > ~/.git-credentials
ok "Git configured for $GH_USER / $GH_EMAIL"

# ═══════════════════════════════════════════════════════════════
hdr "Step 3 — Create GitHub repo (if not exists)"
# ═══════════════════════════════════════════════════════════════
REPO_CHECK=$(curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: token $GH_TOKEN" \
  "https://api.github.com/repos/$GH_USER/$REPO_NAME")

if [ "$REPO_CHECK" = "200" ]; then
  ok "Repo already exists — $GH_USER/$REPO_NAME"
else
  curl -s -X POST \
    -H "Authorization: token $GH_TOKEN" \
    -H "Content-Type: application/json" \
    https://api.github.com/user/repos \
    -d "{\"name\":\"$REPO_NAME\",\"private\":false,\"description\":\"Omega Protocol — Native Java Android App\"}" \
    | python3 -c "import sys,json; r=json.load(sys.stdin); print('Created:', r.get('full_name','?'))"
fi

# ═══════════════════════════════════════════════════════════════
hdr "Step 4 — Extract project zip"
# ═══════════════════════════════════════════════════════════════
# Look for the zip in common locations
ZIP_PATH=""
for loc in \
  "$HOME/storage/downloads/$ZIP_NAME" \
  "/sdcard/Download/$ZIP_NAME" \
  "$HOME/$ZIP_NAME"; do
  if [ -f "$loc" ]; then ZIP_PATH="$loc"; break; fi
done

if [ -n "$ZIP_PATH" ]; then
  ok "Found zip at $ZIP_PATH"
  rm -rf "$PROJECT_DIR"
  mkdir -p "$PROJECT_DIR"
  unzip -q "$ZIP_PATH" -d "$PROJECT_DIR"
  ok "Project extracted to $PROJECT_DIR"
else
  warn "Zip not found in Downloads — using existing directory if present"
  [ -d "$PROJECT_DIR" ] || err "No project directory found at $PROJECT_DIR. Download omega-native.zip first."
fi

# ═══════════════════════════════════════════════════════════════
hdr "Step 5 — Generate signing keystore"
# ═══════════════════════════════════════════════════════════════
if [ -f "$KEYSTORE" ]; then
  ok "Keystore already exists at $KEYSTORE"
else
  if command -v keytool &>/dev/null; then
    keytool -genkeypair -v \
      -keystore "$KEYSTORE" \
      -alias "$KEY_ALIAS" \
      -keyalg RSA -keysize 2048 \
      -validity 10000 \
      -storepass "$KEY_PASS" \
      -keypass   "$KEY_PASS" \
      -dname "CN=OmegaProtocol, O=OmegaProtocol, C=US" 2>/dev/null
    ok "Keystore generated"
  else
    warn "keytool not found — install openjdk-17 or generate keystore manually"
    warn "Then upload KEYSTORE_BASE64 secret manually at:"
    warn "https://github.com/$GH_USER/$REPO_NAME/settings/secrets/actions"
  fi
fi

# ═══════════════════════════════════════════════════════════════
hdr "Step 6 — Upload GitHub Secrets"
# ═══════════════════════════════════════════════════════════════
echo "$GH_TOKEN" | gh auth login --with-token 2>/dev/null || true

if [ -f "$KEYSTORE" ]; then
  base64 "$KEYSTORE" | tr -d '\n' | \
    gh secret set KEYSTORE_BASE64 --repo "$GH_USER/$REPO_NAME"
  echo "$KEY_PASS"  | gh secret set KEYSTORE_PASS --repo "$GH_USER/$REPO_NAME"
  echo "$KEY_ALIAS" | gh secret set KEY_ALIAS     --repo "$GH_USER/$REPO_NAME"
  echo "$KEY_PASS"  | gh secret set KEY_PASS      --repo "$GH_USER/$REPO_NAME"
  ok "All 4 secrets uploaded"
else
  warn "Keystore missing — secrets not uploaded. Upload manually."
fi

# ═══════════════════════════════════════════════════════════════
hdr "Step 7 — Generate app icons"
# ═══════════════════════════════════════════════════════════════
ICON_SRC=""
for loc in \
  "$HOME/storage/downloads/appicon.png" \
  "/sdcard/Download/appicon.png" \
  "$HOME/appicon.png"; do
  if [ -f "$loc" ]; then ICON_SRC="$loc"; break; fi
done

python3 << PYEOF
try:
    from PIL import Image, ImageDraw, ImageFilter
    import os

    src = "$ICON_SRC"
    base = "$PROJECT_DIR/app/src/main/res"
    sizes = {
        'mipmap-mdpi':48,'mipmap-hdpi':72,'mipmap-xhdpi':96,
        'mipmap-xxhdpi':144,'mipmap-xxxhdpi':192
    }

    if src and os.path.exists(src):
        img = Image.open(src).convert("RGBA")
        print(f"Using custom icon from {src}")
    else:
        # Generate default purple-gradient icon
        size = 512
        img = Image.new('RGBA', (size,size), (0,0,0,0))
        d = ImageDraw.Draw(img)
        # Gradient background circle
        for r in range(size//2, 0, -1):
            ratio = r / (size//2)
            R = int(124 * ratio + 26 * (1-ratio))
            G = int(111 * ratio + 10 * (1-ratio))
            B = int(255 * ratio + 64 * (1-ratio))
            d.ellipse([size//2-r, size//2-r, size//2+r, size//2+r], fill=(R,G,B,255))
        # Omega symbol approximation
        d.ellipse([160,120,352,312], outline=(255,255,255,220), width=28)
        d.rectangle([180,280,332,340], fill=(0,0,0,0))  # cut bottom
        d.rectangle([160,320,232,360], fill=(255,255,255,220))
        d.rectangle([280,320,352,360], fill=(255,255,255,220))
        print("Generated default icon")

    for folder, s in sizes.items():
        os.makedirs(f"{base}/{folder}", exist_ok=True)
        out = img.resize((s,s), Image.LANCZOS)
        out.save(f"{base}/{folder}/ic_launcher.png")
        out.save(f"{base}/{folder}/ic_launcher_round.png")
    print("Icons written to all densities")
except Exception as e:
    print(f"Icon generation failed: {e} — using defaults")
PYEOF
ok "App icons done"

# ═══════════════════════════════════════════════════════════════
hdr "Step 8 — Create gradlew wrapper"
# ═══════════════════════════════════════════════════════════════
# GitHub Actions uses gradle/actions/setup-gradle, so gradlew
# just needs to exist and be executable
if [ ! -f "$PROJECT_DIR/gradlew" ]; then
cat > "$PROJECT_DIR/gradlew" << 'GRADLEW'
#!/bin/sh
# Stub gradlew — real build handled by GitHub Actions setup-gradle
exec gradle "$@"
GRADLEW
chmod +x "$PROJECT_DIR/gradlew"
fi
touch "$PROJECT_DIR/gradle/wrapper/gradle-wrapper.jar"
ok "Gradlew stub created"

# ═══════════════════════════════════════════════════════════════
hdr "Step 9 — Git init and push"
# ═══════════════════════════════════════════════════════════════
cd "$PROJECT_DIR"

if [ ! -d ".git" ]; then
  git init
  git remote add origin "https://${GH_USER}:${GH_TOKEN}@github.com/${GH_USER}/${REPO_NAME}.git"
  ok "Git initialised"
else
  git remote set-url origin "https://${GH_USER}:${GH_TOKEN}@github.com/${GH_USER}/${REPO_NAME}.git"
  ok "Remote URL updated"
fi

git add .
COMMIT_MSG="Omega Protocol Native v3 — full Room DB, 9 screens, rival engine, ALEXANDRIUS, cosmetics, particles, streak, share card"
git commit -m "$COMMIT_MSG" --author="$GH_USER <$GH_EMAIL>" 2>/dev/null \
  || git commit --allow-empty -m "Update" --author="$GH_USER <$GH_EMAIL>"
git branch -M main
git push -u origin main --force
ok "Pushed to https://github.com/$GH_USER/$REPO_NAME"

# ═══════════════════════════════════════════════════════════════
hdr "Step 10 — Monitor build"
# ═══════════════════════════════════════════════════════════════
echo "Waiting 20 seconds for Actions to start..."
sleep 20

if command -v gh &>/dev/null; then
  RUN_ID=$(gh run list --repo "$GH_USER/$REPO_NAME" --limit 1 --json databaseId \
           -q '.[0].databaseId' 2>/dev/null || echo "")
  if [ -n "$RUN_ID" ]; then
    echo -e "${CYAN}Watching run $RUN_ID (Ctrl+C to stop watching — build continues in cloud)${NC}"
    gh run watch "$RUN_ID" --repo "$GH_USER/$REPO_NAME" || true
  fi
fi

# ═══════════════════════════════════════════════════════════════
echo ""
echo -e "${GREEN}${BOLD}╔══════════════════════════════════════╗${NC}"
echo -e "${GREEN}${BOLD}║   BUILD SUBMITTED SUCCESSFULLY! 🚀  ║${NC}"
echo -e "${GREEN}${BOLD}╚══════════════════════════════════════╝${NC}"
echo ""
echo -e "Actions: ${CYAN}https://github.com/$GH_USER/$REPO_NAME/actions${NC}"
echo -e "APK:     ${CYAN}https://github.com/$GH_USER/$REPO_NAME/releases/download/latest/OmegaProtocol.apk${NC}"
echo ""
echo -e "${YELLOW}Download APK after ~4 minutes:${NC}"
echo -e "${YELLOW}curl -L -H \"Authorization: token \$GH_TOKEN\" \\${NC}"
echo -e "${YELLOW}  \"https://github.com/$GH_USER/$REPO_NAME/releases/download/latest/OmegaProtocol.apk\" \\${NC}"
echo -e "${YELLOW}  -o /sdcard/Download/OmegaProtocol.apk && echo DONE${NC}"
echo ""
echo "────────────────────────────────────────"
echo "  FUTURE UPDATES — just run:"
echo ""
echo "  cd $PROJECT_DIR"
echo "  git add ."
echo "  git commit -m 'update'"
echo "  git push"
echo "────────────────────────────────────────"
