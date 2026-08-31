-- Datos iniciales para PostgreSQL, exclusivamente para el entorno de pruebas.
-- Ejecutar manualmente DESPUES de que Hibernate cree el esquema actual.
-- No crea tablas, no migra la base y no elimina ni reemplaza datos existentes.
-- Puede repetirse: los registros y las relaciones existentes se conservan.
-- Los hashes de admin y viewer son los del init.sql original; no se cambian
-- las contraseñas. No utilizar estas cuentas de prueba en producción.

BEGIN;

-- Permisos utilizados actualmente por los controladores del backend.
-- Las bajas de programas, ediciones, beneficios y requisitos usan
-- programs:management:edit; no existe un permiso de delete separado en uso.
INSERT INTO permissions (name)
VALUES ('permissions:view'),
       ('roles:view'),
       ('roles:create'),
       ('roles:edit'),
       ('roles:delete'),
       ('users:view'),
       ('users:create'),
       ('users:edit'),
       ('users:delete'),
       ('programs:management:view'),
       ('programs:management:create'),
       ('programs:management:edit'),
       ('enrollment-periods:management:view'),
       ('enrollment-periods:management:create'),
       ('enrollment-periods:management:edit'),
       ('enrollment-periods:management:change-status')
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (name)
VALUES ('ADMIN'),
       ('VIEWER')
ON CONFLICT (name) DO NOTHING;

-- ADMIN conserva acceso a todos los permisos registrados, como en el original.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- VIEWER queda sin permisos administrativos en una base nueva.
-- El original referenciaba example-form:view, que ya no utiliza el backend.
-- No se inventa una política de lectura: sus permisos se configuran manualmente.
-- Si ya tenía permisos en una base existente, este script no los elimina.

-- Cuentas locales: username y password_hash deben estar ambos presentes.
-- external_citizen_id queda NULL: ninguna cuenta se vincula por este script.
INSERT INTO users (username,
                   password_hash,
                   external_citizen_id,
                   name,
                   email,
                   active,
                   created_at,
                   updated_at)
VALUES ('admin',
        '$2a$12$zRbLXcEFmJjMLYfCRANVf.PLF0AK.YTQ9qbubNXNfdoS4rprmNZF.',
        NULL,
        'Administrator',
        'admin@example.com',
        TRUE,
        CURRENT_TIMESTAMP,
        NULL),
       ('viewer',
        '$2a$12$CwjUGshh7GSzhd.oFe2H.OJaq1uwK3WECMo2m0S.oRZ0KFVczyRji',
        NULL,
        'Viewer User',
        'viewer@example.com',
        TRUE,
        CURRENT_TIMESTAMP,
        NULL)
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.username = 'admin' AND r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.username = 'viewer' AND r.name = 'VIEWER'
ON CONFLICT DO NOTHING;

-- No se insertan citizen_snapshot ni identidades externas: son opcionales
-- y la integración con Ciudadanos todavía no está implementada.
-- El rol CIUDADANO y sus permisos quedan pendientes de configuración manual.
-- No existe una columna active_role ni una tabla de sesiones: el rol activo
-- se incluye en el JWT. Estas cuentas tienen un solo rol en una base nueva.
-- Para probar la selección de rol, asignar explícitamente un segundo rol
-- al usuario mediante el ABM; el siguiente login pedirá seleccionarlo.

COMMIT;
