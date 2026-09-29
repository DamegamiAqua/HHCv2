# HANDOFF — Fase 1

## Archivos creados
- Raíz: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/wrapper/gradle-wrapper.properties`, `.gitignore`, `CLAUDE.md`, `HANDOFF_PHASE_1.md`
- `app/build.gradle.kts`, `AndroidManifest.xml`, `res/values/{strings,themes}.xml`
- Kotlin (`com.example.cofre`): `ChestApp`, `MainActivity`, `core/Formatters`, `data/{Entities,Daos,AppDatabase,SettingsStore,ChestRepository}`,
  `ui/{ChestViewModel,AppNav}`, `ui/theme/Theme`, `ui/screens/{Components,ChestVisuals,OnboardingScreen,HomeScreen,MovementFormScreen,HistoryScreen}`

## Qué funciona conceptualmente
Onboarding con saldo inicial → Home con cofre, saldo/meta/% (tope 100%, saldo real puede superar la meta) → aportar y retirar (confirmación,
sin exceder saldo, retiro negativo) → historial reciente primero → editar (cantidad, concepto, fecha, hora) y borrar con confirmación,
saldo recalculado por SUM. Todo en Long/centavos, persistido en Room + DataStore, sin Internet.

## Qué falta
Semanas, gráficas, fotos, fondos, sonidos, backup, final secreto, ícono de launcher, tests, `gradlew`/wrapper jar (Android Studio lo regenera).

## Qué debe revisar la siguiente fase
1. **Compilar por primera vez**: versiones de AGP/Kotlin/KSP/Compose en `build.gradle.kts` y posibles ajustes de imports/APIs.
2. Que Gradle sincronice con `gradle-wrapper.properties` (Gradle 8.7) y compileSdk 34.
3. Comportamiento visual en dispositivo: proporciones del sprite, coordenadas de monedas hacia el cofre, rebote, contador animado.
4. Flujos: onboarding → home; aportar (monedas al volver); retiro > saldo; editar fecha/hora; borrar que dejaría saldo negativo.
5. Decidir si validar saldo negativo cronológico (hoy solo se valida el saldo total).
6. Añadir tests de `Money.parse/format` y del repositorio antes de sumar funciones nuevas.
