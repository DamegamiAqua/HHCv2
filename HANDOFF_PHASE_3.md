# HANDOFF — Fase 3

## Estado
Fase 3 implementada sobre la base de Fase 2; no se reconstruyó el proyecto. No se generó APK ni se realizó una prueba física de la aplicación.

## Añadido
- Fotos de gastos con Android Photo Picker; copia local offline, miniatura muestreada, visor, reemplazo y eliminación.
- Foto de perfil local con alta/cambio/eliminación.
- Fondos de cofre, semanas y otras secciones; escala, posición X/Y y `RESTAURAR FONDO PREDETERMINADO`.
- Ajustes de sonidos: `SONIDOS ON/OFF` y volumen de efectos.
- Ocho efectos originales sintetizados en `res/raw`, cargados por `SoundEngine`; sustituibles sin tocar la lógica.
- Animaciones: contador, monedas/partículas, rebote de aportación/transferencia, pulso de retiro y transiciones de navegación.
- Backup ZIP versión 1: `backup.json` + archivos multimedia. Incluye meta, movimientos, semanas, gastos, transferencias, fechas/horas, preferencias, perfil, fondos y fotos.
- Importación con confirmación y validaciones de formato, versión, referencias multimedia, fechas, signos, saldos y relación transferencia↔cofre 1:1.
- `versionCode=3`, `versionName=0.3.0-phase3`.

## Integridad financiera
No se cambió la regla de Long/centavos ni la arquitectura de transferencias. Las operaciones financieras siguen en repositorios y `withTransaction`. No se añadió migración Room porque el esquema ya contenía `expenses.photoUri` y las preferencias nuevas viven en DataStore.

## Revisión estática realizada
- Se revisaron referencias a `photoUri`, firmas de ViewModels/repositorios y rutas de navegación.
- Se eliminó el uso de `Double` para progreso financiero; el backup usa enteros escalados para ajustes visuales.
- Se añadieron DAOs de lectura completa/limpieza para restauración.
- Se verificó que la restauración inserta semanas antes que gastos/transferencias y transferencias antes que sus movimientos espejo.
- Se verificó limpieza de la fotografía anterior al borrar/reemplazar un gasto después de que la operación de Room tenga éxito.

## Próxima fase — Fase 4
1. Crear tests unitarios de `Money`, `WeekMath`, `WeekRepository`, `buildSeries` y validación de backup.
2. Hacer una compilación/sincronización real en Android Studio y corregir cualquier diferencia de APIs/dependencias que aparezca.
3. Probar migración Room v1→v2 y restauración en dispositivo/emulador.
4. Revisar visualmente Photo Picker, visor, fondos, animaciones y sonido en distintos tamaños de pantalla.
5. Añadir accesibilidad y localización si se desea.
6. Implementar el ícono de launcher y, solo cuando todo lo anterior esté estable, preparar el trabajo del final secreto.
