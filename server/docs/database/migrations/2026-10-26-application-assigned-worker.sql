-- Asignacion y reasignacion del trabajador de una solicitud
-- (PUT /api/admin/applications/{id}/assigned-worker).
-- No cambia el esquema: application.assigned_worker_user_id ya existe.
-- Puede repetirse sin efectos adicionales.

BEGIN;

INSERT INTO public.permissions (name)
VALUES ('applications:management:assign')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r
JOIN public.permissions p ON p.name = 'applications:management:assign'
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

COMMIT;
