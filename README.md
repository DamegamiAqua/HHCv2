# Cofre Hero

App Android (Kotlin · Jetpack Compose · Room) de ahorro gamificado. Ver `CLAUDE.md` para reglas de dominio.

## Ejecutar en GitHub Codespaces

1. Sube este proyecto a un repositorio y abre **Code → Codespaces → Create codespace**.
2. El contenedor (`.devcontainer/`) instala JDK 17 y el Android SDK (platform 34, build-tools 34.0.0) y prepara Gradle
   automáticamente (`scripts/setup-codespace.sh`, tarda unos minutos la primera vez).
3. En la terminal:

```bash
./gradlew testDebugUnitTest   # tests unitarios
./gradlew assembleDebug       # APK en app/build/outputs/apk/debug/app-debug.apk
./gradlew lintDebug           # análisis estático (opcional)
```

Si la terminal ya estaba abierta o falló la preparación: `bash scripts/setup-codespace.sh`.

> Un Codespace no tiene pantalla ni emulador Android. Descarga el APK (clic derecho → *Download*) e instálalo en tu
> teléfono o en un emulador local.
