# HANDOFF — Fase 2 (Semanas)

## Funcionalidades añadidas
- Semanas lun→dom con saldo disponible variable; el prompt "¿Cuánto dinero tienes disponible esta semana?" aparece al abrir la app en una semana nueva (una vez por lunes) o desde la tarjeta de Home.
- Gastos: cantidad, concepto libre, fecha y hora (editables, también en semanas pasadas); alta, edición y borrado con confirmación.
- Resumen: SALDO INICIAL / GASTADO / AHORRADO / DISPONIBLE (Home, historial de semanas, detalle, análisis).
- PASAR AL COFRE: cantidad + concepto; valida contra el disponible; confirma; anima las monedas en Home. Edición y borrado revierten ambos lados.
- Historial de semanas ("28 sep – 4 oct"), entrada a semanas pasadas, "+ SEMANA ANTERIOR", edición del saldo inicial.
- Análisis semanal: gráfica escalonada pixelart (disponible, gasto acumulado, ahorro acumulado) + promedio diario y % ahorrado; reactiva vía Flow.

## Nuevas entidades y relaciones
- `weeks(id, startEpochDay UNIQUE, initialCents, createdAt, updatedAt)`
- `expenses(id, weekId→weeks CASCADE, amountCents>0, concept, occurredAt, photoUri?, createdAt, updatedAt)`
- `transfers(id, weekId→weeks CASCADE, amountCents>0, concept, occurredAt, createdAt, updatedAt)`
- `transactions` ganó `transferId?` (→transfers CASCADE, índice ÚNICO) y `TxType.TRANSFER_IN`. Relación transfer↔fila del cofre = 1:1.
- Room v2 con `MIGRATION_1_2`. Disponible de semana = inicial − Σgastos − Σtransferencias (derivado, no almacenado).

## Archivos nuevos
`core/WeekMath.kt`, `data/{WeekEntities,WeekDaos,Migrations,WeekRepository}.kt`, `ui/{WeeksViewModel,Results}.kt`,
`ui/screens/{PickerDialogs,WeekSetupScreen,WeeksHistoryScreen,WeekDetailScreen,WeekEntryFormScreen,WeekAnalysisScreen}.kt`, `HANDOFF_PHASE_2.md`.

## Archivos modificados
`CLAUDE.md`, `ChestApp.kt`, `MainActivity.kt`, `data/{Entities,Daos,AppDatabase,ChestRepository}.kt`, `ui/{AppNav,ChestViewModel}.kt`,
`ui/screens/{HomeScreen,HistoryScreen,MovementFormScreen,Components}.kt`.

## Puntos que la Fase 3 debe respetar
1. Dinero solo `Long`/centavos. Nada de Double, tampoco en gráficas o backups (serializar como enteros).
2. Transferencias: nunca tocar `transactions` con `transferId` fuera de `WeekRepository`; siempre en `withTransaction` y en pares. Un backup/restauración debe conservar el 1:1 y validarlo al importar.
3. `photoUri` ya existe en `expenses`: Fase 3 solo agrega captura/almacenamiento/UI (si guarda archivos, respaldarlos y limpiar al borrar un gasto).
4. Cambios de esquema → subir versión de Room + `Migration` exacta; no usar migración destructiva.
5. Semanas se conservan siempre (no hay borrado de semanas). Fechas de movimientos deben quedar dentro de su semana.
6. `AppNav` es el único punto que coordina ambos ViewModels; mantener `pendingBurst` como evento consumible.
7. Sonidos/fondos deben engancharse a eventos existentes (`pendingBurst`, confirmaciones) sin tocar reglas financieras.

## Riesgos / revisar al compilar
- **No compilado ni probado.** Primera compilación: imports, APIs Material3 (`SelectableDates`, `DatePicker`) y KSP.
- Verificar que `MIGRATION_1_2` coincide con el esquema generado (Room lanza excepción si difiere) y probar actualizar desde una BD v1.
- Probar: lunes nuevo → prompt; gasto/transferencia > disponible; transferencia y luego retiro del cofre → intentar borrar la transferencia (debe rechazar); editar fecha fuera de semana; semana pasada.
- Sin tests unitarios todavía (recomendado: `WeekRepository`, `WeekMath`, `buildSeries`).
