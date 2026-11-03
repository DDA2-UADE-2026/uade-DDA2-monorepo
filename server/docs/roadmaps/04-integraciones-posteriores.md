# Integraciones de negocio M8

El equipo autorizó implementar también los procesos de negocio. Esta etapa usa **Contrato de eventos M8 para backend** y **Eventos M2 V1.79**, que reemplazan los nombres y reglas preliminares de la lista editada y de los tres PDFs anteriores. La fase de conectividad Core conserva su evidencia separada.

La [guía de eventos M8](../technical/m8-events.md) contiene los contratos, endpoints, permisos, migración, documentación AsyncAPI, activación y límites. No se registran contratos ni suscripciones automáticamente en Core.

## Etapa B1 Contratos y transporte

- [x] Data classes tipadas para tres publicaciones y siete consumos, con envelope Core y `eventVersion=1.0`.
- [x] Validación estructural y condicional; obligatorios-nullables, tipos numéricos y fechas con zona.
- [x] Publicar `updateTicketStatus`; consumir `ticketUpdated`. Usar `debtOverdue`, sin alias `overdueDebt`.
- [x] Beneficios APPROVED, SUSPENDED y FINALIZED, sin REJECTED.
- [x] Adaptadores sobre la misma outbox/inbox, ack tras commit y topología preexistente.
- [x] AsyncAPI autogenerada con Springwolf AMQP y visor de lectura; OpenAPI para HTTP.
- [ ] Comparar schemas desplegados y paquete de schemas M2 con Core antes de activar.

## Etapa B2 Documentación y beneficios

- [x] Pedido documental al presentar solicitud con requisitos, máximo diez y correlación por solicitud.
- [x] Exigir UUID canónico M1 antes de permitir operaciones que necesitan publicarlo; rollback completo si falta.
- [x] Referencias de expedientes/documentos separadas de archivos locales; COMPLETED no equivale a VALID.
- [x] Revisión administrativa protegida y estados documentales compartidos con evaluación/renovación.
- [x] Eventos por otorgamiento, renovación, suspensión, finalización y vencimiento; beneficio estable y amount presente aunque sea null.
- [ ] Coordinar con M1 la vinculación verificada de cuentas y la obtención autorizada del contenido documental.

## Etapa B3 Atención Ciudadana

- [x] Filtrar broadcast y reclasificación; aplicar updatedAt antes de cualquier cierre o reasignación.
- [x] Gestión identificada/anónima, snapshot ROUTED, STARTED por ciclo y reasignación explícita.
- [x] Flujo de información adicional, reapertura, cierre, duplicados, cancelación y escalamiento según M2 v1.79.
- [x] Asignación y vínculo administrativo con solicitud del mismo ciudadano; respuesta manual protegida.
- [x] Hooks de asignación de trabajador, programación/reprogramación de visita y resolución de solicitud.
- [ ] Validar un circuito completo con M2 a través de Core, incluyendo causalidad y no generación de eventos espejo.

## Etapa B4 Ciudadanos y Rentas

- [x] Snapshot y domicilio de ciudadanos vinculados; desconocidos se confirman sin crear cuentas.
- [x] BLOCKED restringe nuevas solicitudes y genera alerta administrativa, sin cambiar beneficios.
- [x] DECEASED finaliza ACTIVE y SUSPENDED y cierra solicitudes abiertas con CLOSED y motivo, según decisión del equipo. Cancela visitas, libera cupo y publica causalmente dentro de la misma transacción.
- [x] Contexto administrativo de deuda y exenciones, sin cambiar elegibilidad ni beneficios.
- [x] Resoluciones de exención fuera de orden y vínculo automático inequívoco; selección manual cuando hay varias prestaciones.
- [ ] Verificar interoperabilidad real con M1 y Rentas, incluyendo todos los casos condicionales.

## Etapa B5 Habilitación

- [ ] Aplicar y verificar la migración M8 sobre copia PostgreSQL.
- [ ] Confirmar publicaciones/suscripciones y schemas reales, HTTP, backlog y recuperación con Core.
- [ ] Desplegar la versión exacta con variables privadas y activar CORE_BUSINESS_ENABLED, publicador y consumidor deliberadamente.
- [ ] Guardar evidencia por eventId del recorrido completo y las pruebas de fallo, sin datos personales ni secretos.

La implementación local no acredita aceptación o procesamiento remoto. Mantener los pendientes externos abiertos hasta contar con evidencia; ver [habilitación Core](03-core-prueba-y-habilitacion.md).
