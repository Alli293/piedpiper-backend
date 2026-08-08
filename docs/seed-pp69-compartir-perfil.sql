-- =============================================================================
-- SEED DATA para demo PP-69: Compartición del perfil público
-- Agrega certificaciones, insignias y slug_historico a la empresa Cahuita
-- para verificar todas las secciones del perfil público y la compartición.
-- =============================================================================
-- Prerequisito: ejecutar primero seed-pp86-demo.sql (crea la empresa Cahuita)
-- Empresa Cahuita ID: a1000001-0000-0000-0000-000000000008
-- Auditor ID: da69b327-e866-4960-a55f-1431abd1f958

-- ============ ACTUALIZAR CAHUITA: logo ============

UPDATE empresas
SET logo_url = 'https://carbonhub.app/images/cahuita-logo.png'
WHERE id = 'a1000001-0000-0000-0000-000000000008';

-- ============ USUARIO AUDITOR (necesario para FK de certificaciones) ============

INSERT INTO usuarios (id, email, nombre, apellidos, rol, estado, metodo_auth, configuracion_completa, intentos_fallidos, fecha_registro)
VALUES
('da69b327-e866-4960-a55f-1431abd1f958', 'auditor.seed@carbonhub.cr', 'Auditor', 'Demo CarbonHub', 'AUDITOR_CERTIFICADO', 'ACTIVO', 'CORREO', true, 0, NOW())
ON CONFLICT (id) DO NOTHING;

-- ============ CERTIFICACIONES PARA CAHUITA ============
-- 2 certificaciones ACTIVA adicionales (solo ACTIVA está permitida por check constraint)

INSERT INTO certificaciones (id, id_auditoria, empresa_id, auditor_id, tipo, fecha_emision, fecha_vencimiento, estado, credencial_jwt, indice_estado)
VALUES
-- Certificación vigente: Inventario GEI
('b2000001-0000-0000-0000-000000000016', 'c3000001-0000-0000-0000-000000000016', 'a1000001-0000-0000-0000-000000000008', 'da69b327-e866-4960-a55f-1431abd1f958', 'INVENTARIO_GEI', '2025-08-01 00:00:00+00', '2027-08-01', 'ACTIVA', 'eyJ0eXAiOiJKV1QiLCJhbGciOiJFZERTQSJ9.seed-pp69-016', 300),
-- Certificación vigente: Carbono Neutral
('b2000001-0000-0000-0000-000000000017', 'c3000001-0000-0000-0000-000000000017', 'a1000001-0000-0000-0000-000000000008', 'da69b327-e866-4960-a55f-1431abd1f958', 'CARBONO_NEUTRAL', '2025-02-15 00:00:00+00', '2027-02-15', 'ACTIVA', 'eyJ0eXAiOiJKV1QiLCJhbGciOiJFZERTQSJ9.seed-pp69-017', 301)
ON CONFLICT (id_auditoria) DO NOTHING;

-- ============ INSIGNIAS PARA CAHUITA ============
-- id_insignia = 3 corresponde a "Excelencia climatica empresarial" (del bootstrap)
-- Cahuita ahora tiene 3 certificaciones activas → bronce

INSERT INTO insignias_empresa (id, empresa_id, id_insignia, nivel_insignia, fecha_obtencion)
VALUES
('f6000001-0000-0000-0000-000000000001', 'a1000001-0000-0000-0000-000000000008', 3, 'bronce', '2025-04-15 00:00:00+00')
ON CONFLICT ON CONSTRAINT uk_insignias_empresa_empresa_insignia_nivel DO NOTHING;

-- ============ SLUG HISTORICO ============
-- Simula que Cahuita antes se llamaba "parque-cahuita" y cambió su slug a "cahuita"
-- Esto permite probar el redirect 301 al visitar /empresa/parque-cahuita/reputacion

INSERT INTO slugs_historicos (id, empresa_id, slug_anterior, fecha_cambio)
VALUES
('e7000001-0000-0000-0000-000000000001', 'a1000001-0000-0000-0000-000000000008', 'parque-cahuita', '2025-06-01 00:00:00+00')
ON CONFLICT ON CONSTRAINT slugs_historicos_slug_anterior_key DO NOTHING;

-- ============ EMISIONES PARA CAHUITA ============
-- Registros de electricidad para los últimos 3 años (evolución de huella)

INSERT INTO emisiones (id, empresa_id, categoria, fecha_actividad, titulo, carbon_kg, carbon_mt, factor_emision_id, estimated_at, created_at, created_by_user_id, electricity_value, electricity_unit)
VALUES
-- 2023
('a8000001-0000-0000-0000-000000000001', 'a1000001-0000-0000-0000-000000000008', 'ELECTRICIDAD', '2023-03-15', 'Consumo eléctrico Q1 2023', 450.000, 0.450, 'electricity-supply_grid-source_residual_mix', '2023-03-15 12:00:00+00', '2023-03-15 12:00:00+00', NULL, 3200.000, 'KWH'),
('a8000001-0000-0000-0000-000000000002', 'a1000001-0000-0000-0000-000000000008', 'ELECTRICIDAD', '2023-06-20', 'Consumo eléctrico Q2 2023', 520.000, 0.520, 'electricity-supply_grid-source_residual_mix', '2023-06-20 12:00:00+00', '2023-06-20 12:00:00+00', NULL, 3700.000, 'KWH'),
('a8000001-0000-0000-0000-000000000003', 'a1000001-0000-0000-0000-000000000008', 'ELECTRICIDAD', '2023-09-10', 'Consumo eléctrico Q3 2023', 480.000, 0.480, 'electricity-supply_grid-source_residual_mix', '2023-09-10 12:00:00+00', '2023-09-10 12:00:00+00', NULL, 3400.000, 'KWH'),
('a8000001-0000-0000-0000-000000000004', 'a1000001-0000-0000-0000-000000000008', 'ELECTRICIDAD', '2023-12-05', 'Consumo eléctrico Q4 2023', 410.000, 0.410, 'electricity-supply_grid-source_residual_mix', '2023-12-05 12:00:00+00', '2023-12-05 12:00:00+00', NULL, 2900.000, 'KWH'),
-- 2024 (reducción progresiva)
('a8000001-0000-0000-0000-000000000005', 'a1000001-0000-0000-0000-000000000008', 'ELECTRICIDAD', '2024-03-12', 'Consumo eléctrico Q1 2024', 380.000, 0.380, 'electricity-supply_grid-source_residual_mix', '2024-03-12 12:00:00+00', '2024-03-12 12:00:00+00', NULL, 2700.000, 'KWH'),
('a8000001-0000-0000-0000-000000000006', 'a1000001-0000-0000-0000-000000000008', 'ELECTRICIDAD', '2024-06-18', 'Consumo eléctrico Q2 2024', 420.000, 0.420, 'electricity-supply_grid-source_residual_mix', '2024-06-18 12:00:00+00', '2024-06-18 12:00:00+00', NULL, 3000.000, 'KWH'),
('a8000001-0000-0000-0000-000000000007', 'a1000001-0000-0000-0000-000000000008', 'ELECTRICIDAD', '2024-09-22', 'Consumo eléctrico Q3 2024', 350.000, 0.350, 'electricity-supply_grid-source_residual_mix', '2024-09-22 12:00:00+00', '2024-09-22 12:00:00+00', NULL, 2500.000, 'KWH'),
('a8000001-0000-0000-0000-000000000008', 'a1000001-0000-0000-0000-000000000008', 'ELECTRICIDAD', '2024-12-10', 'Consumo eléctrico Q4 2024', 320.000, 0.320, 'electricity-supply_grid-source_residual_mix', '2024-12-10 12:00:00+00', '2024-12-10 12:00:00+00', NULL, 2300.000, 'KWH'),
-- 2025 (sigue bajando)
('a8000001-0000-0000-0000-000000000009', 'a1000001-0000-0000-0000-000000000008', 'ELECTRICIDAD', '2025-03-08', 'Consumo eléctrico Q1 2025', 290.000, 0.290, 'electricity-supply_grid-source_residual_mix', '2025-03-08 12:00:00+00', '2025-03-08 12:00:00+00', NULL, 2050.000, 'KWH'),
('a8000001-0000-0000-0000-000000000010', 'a1000001-0000-0000-0000-000000000008', 'ELECTRICIDAD', '2025-06-15', 'Consumo eléctrico Q2 2025', 310.000, 0.310, 'electricity-supply_grid-source_residual_mix', '2025-06-15 12:00:00+00', '2025-06-15 12:00:00+00', NULL, 2200.000, 'KWH')
ON CONFLICT (id) DO NOTHING;

-- ============ FIN DEL SEED ============
-- Verificación:
-- 1. GET /api/perfil-publico/cahuita → perfil con 3 certs vigentes, 1 insignia
-- 2. GET /api/perfil-publico/cahuita/certificaciones → 3 certificaciones ACTIVA
-- 3. GET /api/perfil-publico/cahuita/insignias → 1 insignia bronce
-- 4. GET /api/perfil-publico/cahuita/compartir → enlace, QR, sello, OG
-- 5. GET /api/perfil-publico/parque-cahuita → 301 redirect a /cahuita
