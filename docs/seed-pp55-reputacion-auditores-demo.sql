-- =============================================================================
-- SEED DATA para demo PP-55: métricas de reputación del auditor
-- Agrega empresas de varios sectores, cuentas de prueba y auditores con
-- auditorías terminales para validar auditorías completadas, tiempo promedio
-- de respuesta y distribución por sector.
-- =============================================================================
-- Contraseña de todas las cuentas creadas por este seed: CarbonHub123!
-- Ejecutar contra una base local/dev. No usar en producción.

-- ============ EMPRESAS DE PRUEBA ============

INSERT INTO empresas (
    id,
    nombre_empresa,
    cedula_juridica,
    sector_industrial,
    pais,
    cantidad_empleados,
    correo_corporativo,
    sitio_web,
    logo_url,
    slug,
    descripcion,
    nivel_ecologico,
    estado,
    fecha_registro
)
VALUES
('bb550001-0000-0000-0000-000000000001', 'Café del Valle S.A.', '3-101-955001', 'AGROINDUSTRIA', 'Costa Rica', 48, 'admin+cafe-valle@carbonhub.test', 'https://demo.carbonhub.test/cafe-valle', NULL, 'cafe-del-valle-demo', 'Beneficio cafetalero con inventario GEI anual.', 'PLATA', 'ACTIVO', NOW()),
('bb550001-0000-0000-0000-000000000002', 'Transporte Verde del Istmo S.A.', '3-101-955002', 'TRANSPORTE', 'Costa Rica', 95, 'admin+transporte-verde@carbonhub.test', 'https://demo.carbonhub.test/transporte-verde', NULL, 'transporte-verde-istmo-demo', 'Operador logístico con flotilla híbrida.', 'BRONCE', 'ACTIVO', NOW()),
('bb550001-0000-0000-0000-000000000003', 'Textiles Naranjo S.R.L.', '3-101-955003', 'MANUFACTURA', 'Costa Rica', 130, 'admin+textiles-naranjo@carbonhub.test', 'https://demo.carbonhub.test/textiles-naranjo', NULL, 'textiles-naranjo-demo', 'Planta manufacturera con programa de eficiencia energética.', 'PLATA', 'ACTIVO', NOW()),
('bb550001-0000-0000-0000-000000000004', 'Mercado Circular S.A.', '3-101-955004', 'RETAIL', 'Costa Rica', 62, 'admin+mercado-circular@carbonhub.test', 'https://demo.carbonhub.test/mercado-circular', NULL, 'mercado-circular-demo', 'Cadena retail con medición de huella por sucursal.', 'BRONCE', 'ACTIVO', NOW()),
('bb550001-0000-0000-0000-000000000005', 'Servicios Climáticos Integrados S.A.', '3-101-955005', 'SERVICIOS', 'Costa Rica', 38, 'admin+servicios-climaticos@carbonhub.test', 'https://demo.carbonhub.test/servicios-climaticos', NULL, 'servicios-climaticos-demo', 'Empresa de consultoría y servicios ambientales.', 'ORO', 'ACTIVO', NOW()),
('bb550001-0000-0000-0000-000000000006', 'Hotel Bahía Azul S.A.', '3-101-955006', 'HOTELERIA', 'Costa Rica', 84, 'admin+bahia-azul@carbonhub.test', 'https://demo.carbonhub.test/bahia-azul', NULL, 'hotel-bahia-azul-demo', 'Hotel costero con operación de turismo sostenible.', 'ORO', 'ACTIVO', NOW()),
('bb550001-0000-0000-0000-000000000007', 'Fundación Bosque Vivo', '3-101-955007', 'OTRO', 'Costa Rica', 24, 'admin+bosque-vivo@carbonhub.test', 'https://demo.carbonhub.test/bosque-vivo', NULL, 'fundacion-bosque-vivo-demo', 'Organización de restauración ecológica y educación ambiental.', 'PLATA', 'ACTIVO', NOW())
ON CONFLICT (cedula_juridica) DO UPDATE
SET nombre_empresa = EXCLUDED.nombre_empresa,
    sector_industrial = EXCLUDED.sector_industrial,
    correo_corporativo = EXCLUDED.correo_corporativo,
    sitio_web = EXCLUDED.sitio_web,
    descripcion = EXCLUDED.descripcion,
    nivel_ecologico = EXCLUDED.nivel_ecologico,
    estado = EXCLUDED.estado;

-- ============ USUARIOS ADMIN DE EMPRESA ============

INSERT INTO usuarios (
    id,
    email,
    nombre,
    apellidos,
    nombre_visible,
    rol,
    estado,
    metodo_auth,
    password_hash,
    configuracion_completa,
    intentos_fallidos,
    fecha_registro,
    empresa_id
)
VALUES
('cc550001-0000-0000-0000-000000000001', 'admin+cafe-valle@carbonhub.test', 'Admin', 'Café del Valle', 'Admin Café del Valle', 'ADMINISTRADOR_EMPRESA', 'ACTIVO', 'CORREO', '$2a$10$yp3lvXnW989O6Di1Zigl3ukTkir7AdnPdSC.37l3gW9Vmfg2r6eTe', true, 0, NOW(), 'bb550001-0000-0000-0000-000000000001'),
('cc550001-0000-0000-0000-000000000002', 'admin+transporte-verde@carbonhub.test', 'Admin', 'Transporte Verde', 'Admin Transporte Verde', 'ADMINISTRADOR_EMPRESA', 'ACTIVO', 'CORREO', '$2a$10$yp3lvXnW989O6Di1Zigl3ukTkir7AdnPdSC.37l3gW9Vmfg2r6eTe', true, 0, NOW(), 'bb550001-0000-0000-0000-000000000002'),
('cc550001-0000-0000-0000-000000000003', 'admin+textiles-naranjo@carbonhub.test', 'Admin', 'Textiles Naranjo', 'Admin Textiles Naranjo', 'ADMINISTRADOR_EMPRESA', 'ACTIVO', 'CORREO', '$2a$10$yp3lvXnW989O6Di1Zigl3ukTkir7AdnPdSC.37l3gW9Vmfg2r6eTe', true, 0, NOW(), 'bb550001-0000-0000-0000-000000000003'),
('cc550001-0000-0000-0000-000000000004', 'admin+mercado-circular@carbonhub.test', 'Admin', 'Mercado Circular', 'Admin Mercado Circular', 'ADMINISTRADOR_EMPRESA', 'ACTIVO', 'CORREO', '$2a$10$yp3lvXnW989O6Di1Zigl3ukTkir7AdnPdSC.37l3gW9Vmfg2r6eTe', true, 0, NOW(), 'bb550001-0000-0000-0000-000000000004'),
('cc550001-0000-0000-0000-000000000005', 'admin+servicios-climaticos@carbonhub.test', 'Admin', 'Servicios Climáticos', 'Admin Servicios Climáticos', 'ADMINISTRADOR_EMPRESA', 'ACTIVO', 'CORREO', '$2a$10$yp3lvXnW989O6Di1Zigl3ukTkir7AdnPdSC.37l3gW9Vmfg2r6eTe', true, 0, NOW(), 'bb550001-0000-0000-0000-000000000005'),
('cc550001-0000-0000-0000-000000000006', 'admin+bahia-azul@carbonhub.test', 'Admin', 'Bahía Azul', 'Admin Bahía Azul', 'ADMINISTRADOR_EMPRESA', 'ACTIVO', 'CORREO', '$2a$10$yp3lvXnW989O6Di1Zigl3ukTkir7AdnPdSC.37l3gW9Vmfg2r6eTe', true, 0, NOW(), 'bb550001-0000-0000-0000-000000000006'),
('cc550001-0000-0000-0000-000000000007', 'admin+bosque-vivo@carbonhub.test', 'Admin', 'Bosque Vivo', 'Admin Bosque Vivo', 'ADMINISTRADOR_EMPRESA', 'ACTIVO', 'CORREO', '$2a$10$yp3lvXnW989O6Di1Zigl3ukTkir7AdnPdSC.37l3gW9Vmfg2r6eTe', true, 0, NOW(), 'bb550001-0000-0000-0000-000000000007')
ON CONFLICT (email) DO UPDATE
SET nombre = EXCLUDED.nombre,
    apellidos = EXCLUDED.apellidos,
    nombre_visible = EXCLUDED.nombre_visible,
    rol = EXCLUDED.rol,
    estado = EXCLUDED.estado,
    metodo_auth = EXCLUDED.metodo_auth,
    password_hash = EXCLUDED.password_hash,
    configuracion_completa = EXCLUDED.configuracion_completa,
    empresa_id = EXCLUDED.empresa_id;

-- ============ USUARIOS AUDITORES ============

INSERT INTO usuarios (
    id,
    email,
    nombre,
    apellidos,
    nombre_visible,
    rol,
    estado,
    metodo_auth,
    password_hash,
    configuracion_completa,
    intentos_fallidos,
    fecha_registro
)
VALUES
('aa550001-0000-0000-0000-000000000001', 'auditor+daniela-quiros@carbonhub.test', 'Daniela', 'Quirós Vega', 'Ing. Daniela Quirós Vega', 'AUDITOR_CERTIFICADO', 'ACTIVO', 'CORREO', '$2a$10$yp3lvXnW989O6Di1Zigl3ukTkir7AdnPdSC.37l3gW9Vmfg2r6eTe', true, 0, NOW()),
('aa550001-0000-0000-0000-000000000002', 'auditor+juana-rojas@carbonhub.test', 'Juana', 'Rojas', 'Juana Rojas', 'AUDITOR_CERTIFICADO', 'ACTIVO', 'CORREO', '$2a$10$yp3lvXnW989O6Di1Zigl3ukTkir7AdnPdSC.37l3gW9Vmfg2r6eTe', true, 0, NOW()),
('aa550001-0000-0000-0000-000000000003', 'auditor+mateo-salas@carbonhub.test', 'Mateo', 'Salas Mora', 'Mateo Salas Mora', 'AUDITOR_CERTIFICADO', 'ACTIVO', 'CORREO', '$2a$10$yp3lvXnW989O6Di1Zigl3ukTkir7AdnPdSC.37l3gW9Vmfg2r6eTe', true, 0, NOW()),
('aa550001-0000-0000-0000-000000000004', 'auditor+valeria-monge@carbonhub.test', 'Valeria', 'Monge Arias', 'Valeria Monge Arias', 'AUDITOR_CERTIFICADO', 'ACTIVO', 'CORREO', '$2a$10$yp3lvXnW989O6Di1Zigl3ukTkir7AdnPdSC.37l3gW9Vmfg2r6eTe', true, 0, NOW()),
('aa550001-0000-0000-0000-000000000005', 'auditor+andres-castro@carbonhub.test', 'Andrés', 'Castro Solís', 'Andrés Castro Solís', 'AUDITOR_CERTIFICADO', 'ACTIVO', 'CORREO', '$2a$10$yp3lvXnW989O6Di1Zigl3ukTkir7AdnPdSC.37l3gW9Vmfg2r6eTe', true, 0, NOW())
ON CONFLICT (email) DO UPDATE
SET nombre = EXCLUDED.nombre,
    apellidos = EXCLUDED.apellidos,
    nombre_visible = EXCLUDED.nombre_visible,
    rol = EXCLUDED.rol,
    estado = EXCLUDED.estado,
    metodo_auth = EXCLUDED.metodo_auth,
    password_hash = EXCLUDED.password_hash,
    configuracion_completa = EXCLUDED.configuracion_completa;

-- ============ PERFILES DE AUDITOR ============

INSERT INTO perfiles_auditor (
    id,
    auditor_id,
    foto_perfil,
    disponible,
    auditorias_completadas,
    calificacion_promedio,
    total_resenas,
    tiempo_respuesta_horas,
    anios_experiencia,
    provincia,
    descripcion_profesional,
    actualizado_en
)
VALUES
('dd550001-0000-0000-0000-000000000001', 'aa550001-0000-0000-0000-000000000001', NULL, true, 5, 4.8, 6, 30, 12, 'SAN_JOSE', 'Auditora ambiental certificada con experiencia en agroindustria, manufactura y gestión hídrica.', NOW()),
('dd550001-0000-0000-0000-000000000002', 'aa550001-0000-0000-0000-000000000002', NULL, true, 5, 4.6, 4, 24, 8, 'HEREDIA', 'Especialista en inventarios GEI para servicios, retail y organizaciones de impacto social.', NOW()),
('dd550001-0000-0000-0000-000000000003', 'aa550001-0000-0000-0000-000000000003', NULL, true, 2, 4.4, 3, 36, 9, 'ALAJUELA', 'Auditor con foco en logística, transporte bajo en carbono y eficiencia de flotas.', NOW()),
('dd550001-0000-0000-0000-000000000004', 'aa550001-0000-0000-0000-000000000004', NULL, true, 2, 4.9, 3, 18, 10, 'GUANACASTE', 'Auditora de turismo sostenible y operación hotelera con enfoque en energía renovable.', NOW()),
('dd550001-0000-0000-0000-000000000005', 'aa550001-0000-0000-0000-000000000005', NULL, false, 2, 4.1, 2, 42, 6, 'CARTAGO', 'Auditor técnico para manufactura, retail y planes de reducción de emisiones.', NOW())
ON CONFLICT ON CONSTRAINT uk_perfiles_auditor_auditor DO UPDATE
SET disponible = EXCLUDED.disponible,
    auditorias_completadas = EXCLUDED.auditorias_completadas,
    calificacion_promedio = EXCLUDED.calificacion_promedio,
    total_resenas = EXCLUDED.total_resenas,
    tiempo_respuesta_horas = EXCLUDED.tiempo_respuesta_horas,
    anios_experiencia = EXCLUDED.anios_experiencia,
    provincia = EXCLUDED.provincia,
    descripcion_profesional = EXCLUDED.descripcion_profesional,
    actualizado_en = EXCLUDED.actualizado_en;

-- ============ ESPECIALIDADES Y ZONAS ============

INSERT INTO perfil_auditor_especialidades (perfil_auditor_id, especialidad)
SELECT perfiles.id, datos.especialidad
FROM (VALUES
    ('aa550001-0000-0000-0000-000000000001', 'AGROINDUSTRIA'),
    ('aa550001-0000-0000-0000-000000000001', 'ENERGIA_RENOVABLE'),
    ('aa550001-0000-0000-0000-000000000001', 'MANUFACTURA'),
    ('aa550001-0000-0000-0000-000000000002', 'SERVICIOS'),
    ('aa550001-0000-0000-0000-000000000002', 'AGROINDUSTRIA'),
    ('aa550001-0000-0000-0000-000000000003', 'LOGISTICA_TRANSPORTE'),
    ('aa550001-0000-0000-0000-000000000003', 'ENERGIA_RENOVABLE'),
    ('aa550001-0000-0000-0000-000000000004', 'TURISMO_SOSTENIBLE'),
    ('aa550001-0000-0000-0000-000000000004', 'ENERGIA_RENOVABLE'),
    ('aa550001-0000-0000-0000-000000000005', 'MANUFACTURA'),
    ('aa550001-0000-0000-0000-000000000005', 'LOGISTICA_TRANSPORTE')
) AS datos(auditor_id, especialidad)
JOIN perfiles_auditor perfiles ON perfiles.auditor_id = datos.auditor_id::uuid
WHERE NOT EXISTS (
    SELECT 1
    FROM perfil_auditor_especialidades existentes
    WHERE existentes.perfil_auditor_id = perfiles.id
      AND existentes.especialidad = datos.especialidad
);

INSERT INTO perfil_auditor_zonas (perfil_auditor_id, zona)
SELECT perfiles.id, datos.zona
FROM (VALUES
    ('aa550001-0000-0000-0000-000000000001', 'SAN_JOSE'),
    ('aa550001-0000-0000-0000-000000000001', 'ALAJUELA'),
    ('aa550001-0000-0000-0000-000000000001', 'CARTAGO'),
    ('aa550001-0000-0000-0000-000000000002', 'HEREDIA'),
    ('aa550001-0000-0000-0000-000000000002', 'SAN_JOSE'),
    ('aa550001-0000-0000-0000-000000000003', 'ALAJUELA'),
    ('aa550001-0000-0000-0000-000000000003', 'PUNTARENAS'),
    ('aa550001-0000-0000-0000-000000000004', 'GUANACASTE'),
    ('aa550001-0000-0000-0000-000000000004', 'PUNTARENAS'),
    ('aa550001-0000-0000-0000-000000000005', 'CARTAGO'),
    ('aa550001-0000-0000-0000-000000000005', 'LIMON')
) AS datos(auditor_id, zona)
JOIN perfiles_auditor perfiles ON perfiles.auditor_id = datos.auditor_id::uuid
WHERE NOT EXISTS (
    SELECT 1
    FROM perfil_auditor_zonas existentes
    WHERE existentes.perfil_auditor_id = perfiles.id
      AND existentes.zona = datos.zona
);

-- ============ AUDITORÍAS TERMINALES PARA MÉTRICAS PP-55 ============

INSERT INTO solicitudes_auditoria (
    id,
    empresa_id,
    tipo_certificacion,
    periodo_inicio,
    periodo_fin,
    descripcion_solicitud,
    estado,
    fecha_creacion,
    auditor_id,
    origen_asignacion,
    fecha_asignacion,
    fecha_aceptacion,
    fecha_auditoria_realizada,
    fecha_carga_reporte,
    resultado_auditoria,
    observaciones,
    fecha_resolucion,
    fecha_vencimiento_cert,
    version
)
VALUES
-- Daniela: mezcla de agroindustria, manufactura, transporte y servicios.
('ee550001-0000-0000-0000-000000000001', 'bb550001-0000-0000-0000-000000000001', 'INICIAL', '2025-01-01', '2025-12-31', 'Inventario GEI de beneficio cafetalero.', 'CERTIFICACION_EMITIDA', '2026-01-05 14:00:00+00', 'aa550001-0000-0000-0000-000000000001', 'EMPRESA', '2026-01-05 16:00:00+00', '2026-01-06 08:00:00+00', '2026-01-20', '2026-01-21 18:00:00+00', 'APROBADA', NULL, '2026-01-22 10:00:00+00', '2028-01-22', 0),
('ee550001-0000-0000-0000-000000000002', 'bb550001-0000-0000-0000-000000000001', 'RENOVACION', '2024-01-01', '2024-12-31', 'Renovación de inventario GEI cafetalero.', 'CERTIFICACION_EMITIDA', '2026-02-02 13:00:00+00', 'aa550001-0000-0000-0000-000000000001', 'EMPRESA', '2026-02-02 14:00:00+00', '2026-02-03 09:00:00+00', '2026-02-16', '2026-02-17 17:00:00+00', 'APROBADA', NULL, '2026-02-18 11:00:00+00', '2028-02-18', 0),
('ee550001-0000-0000-0000-000000000003', 'bb550001-0000-0000-0000-000000000003', 'INICIAL', '2025-01-01', '2025-12-31', 'Auditoría de planta textil.', 'CERTIFICACION_EMITIDA', '2026-03-04 09:00:00+00', 'aa550001-0000-0000-0000-000000000001', 'EMPRESA', '2026-03-04 11:00:00+00', '2026-03-05 09:00:00+00', '2026-03-19', '2026-03-20 16:00:00+00', 'APROBADA', NULL, '2026-03-21 10:00:00+00', '2028-03-21', 0),
('ee550001-0000-0000-0000-000000000004', 'bb550001-0000-0000-0000-000000000002', 'INICIAL', '2025-01-01', '2025-12-31', 'Revisión de flota logística.', 'CERTIFICACION_EMITIDA', '2026-04-06 10:00:00+00', 'aa550001-0000-0000-0000-000000000001', 'EMPRESA', '2026-04-06 12:00:00+00', '2026-04-07 14:00:00+00', '2026-04-22', '2026-04-23 16:00:00+00', 'APROBADA', NULL, '2026-04-24 09:00:00+00', '2028-04-24', 0),
('ee550001-0000-0000-0000-000000000005', 'bb550001-0000-0000-0000-000000000005', 'INICIAL', '2025-01-01', '2025-12-31', 'Verificación de servicios climáticos.', 'OBSERVACIONES_PENDIENTES', '2026-05-02 08:00:00+00', 'aa550001-0000-0000-0000-000000000001', 'EMPRESA', '2026-05-02 10:00:00+00', '2026-05-03 09:00:00+00', '2026-05-17', '2026-05-18 15:00:00+00', 'OBSERVACIONES', 'Actualizar evidencias de alcance 3 antes de la emisión final.', '2026-05-19 11:00:00+00', NULL, 0),
-- Juana: mayor peso en servicios para reproducir el caso visto en pantalla.
('ee550001-0000-0000-0000-000000000006', 'bb550001-0000-0000-0000-000000000005', 'INICIAL', '2025-01-01', '2025-12-31', 'Auditoría de consultoría ambiental.', 'CERTIFICACION_EMITIDA', '2026-01-08 09:00:00+00', 'aa550001-0000-0000-0000-000000000002', 'EMPRESA', '2026-01-08 10:00:00+00', '2026-01-08 18:00:00+00', '2026-01-24', '2026-01-25 17:00:00+00', 'APROBADA', NULL, '2026-01-26 12:00:00+00', '2028-01-26', 0),
('ee550001-0000-0000-0000-000000000007', 'bb550001-0000-0000-0000-000000000005', 'RENOVACION', '2024-01-01', '2024-12-31', 'Renovación de consultoría ambiental.', 'CERTIFICACION_EMITIDA', '2026-02-08 09:00:00+00', 'aa550001-0000-0000-0000-000000000002', 'EMPRESA', '2026-02-08 10:00:00+00', '2026-02-08 16:00:00+00', '2026-02-22', '2026-02-23 13:00:00+00', 'APROBADA', NULL, '2026-02-24 10:00:00+00', '2028-02-24', 0),
('ee550001-0000-0000-0000-000000000008', 'bb550001-0000-0000-0000-000000000007', 'INICIAL', '2025-01-01', '2025-12-31', 'Auditoría de organización ambiental.', 'CERTIFICACION_EMITIDA', '2026-03-08 09:00:00+00', 'aa550001-0000-0000-0000-000000000002', 'EMPRESA', '2026-03-08 10:00:00+00', '2026-03-09 08:00:00+00', '2026-03-21', '2026-03-22 13:00:00+00', 'APROBADA', NULL, '2026-03-23 10:00:00+00', '2028-03-23', 0),
('ee550001-0000-0000-0000-000000000009', 'bb550001-0000-0000-0000-000000000004', 'INICIAL', '2025-01-01', '2025-12-31', 'Auditoría de cadena retail.', 'CERTIFICACION_EMITIDA', '2026-04-08 09:00:00+00', 'aa550001-0000-0000-0000-000000000002', 'EMPRESA', '2026-04-08 10:00:00+00', '2026-04-09 10:00:00+00', '2026-04-23', '2026-04-24 13:00:00+00', 'APROBADA', NULL, '2026-04-25 10:00:00+00', '2028-04-25', 0),
('ee550001-0000-0000-0000-000000000010', 'bb550001-0000-0000-0000-000000000005', 'INICIAL', '2025-01-01', '2025-12-31', 'Verificación de servicios profesionales.', 'CERTIFICACION_EMITIDA', '2026-05-08 09:00:00+00', 'aa550001-0000-0000-0000-000000000002', 'EMPRESA', '2026-05-08 10:00:00+00', '2026-05-08 18:00:00+00', '2026-05-23', '2026-05-24 13:00:00+00', 'APROBADA', NULL, '2026-05-25 10:00:00+00', '2028-05-25', 0),
-- Resto de auditores.
('ee550001-0000-0000-0000-000000000011', 'bb550001-0000-0000-0000-000000000002', 'INICIAL', '2025-01-01', '2025-12-31', 'Inventario de transporte de carga.', 'CERTIFICACION_EMITIDA', '2026-02-10 09:00:00+00', 'aa550001-0000-0000-0000-000000000003', 'EMPRESA', '2026-02-10 12:00:00+00', '2026-02-11 08:00:00+00', '2026-02-24', '2026-02-25 12:00:00+00', 'APROBADA', NULL, '2026-02-26 10:00:00+00', '2028-02-26', 0),
('ee550001-0000-0000-0000-000000000012', 'bb550001-0000-0000-0000-000000000002', 'RENOVACION', '2024-01-01', '2024-12-31', 'Renovación de flota.', 'OBSERVACIONES_PENDIENTES', '2026-03-10 09:00:00+00', 'aa550001-0000-0000-0000-000000000003', 'EMPRESA', '2026-03-10 12:00:00+00', '2026-03-11 09:00:00+00', '2026-03-24', '2026-03-25 12:00:00+00', 'OBSERVACIONES', 'Falta ampliar el respaldo de consumo de diésel por ruta.', '2026-03-26 10:00:00+00', NULL, 0),
('ee550001-0000-0000-0000-000000000013', 'bb550001-0000-0000-0000-000000000006', 'INICIAL', '2025-01-01', '2025-12-31', 'Auditoría hotelera.', 'CERTIFICACION_EMITIDA', '2026-02-14 09:00:00+00', 'aa550001-0000-0000-0000-000000000004', 'EMPRESA', '2026-02-14 10:00:00+00', '2026-02-14 20:00:00+00', '2026-03-02', '2026-03-03 12:00:00+00', 'APROBADA', NULL, '2026-03-04 10:00:00+00', '2028-03-04', 0),
('ee550001-0000-0000-0000-000000000014', 'bb550001-0000-0000-0000-000000000006', 'RENOVACION', '2024-01-01', '2024-12-31', 'Renovación hotelera.', 'CERTIFICACION_EMITIDA', '2026-04-14 09:00:00+00', 'aa550001-0000-0000-0000-000000000004', 'EMPRESA', '2026-04-14 10:00:00+00', '2026-04-14 18:00:00+00', '2026-04-28', '2026-04-29 12:00:00+00', 'APROBADA', NULL, '2026-04-30 10:00:00+00', '2028-04-30', 0),
('ee550001-0000-0000-0000-000000000015', 'bb550001-0000-0000-0000-000000000003', 'INICIAL', '2025-01-01', '2025-12-31', 'Auditoría de manufactura.', 'CERTIFICACION_EMITIDA', '2026-03-16 09:00:00+00', 'aa550001-0000-0000-0000-000000000005', 'EMPRESA', '2026-03-16 10:00:00+00', '2026-03-18 08:00:00+00', '2026-04-01', '2026-04-02 12:00:00+00', 'APROBADA', NULL, '2026-04-03 10:00:00+00', '2028-04-03', 0),
('ee550001-0000-0000-0000-000000000016', 'bb550001-0000-0000-0000-000000000004', 'INICIAL', '2025-01-01', '2025-12-31', 'Auditoría retail.', 'CERTIFICACION_EMITIDA', '2026-05-16 09:00:00+00', 'aa550001-0000-0000-0000-000000000005', 'EMPRESA', '2026-05-16 10:00:00+00', '2026-05-18 09:00:00+00', '2026-06-01', '2026-06-02 12:00:00+00', 'APROBADA', NULL, '2026-06-03 10:00:00+00', '2028-06-03', 0)
ON CONFLICT (id) DO UPDATE
SET empresa_id = EXCLUDED.empresa_id,
    tipo_certificacion = EXCLUDED.tipo_certificacion,
    periodo_inicio = EXCLUDED.periodo_inicio,
    periodo_fin = EXCLUDED.periodo_fin,
    descripcion_solicitud = EXCLUDED.descripcion_solicitud,
    estado = EXCLUDED.estado,
    fecha_creacion = EXCLUDED.fecha_creacion,
    auditor_id = EXCLUDED.auditor_id,
    origen_asignacion = EXCLUDED.origen_asignacion,
    fecha_asignacion = EXCLUDED.fecha_asignacion,
    fecha_aceptacion = EXCLUDED.fecha_aceptacion,
    fecha_auditoria_realizada = EXCLUDED.fecha_auditoria_realizada,
    fecha_carga_reporte = EXCLUDED.fecha_carga_reporte,
    resultado_auditoria = EXCLUDED.resultado_auditoria,
    observaciones = EXCLUDED.observaciones,
    fecha_resolucion = EXCLUDED.fecha_resolucion,
    fecha_vencimiento_cert = EXCLUDED.fecha_vencimiento_cert,
    version = EXCLUDED.version;

-- ============ REPORTES DE AUDITORÍA ============

INSERT INTO reportes_auditoria (id, solicitud_id, nombre_archivo, tipo_contenido, tamanio_bytes, fecha_carga)
SELECT datos.id::uuid, datos.solicitud_id::uuid, datos.nombre_archivo, 'application/pdf', datos.tamanio_bytes, datos.fecha_carga::timestamp with time zone
FROM (VALUES
    ('ff550001-0000-0000-0000-000000000001', 'ee550001-0000-0000-0000-000000000001', 'reporte-cafe-valle-2025.pdf', 884000, '2026-01-21 18:00:00+00'),
    ('ff550001-0000-0000-0000-000000000002', 'ee550001-0000-0000-0000-000000000003', 'reporte-textiles-naranjo-2025.pdf', 792000, '2026-03-20 16:00:00+00'),
    ('ff550001-0000-0000-0000-000000000003', 'ee550001-0000-0000-0000-000000000006', 'reporte-servicios-climaticos-2025.pdf', 650000, '2026-01-25 17:00:00+00'),
    ('ff550001-0000-0000-0000-000000000004', 'ee550001-0000-0000-0000-000000000013', 'reporte-hotel-bahia-azul-2025.pdf', 720000, '2026-03-03 12:00:00+00')
) AS datos(id, solicitud_id, nombre_archivo, tamanio_bytes, fecha_carga)
ON CONFLICT (solicitud_id) DO UPDATE
SET nombre_archivo = EXCLUDED.nombre_archivo,
    tipo_contenido = EXCLUDED.tipo_contenido,
    tamanio_bytes = EXCLUDED.tamanio_bytes,
    fecha_carga = EXCLUDED.fecha_carga;

-- ============ FIN DEL SEED ============
-- Cuentas destacadas:
-- auditor+daniela-quiros@carbonhub.test / CarbonHub123!
-- auditor+juana-rojas@carbonhub.test / CarbonHub123!
-- admin+cafe-valle@carbonhub.test / CarbonHub123!
-- admin+servicios-climaticos@carbonhub.test / CarbonHub123!
--
-- Verificación sugerida:
-- SELECT sector_industrial, COUNT(*) FROM empresas WHERE cedula_juridica LIKE '3-101-955%' GROUP BY sector_industrial;
-- SELECT u.email, p.auditorias_completadas, p.calificacion_promedio FROM usuarios u JOIN perfiles_auditor p ON p.auditor_id = u.id WHERE u.email LIKE 'auditor+%@carbonhub.test';
-- SELECT u.email, e.sector_industrial, COUNT(*) FROM solicitudes_auditoria s JOIN usuarios u ON u.id = s.auditor_id JOIN empresas e ON e.id = s.empresa_id WHERE s.id::text LIKE 'ee550001-%' GROUP BY u.email, e.sector_industrial ORDER BY u.email, e.sector_industrial;
