#!/usr/bin/env bash
# Prepara un GitHub Codespace para compilar Cofre Hero (JDK 17 + Android SDK + Gradle wrapper).
set -euo pipefail

cd "$(dirname "$0")/.."
PROJECT_DIR="$(pwd)"

export ANDROID_HOME="${ANDROID_HOME:-/usr/local/android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
CMDLINE_TOOLS_VERSION="11076708"   # commandline-tools 12.0
PLATFORM="android-34"
BUILD_TOOLS="34.0.0"

echo "==> Java:"; java -version 2>&1 | head -1

# 1) Android command-line tools
if [ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
  echo "==> Instalando Android command-line tools en $ANDROID_HOME"
  sudo mkdir -p "$ANDROID_HOME/cmdline-tools"
  sudo chown -R "$(id -u):$(id -g)" "$ANDROID_HOME"
  tmp="$(mktemp -d)"
  curl -fsSL -o "$tmp/cmdline-tools.zip" \
    "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
  unzip -q "$tmp/cmdline-tools.zip" -d "$tmp"
  rm -rf "$ANDROID_HOME/cmdline-tools/latest"
  mv "$tmp/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
  rm -rf "$tmp"
fi

SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"

# 2) Licencias + paquetes necesarios
echo "==> Aceptando licencias"
yes | "$SDKMANAGER" --licenses >/dev/null 2>&1 || true
echo "==> Instalando platform-tools, platforms;$PLATFORM, build-tools;$BUILD_TOOLS"
"$SDKMANAGER" --install "platform-tools" "platforms;$PLATFORM" "build-tools;$BUILD_TOOLS" >/dev/null

# 3) local.properties (ignorado por git)
echo "sdk.dir=$ANDROID_HOME" > "$PROJECT_DIR/local.properties"

# 4) Variables persistentes para nuevas terminales
if ! grep -q "ANDROID_HOME=" "$HOME/.bashrc" 2>/dev/null; then
  {
    echo "export ANDROID_HOME=$ANDROID_HOME"
    echo "export ANDROID_SDK_ROOT=$ANDROID_HOME"
    echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools'
  } >> "$HOME/.bashrc"
fi

# 5) Gradle wrapper (el .jar es binario; si falta, se descarga)
chmod +x "$PROJECT_DIR/gradlew"
if [ ! -f "$PROJECT_DIR/gradle/wrapper/gradle-wrapper.jar" ]; then
  echo "==> Descargando gradle-wrapper.jar"
  curl -fsSL -o "$PROJECT_DIR/gradle/wrapper/gradle-wrapper.jar" \
    "https://raw.githubusercontent.com/gradle/gradle/v8.7.0/gradle/wrapper/gradle-wrapper.jar"
fi

# 6) Precalienta Gradle (descarga distribución 8.7 y plugins)
echo "==> Precalentando Gradle"
"$PROJECT_DIR/gradlew" --version

cat <<MSG

Listo. Comandos útiles:
  ./gradlew testDebugUnitTest     # tests unitarios (Money, WeekMath, FinalSecret)
  ./gradlew assembleDebug         # genera app/build/outputs/apk/debug/app-debug.apk
  ./gradlew lintDebug             # análisis estático
MSG
