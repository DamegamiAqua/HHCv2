# Cofre Hero — Estado final del proyecto

## Estado de la Fase 4

Fase final implementada sobre la base existente de las Fases 1–3. No se reconstruyó el proyecto, no se eliminó funcionalidad financiera y no se generó APK.

La auditoría realizada en Claude Chat fue de lectura y revisión estática del árbol completo disponible en el ZIP. No se dispuso aquí de Android SDK/Android Studio para una compilación física ni para instalar o ejecutar la aplicación.

## Arquitectura

- MVVM.
- Jetpack Compose + Material 3 con tema oscuro pixelart moderno.
- Room como fuente de verdad financiera.
- DataStore para preferencias visuales/sonoras y perfil.
- `LocalMediaStore` para fotografías/fondos locales y offline.
- `BackupManager` para copia/restauración versionada.
- `SoundEngine` + `SoundEvent` para efectos sustituibles.
- `FinalSecret` como regla pura de desbloqueo, dependiente únicamente del saldo real y la fecha local del dispositivo.
- Navegación centralizada en `AppNav`.

## Tecnologías

Kotlin 2.0.20 · Android Gradle Plugin 8.5.2 · Gradle 8.7 · Jetpack Compose BOM 2024.09.02 · Material 3 · Room 2.6.1 + KSP · DataStore Preferences 1.1.1 · Navigation Compose 2.8.2 · Java 17.

`compileSdk = 34`, `targetSdk = 34`, `minSdk = 26`.

## Funcionalidades finales

### Cofre y finanzas

- Saldo inicial.
- Aportaciones.
- Retiros con confirmación.
- Edición y eliminación.
- Historial.
- Saldo real superior a la meta sin truncarlo.
- Progreso visual limitado a 100%.
- Semanas lunes→domingo.
- Gastos, edición y eliminación.
- Fechas y horas editables dentro de la semana.
- Transferencias semana→cofre atómicas 1:1.
- Validación de saldo de semana y saldo del cofre.

### Fotos y perfil

- Foto opcional por gasto.
- Selector Android Photo Picker.
- Copia local offline.
- Miniatura y visor ampliado.
- Reemplazo y eliminación.
- Limpieza de archivos no guardados.
- Foto de perfil local con cambio/eliminación.

### Fondos y estilo

- Fondo del cofre.
- Fondo semanal.
- Fondo de otras secciones.
- Escala y posición X/Y.
- Recorte sin deformación mediante `ContentScale.Crop`.
- Restauración al fondo predeterminado.
- Tema oscuro, dorado, azul y alto contraste visual.

### Sonidos y animaciones

- Sonidos ON/OFF.
- Volumen de efectos.
- Eventos de botón, navegación, confirmación, moneda, aportación, transferencia, retiro y recompensa.
- Efectos locales originales en `res/raw`.
- `SoundEngine` preparado para sustituir archivos sin cambiar la lógica.
- Animaciones breves para contador, cofres, monedas, retiros y transiciones.

### Backup

- Exportación ZIP offline.
- Formato `cofre-hero-backup` versionado.
- Incluye meta, movimientos, semanas, gastos, transferencias, fechas/horas, preferencias, perfil, fondos y fotografías.
- Restauración con confirmación antes de sobrescribir.
- Validación de versión, relaciones, signos monetarios, semanas, saldos, referencias multimedia y vínculo transferencia↔movimiento del cofre.
- Validación de ZIP y límites de tamaño para evitar entradas multimedia excesivas.

### Final secreto

- Desbloqueo dinámico y local.
- Requiere simultáneamente la fecha local establecida y el saldo real de la meta.
- No depende de Internet ni de un booleano de desbloqueo hardcodeado.
- La condición se vuelve a evaluar mientras la aplicación permanece abierta.
- El saldo puede continuar por encima de la meta.
- Una vez desbloqueado, existe reproducción posterior del contenido.
- No se documenta aquí el contenido narrativo exacto para preservar la sorpresa.

## Auditoría financiera

Se mantuvo `Long` en centavos para toda la lógica de dinero.

Las aportaciones y retiros se validan dentro de `db.withTransaction`. Las transferencias mantienen una fila canónica semanal y una fila espejo en el cofre, creadas/editadas/eliminadas juntas.

La edición y eliminación de transferencias sigue rechazando operaciones que dejen el cofre en negativo. Los gastos y transferencias continúan restringidos a la semana correspondiente.

La lógica del progreso visual nunca cambia el saldo real: al superar la meta, el porcentaje permanece en 100% y el excedente se conserva en `balanceCents`. Se añadió además idempotencia al onboarding para evitar duplicar el saldo inicial si DataStore falla después de que Room ya lo haya guardado.

## Auditoría de backup

El backup conserva los IDs y relaciones de Room necesarios para reconstruir el estado. Las fotografías/fondos se empaquetan como archivos locales y las preferencias conservan sus referencias internas.

La importación valida primero y solo después inicia la escritura de la base. Se mejoraron además las comprobaciones de IDs, meta, tipos monetarios, vínculos `TRANSFER_IN`, fechas, límites de archivo y que cada multimedia referenciada sea una imagen decodificable. El reemplazo de la carpeta multimedia conserva una ruta de recuperación si falla la sustitución.

## Auditoría de UX y recursos

- Cifras monetarias mostradas mediante `Money.format` con formato MXN.
- Imágenes con `ContentScale.Fit` o `Crop` según el contexto, sin deformar.
- Contraste reforzado con paleta dorado/azul sobre fondo oscuro.
- Mensajes de error orientados a la acción.
- Confirmación explícita antes de importar backup y antes de operaciones financieras sensibles.
- Accesibilidad básica mejorada mediante descripciones de fotografías no decorativas.
- Diseño con scroll vertical en pantallas de contenido largo para tamaños pequeños.

## Archivos principales añadidos/modificados en Fase 4

- `app/src/main/java/com/example/cofre/core/FinalSecret.kt`
- `app/src/main/java/com/example/cofre/ui/screens/FinalSecretScreen.kt`
- `app/src/main/java/com/example/cofre/ui/AppNav.kt`
- `app/src/main/java/com/example/cofre/ui/screens/HomeScreen.kt`
- `app/src/main/java/com/example/cofre/ui/screens/SettingsScreen.kt`
- `app/src/main/java/com/example/cofre/ui/screens/WeekEntryFormScreen.kt`
- `app/src/main/java/com/example/cofre/ui/screens/MediaUi.kt`
- `app/src/main/java/com/example/cofre/ui/ChestViewModel.kt`
- `app/src/main/java/com/example/cofre/ui/SettingsViewModel.kt`
- `app/src/main/java/com/example/cofre/data/SoundEngine.kt`
- `app/src/main/java/com/example/cofre/data/Daos.kt`
- `app/src/main/java/com/example/cofre/core/Results.kt`
- `app/src/main/java/com/example/cofre/data/BackupManager.kt`
- `app/build.gradle.kts`
- `CLAUDE.md`
- `FINAL_PROJECT_STATUS.md`
- `PHASE_4_TEST_MATRIX.md`

## Pruebas preparadas

Se incluyen pruebas unitarias para `FinalSecret`, `Money` y `WeekMath` en `app/src/test`. También se hizo una comprobación ejecutable independiente de los archivos Kotlin puros del núcleo; esto no sustituye la compilación/test real de Android.

La matriz de pruebas de integración/manuales se encuentra en `PHASE_4_TEST_MATRIX.md` y cubre saldo inicial, aportación, retiro válido/inválido, semanas, gasto, transferencia, edición, eliminación, semana anterior, movimiento atrasado, backup/restore, fotos, fondos, sonidos, persistencia, meta, excedente y las fechas críticas del final secreto.

## Cómo abrir en Android Studio

1. Abrir Android Studio.
2. Seleccionar **Open** y escoger la carpeta raíz que contiene `settings.gradle.kts`.
3. Esperar a que Gradle Sync termine antes de editar código.
4. Usar JDK 17 para el Gradle JVM del proyecto.
5. Verificar que el SDK de Android correspondiente a `compileSdk 34` esté instalado.

## GitHub Codespaces / CreateSpace

Abrir el repositorio en un Codespace y trabajar desde la carpeta raíz del proyecto (`CofreHero/`). El entorno debe disponer de JDK 17 y de un Android SDK compatible si se desea compilar desde consola.

El repositorio original no incluye `gradlew`/`gradle-wrapper.jar`; Android Studio puede importar el proyecto con la configuración de `gradle-wrapper.properties`, y un entorno de Codespaces con Gradle instalado puede regenerar el wrapper.

## Comandos recomendados

Regenerar wrapper si el entorno no lo tiene:

```bash
gradle wrapper --gradle-version 8.7
```

Después:

```bash
./gradlew :app:assembleDebug
./gradlew :app:test
```

Para una comprobación adicional del modelo de Room en un dispositivo/emulador, ejecutar desde Android Studio las pruebas instrumentadas apropiadas una vez que el entorno Android esté configurado.

## Limitaciones reales de Claude Chat

- No se realizó una compilación física de Android.
- No se ejecutaron los tests en Android/Gradle en un entorno con Android SDK.
- No se instaló ni ejecutó un APK.
- Las comprobaciones realizadas aquí son revisión estática del código, consistencia de referencias y preparación de pruebas/documentación.
