-- Monitoreo Core: registra el permiso y lo asigna a ADMIN, sin modificar otras cuentas o roles.
-- Alternativa manual cuando BOOTSTRAP_ADMIN_ENABLED=false. Puede repetirse.
BEGIN;

INSERT INTO public.permissions (name)
VALUES ('integrations:core:view')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r
JOIN public.permissions p ON p.name = 'integrations:core:view'
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

COMMIT;
