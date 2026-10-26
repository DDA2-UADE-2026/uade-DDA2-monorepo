-- Tablero de indicadores (GET /api/admin/indicators).
-- No cambia el esquema: solo registra el permiso y se lo otorga a ADMIN.
-- Puede repetirse sin efectos adicionales.

BEGIN;

INSERT INTO public.permissions (name)
VALUES ('indicators:view')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r
JOIN public.permissions p ON p.name = 'indicators:view'
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

COMMIT;
