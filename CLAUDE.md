# CLAUDE.md — Cofre Hero

## Propósito
App Android de finanzas personales gamificada, 100% offline. Un cofre dorado con detalles azules representa el ahorro para la meta **Hero Hunk 160R 4V**, **$15,000.00 MXN**. Debe transmitir "Estoy avanzando". Además hay un saldo semanal (lunes→domingo) del que se gasta y desde el cual se "pasa al cofre".

## Tecnologías
Kotlin 2.0.20 · Jetpack Compose (BOM 2024.09.02) · Material 3 personalizado (oscuro, dorado + azul, monoespaciada) · Room 2.6.1 (KSP, versión 2, `MIGRATION_1_2`) · DataStore · Coroutines/Flow · Navigation Compose · MVVM · Photo Picker · SoundPool. Sin backend/Firebase/login/Internet.

## Estructura (`app/src/main/java/com/example/cofre/`)
- `ChestApp.kt` — Application; una sola `AppDatabase`; expone repositorios, DataStore, media y backup.
- `MainActivity.kt` — hosts Compose; `onResume` → `weeksViewModel.refreshToday()`.
- `core/Formatters.kt` — `Money`, `DateFmt`. `core/WeekMath.kt` — semanas por `epochDay`, rangos, límites en ms. `core/FinalSecret.kt` — única regla pura de desbloqueo del final secreto.
- `data/Entities.kt` — `GoalEntity`, `TransactionEntity` (+`transferId`), `TxType` (+`TRANSFER_IN`).
- `data/WeekEntities.kt` — `WeekEntity`, `ExpenseEntity`, `TransferEntity`.
- `data/Daos.kt` · `data/WeekDaos.kt` — Room DAOs.
- `data/Migrations.kt` — `MIGRATION_1_2`.
- `data/ChestRepository.kt` — reglas del cofre.
- `data/WeekRepository.kt` — reglas de semanas, gastos y transferencias.
- `data/SettingsStore.kt` — preferencias visuales/sonoras.
- `data/MediaStore.kt` — almacenamiento privado local de imágenes.
- `data/BackupManager.kt` — ZIP versionado y restauración.
- `data/SoundEngine.kt` — efectos originales intercambiables.
- `ui/ChestViewModel.kt` · `ui/WeeksViewModel.kt` · `ui/SettingsViewModel.kt`.
- `ui/AppNav.kt` — navegación y coordinación; incluye la ruta protegida del final secreto.
- `ui/screens/` — onboarding, home, cofre, historial, semanas, análisis, fotos, ajustes y `FinalSecretScreen`.

Flujo: Room → Repository (Flow) → ViewModel (StateFlow) → Compose. Detalle de semana: `WeeksViewModel.observeDetail(id)`.

## Reglas monetarias (NO romper)
- Dinero SIEMPRE `Long` en centavos; nunca Float/Double para importes ni para almacenar importes animados o de gráficas. Fracciones/coordenadas visuales sí pueden ser Float.
- Cofre: `transactions.amountCents` con signo; saldo = `SUM`. El saldo puede superar la meta; el % visual se acota a 100.
- Semana: `disponible = initialCents − Σ expenses − Σ transfers`, calculado siempre (no se guarda). Debe ser ≥ 0 tras cualquier alta/edición/baja.
- Entrada de usuario: `Money.parse` (BigDecimal, ≤ 2 decimales).
- Todas las reglas financieras viven en los repositorios dentro de `db.withTransaction`.

## Transferencia semana → cofre (integridad)
- `transfers` es la operación canónica. Su reflejo en el cofre es UNA fila `transactions` con `type=TRANSFER_IN` y `transferId = transfers.id` (FK CASCADE + índice ÚNICO → relación 1:1).
- Ambas se crean/editan/borran juntas dentro de una transacción de Room.
- Editar/borrar solo por `WeekRepository`.
- Reducir o borrar una transferencia se rechaza si dejaría el saldo del cofre < 0.
- Fechas de gastos/transferencias deben caer dentro de su semana (lunes 00:00 → domingo 23:59:59.999, zona del dispositivo).

## Multimedia
- Las imágenes se copian a `filesDir/media` y no dependen del URI original.
- `photoUri` en gastos almacena una referencia interna local, no una URI remota.
- Los gastos limpian fotografías reemplazadas/eliminadas; las fotos nuevas no guardadas se limpian al salir del formulario.
- Fondos usan `ContentScale.Crop`, escala y desplazamiento; no deformar.

## Backup
- Formato `cofre-hero-backup`, versión 1.
- Incluye meta, movimientos, semanas, gastos, transferencias, fechas/horas, configuraciones, perfil, fondos y fotografías.
- Importación: validación completa → staging multimedia → transacción Room → restauración de preferencias/media.
- No usar migración destructiva.

## Final secreto
- Se desbloquea únicamente si `FinalSecret.isUnlocked(balanceCents, localDate)` devuelve `true`.
- Condiciones: fecha local del dispositivo >= 22/09/2027 y saldo real del cofre >= $15,000 MXN.
- La evaluación es dinámica, local y no depende de Internet.
- No almacenar un booleano de desbloqueo hardcodeado.
- El saldo puede superar $15,000; el progreso visual sigue en 100% pero el saldo real se conserva.
- Antes del desbloqueo no se muestran spoilers. Después existe una opción de reproducción.
- No documentar aquí el contenido narrativo exacto del final.

## Sonidos/animaciones
- `SoundEvent` centraliza botón, navegación, confirmación, moneda, aportación, transferencia, retiro y recompensa.
- `SoundEngine` usa `SoundPool` y espera la carga de samples antes de reproducir.
- Los efectos son originales y están en `res/raw`; no copiar obras/marcas.
- Animaciones breves y fluidas; el contador financiero anima una fracción, no convierte el importe a Float.

## Fase 4 completada
- Final secreto implementado y protegido.
- Auditoría financiera y de backup reforzada.
- Limpieza de fotografías temporales corregida.
- Manejo de errores de backup y límites de ZIP reforzado.
- Tests unitarios preparados para `Money`, `WeekMath` y `FinalSecret`.
- `FINAL_PROJECT_STATUS.md` y `PHASE_4_TEST_MATRIX.md` creados.
- `versionCode=4`, `versionName=0.4.0-final`.

## Compilación / limitaciones
- El proyecto queda preparado para Android Studio con `compileSdk=34`, `targetSdk=34`, Java/Kotlin 17 y Gradle 8.7.
- No afirmar compilación, tests ejecutados ni APK generado a partir de la revisión en Claude Chat. Esa validación debe hacerse posteriormente en Android Studio/CI con Android SDK disponible.

## No tocar
- No iniciar ni revelar el final secreto por caminos alternos.
- No añadir backend, login, servidor o sincronización.
- No eliminar semanas existentes.
- No modificar la semántica financiera de saldo, gastos, retiros o transferencias sin actualizar tests y auditoría.
