# Pruebas de integración pendientes
No se ejecutaron en el núcleo adjunto.

- Dos cierres desde la misma versión: uno confirma y uno recibe conflicto; no hay penalizaciones parciales.
- Reintento con igual commandId y hash: devuelve recibo; acumulados sin cambio.
- commandId reutilizado con otro hash o autor: rechazar.
- Fallo antes del commit: no hay confirmación ni modificación parcial.
- Fallo después del commit y antes de publicar: snapshot recupera estado confirmado.
- Proyección por jugador: no revelar mano rival ni orden del mazo.
- Totales igualan historial; Clásica tiene una ronda; Apuntado activa umbral desde 101.
- Límite preventivo de tamaño BSON; perfil MVP1 e inmutable por partida.
