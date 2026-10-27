-- Consulta ciudadana: permiso propio. Ejecutar antes del despliegue sobre el esquema existente.
-- Idempotente; no crea otorgamientos ni modifica estados, cupos o historiales.
BEGIN;
INSERT INTO public.permissions (name) VALUES ('applications:own:benefits:view') ON CONFLICT (name) DO NOTHING;
INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM public.roles r
JOIN public.permissions p ON p.name = 'applications:own:benefits:view'
WHERE r.name IN ('CIUDADANO', 'ADMIN') ON CONFLICT DO NOTHING;
COMMIT;
