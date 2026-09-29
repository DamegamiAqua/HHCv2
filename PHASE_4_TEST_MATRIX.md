# Fase 4 — Matriz de pruebas preparada

Esta matriz describe los casos que deben ejecutarse posteriormente en Android Studio/emulador/dispositivo. No se marca ningún caso como ejecutado aquí.

| Caso | Estado esperado |
|---|---|
| $0 inicial | Cofre válido, saldo $0.00, progreso 0%. |
| Reintento de onboarding tras fallo de persistencia | No duplica el saldo inicial ya escrito en Room. |
| +$250.00 | Saldo $250.00; registro de aportación; contador y animación. |
| Retiro parcial | Saldo decrementa exactamente la cantidad retirada. |
| Retiro inválido | Se rechaza sin modificar saldo. |
| Semana nueva | Solo se crea la semana correspondiente; prompt una vez por lunes. |
| Gasto válido | Reduce disponible exactamente el importe registrado. |
| Gasto inválido > disponible | Se rechaza sin modificar saldo semanal. |
| Transferencia | Reduce disponible semanal y crea exactamente un espejo `TRANSFER_IN` en cofre. |
| Editar transferencia | Ambas partes cambian juntas y conservan el 1:1. |
| Eliminar transferencia | Ambas partes se eliminan juntas; el disponible semanal se recupera. |
| Eliminar/reducir transferencia tras retiro del cofre | Operación rechazada si deja el cofre negativo. |
| Edición | Cambio de importe/concepto/fecha conservando invariantes. |
| Eliminación | Operación confirmada y saldo derivado correcto. |
| Semana anterior | Se puede crear una semana pasada, sin semanas futuras. |
| Movimiento atrasado | Fecha pasada dentro del periodo permitido conserva consistencia. |
| Fecha fuera de semana | Gasto/transferencia rechazados. |
| Backup export | JSON + multimedia + configuración presentes. |
| Backup restore | Se reconstruye el estado y se conserva transferencia↔cofre. |
| Backup corrupto | Mensaje claro; no sobrescribir datos actuales. |
| Backup versión no soportada | Mensaje claro; no sobrescribir datos actuales. |
| Backup con imagen corrupta | Rechazar antes de reemplazar datos/media actuales. |
| Foto de gasto | Miniatura, visor, reemplazo y eliminación; sin deformación. |
| Foto seleccionada y salida sin guardar | Archivo temporal eliminado. |
| Foto de perfil | Elegir/cambiar/eliminar y persistir tras reinicio. |
| Fondo personalizado | Imagen sin deformación; escala/posición; restauración. |
| Sonidos | ON/OFF, volumen y eventos correctos. |
| Persistencia | Cerrar/reabrir conserva Room, DataStore y multimedia. |
| $15,000 + 21/09/2027 | Final secreto bloqueado. |
| $14,999 + 22/09/2027 | Final secreto bloqueado. |
| $15,000 + 22/09/2027 | Final secreto desbloqueado. |
| $15,250 + fecha posterior | Final secreto desbloqueado. |
| $18,000 / $15,000 | Saldo real $18,000; progreso visual 100%; excedente conservado. |
| Cambio de fecha a medianoche con app abierta | La condición del final se actualiza dinámicamente sin Internet. |
