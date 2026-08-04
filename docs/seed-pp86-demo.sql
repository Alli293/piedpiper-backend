-- =============================================================================
-- SEED DATA para demo PP-86: Priorización Ambiental
-- Inserta empresas turísticas de Costa Rica con indicadores ambientales
-- para que el matching por nombre funcione con las recomendaciones de Gemini.
-- =============================================================================

-- Variables reutilizables
-- Auditor existente para certificaciones:
-- da69b327-e866-4960-a55f-1431abd1f958 (Alison, ADMINISTRADOR_EMPRESA)

-- ============ EMPRESAS TURÍSTICAS ============

INSERT INTO empresas (id, nombre_empresa, cedula_juridica, sector_industrial, pais, cantidad_empleados, correo_corporativo, sitio_web, logo_url, slug, descripcion, nivel_ecologico, estado, fecha_registro)
VALUES
('a1000001-0000-0000-0000-000000000001', 'Manuel Antonio', '3-101-900001', 'HOTELERIA', 'Costa Rica', 45, 'info@manuelantonio.cr', 'https://manuelantonio.cr', NULL, 'manuel-antonio', 'Parque Nacional Manuel Antonio - turismo sostenible', 'ORO', 'ACTIVO', NOW()),
('a1000001-0000-0000-0000-000000000002', 'Selvatura Park', '3-101-900002', 'SERVICIOS', 'Costa Rica', 80, 'info@selvatura.cr', 'https://selvatura.cr', NULL, 'selvatura-park', 'Reserva de aventura y canopy en Monteverde', 'PLATA', 'ACTIVO', NOW()),
('a1000001-0000-0000-0000-000000000003', 'Tabacon', '3-101-900003', 'HOTELERIA', 'Costa Rica', 200, 'info@tabacon.cr', 'https://tabacon.cr', NULL, 'tabacon', 'Aguas termales y resort en Arenal', 'ORO', 'ACTIVO', NOW()),
('a1000001-0000-0000-0000-000000000004', 'Monteverde', '3-101-900004', 'SERVICIOS', 'Costa Rica', 30, 'info@monteverde.cr', 'https://monteverde.cr', NULL, 'monteverde', 'Reserva Biológica Bosque Nuboso Monteverde', 'ORO', 'ACTIVO', NOW()),
('a1000001-0000-0000-0000-000000000005', 'Arenal', '3-101-900005', 'HOTELERIA', 'Costa Rica', 60, 'info@arenal.cr', 'https://arenal.cr', NULL, 'arenal', 'Volcán Arenal - turismo de naturaleza', 'PLATA', 'ACTIVO', NOW()),
('a1000001-0000-0000-0000-000000000006', 'Tortuguero', '3-101-900006', 'SERVICIOS', 'Costa Rica', 25, 'info@tortuguero.cr', 'https://tortuguero.cr', NULL, 'tortuguero', 'Parque Nacional Tortuguero', 'ORO', 'ACTIVO', NOW()),
('a1000001-0000-0000-0000-000000000007', 'Corcovado', '3-101-900007', 'SERVICIOS', 'Costa Rica', 20, 'info@corcovado.cr', 'https://corcovado.cr', NULL, 'corcovado', 'Parque Nacional Corcovado, Osa', 'PLATA', 'ACTIVO', NOW()),
('a1000001-0000-0000-0000-000000000008', 'Cahuita', '3-101-900008', 'SERVICIOS', 'Costa Rica', 15, 'info@cahuita.cr', 'https://cahuita.cr', NULL, 'cahuita', 'Parque Nacional Cahuita', 'BRONCE', 'ACTIVO', NOW())
ON CONFLICT (cedula_juridica) DO NOTHING;


-- ============ CERTIFICACIONES ACTIVAS ============
-- Cada empresa tiene entre 1-3 certificaciones vigentes

INSERT INTO certificaciones (id, id_auditoria, empresa_id, auditor_id, tipo, fecha_emision, fecha_vencimiento, estado, credencial_jwt, indice_estado)
VALUES
-- Manuel Antonio: 3 certificaciones (score alto)
('b2000001-0000-0000-0000-000000000001', 'c3000001-0000-0000-0000-000000000001', 'a1000001-0000-0000-0000-000000000001', 'da69b327-e866-4960-a55f-1431abd1f958', 'CARBONO_NEUTRAL', '2025-06-01 00:00:00+00', '2027-06-01', 'ACTIVA', 'eyJ0eXAiOiJKV1QiLCJhbGciOiJFZERTQSJ9.seed-demo-001', 200),
('b2000001-0000-0000-0000-000000000002', 'c3000001-0000-0000-0000-000000000002', 'a1000001-0000-0000-0000-000000000001', 'da69b327-e866-4960-a55f-1431abd1f958', 'INVENTARIO_GEI', '2025-03-01 00:00:00+00', '2027-03-01', 'ACTIVA', 'eyJ0eXAiOiJKV1QiLCJhbGciOiJFZERTQSJ9.seed-demo-002', 201),
('b2000001-0000-0000-0000-000000000003', 'c3000001-0000-0000-0000-000000000003', 'a1000001-0000-0000-0000-000000000001', 'da69b327-e866-4960-a55f-1431abd1f958', 'REDUCCION_EMISIONES', '2025-09-01 00:00:00+00', '2027-09-01', 'ACTIVA', 'eyJ0eXAiOiJKV1QiLCJhbGciOiJFZERTQSJ9.seed-demo-003', 202),
-- Selvatura Park: 2 certificaciones
('b2000001-0000-0000-0000-000000000004', 'c3000001-0000-0000-0000-000000000004', 'a1000001-0000-0000-0000-000000000002', 'da69b327-e866-4960-a55f-1431abd1f958', 'INVENTARIO_GEI', '2025-04-15 00:00:00+00', '2027-04-15', 'ACTIVA', 'eyJ0eXAiOiJKV1QiLCJhbGciOiJFZERTQSJ9.seed-demo-004', 203),
('b2000001-0000-0000-0000-000000000005', 'c3000001-0000-0000-0000-000000000005', 'a1000001-0000-0000-0000-000000000002', 'da69b327-e866-4960-a55f-1431abd1f958', 'REDUCCION_EMISIONES', '2025-07-01 00:00:00+00', '2027-07-01', 'ACTIVA', 'eyJ0eXAiOiJKV1QiLCJhbGciOiJFZERTQSJ9.seed-demo-005', 204),
-- Tabacon: 3 certificaciones (score alto)
('b2000001-0000-0000-0000-000000000006', 'c3000001-0000-0000-0000-000000000006', 'a1000001-0000-0000-0000-000000000003', 'da69b327-e866-4960-a55f-1431abd1f958', 'CARBONO_NEUTRAL_PLUS', '2025-01-15 00:00:00+00', '2027-01-15', 'ACTIVA', 'eyJ0eXAiOiJKV1QiLCJhbGciOiJFZERTQSJ9.seed-demo-006', 205),
('b2000001-0000-0000-0000-000000000007', 'c3000001-0000-0000-0000-000000000007', 'a1000001-0000-0000-0000-000000000003', 'da69b327-e866-4960-a55f-1431abd1f958', 'INVENTARIO_GEI', '2025-05-01 00:00:00+00', '2027-05-01', 'ACTIVA', 'eyJ0eXAiOiJKV1QiLCJhbGciOiJFZERTQSJ9.seed-demo-007', 206),
('b2000001-0000-0000-0000-000000000008', 'c3000001-0000-0000-0000-000000000008', 'a1000001-0000-0000-0000-000000000003', 'da69b327-e866-4960-a55f-1431abd1f958', 'REDUCCION_EMISIONES', '2025-08-01 00:00:00+00', '2027-08-01', 'ACTIVA', 'eyJ0eXAiOiJKV1QiLCJhbGciOiJFZERTQSJ9.seed-demo-008', 207),
-- Monteverde: 2 certificaciones
('b2000001-0000-0000-0000-000000000009', 'c3000001-0000-0000-0000-000000000009', 'a1000001-0000-0000-0000-000000000004', 'da69b327-e866-4960-a55f-1431abd1f958', 'CARBONO_NEUTRAL', '2025-02-01 00:00:00+00', '2027-02-01', 'ACTIVA', 'eyJ0eXAiOiJKV1QiLCJhbGciOiJFZERTQSJ9.seed-demo-009', 208),
('b2000001-0000-0000-0000-000000000010', 'c3000001-0000-0000-0000-000000000010', 'a1000001-0000-0000-0000-000000000004', 'da69b327-e866-4960-a55f-1431abd1f958', 'ADAPTACION_CLIMATICA', '2025-06-15 00:00:00+00', '2027-06-15', 'ACTIVA', 'eyJ0eXAiOiJKV1QiLCJhbGciOiJFZERTQSJ9.seed-demo-010', 209),
-- Arenal: 1 certificacion
('b2000001-0000-0000-0000-000000000011', 'c3000001-0000-0000-0000-000000000011', 'a1000001-0000-0000-0000-000000000005', 'da69b327-e866-4960-a55f-1431abd1f958', 'INVENTARIO_GEI', '2025-10-01 00:00:00+00', '2027-10-01', 'ACTIVA', 'eyJ0eXAiOiJKV1QiLCJhbGciOiJFZERTQSJ9.seed-demo-011', 210),
-- Tortuguero: 2 certificaciones
('b2000001-0000-0000-0000-000000000012', 'c3000001-0000-0000-0000-000000000012', 'a1000001-0000-0000-0000-000000000006', 'da69b327-e866-4960-a55f-1431abd1f958', 'CARBONO_NEUTRAL', '2025-03-15 00:00:00+00', '2027-03-15', 'ACTIVA', 'eyJ0eXAiOiJKV1QiLCJhbGciOiJFZERTQSJ9.seed-demo-012', 211),
('b2000001-0000-0000-0000-000000000013', 'c3000001-0000-0000-0000-000000000013', 'a1000001-0000-0000-0000-000000000006', 'da69b327-e866-4960-a55f-1431abd1f958', 'REDUCCION_PLUS', '2025-07-15 00:00:00+00', '2027-07-15', 'ACTIVA', 'eyJ0eXAiOiJKV1QiLCJhbGciOiJFZERTQSJ9.seed-demo-013', 212),
-- Corcovado: 1 certificacion
('b2000001-0000-0000-0000-000000000014', 'c3000001-0000-0000-0000-000000000014', 'a1000001-0000-0000-0000-000000000007', 'da69b327-e866-4960-a55f-1431abd1f958', 'INVENTARIO_GEI', '2025-05-15 00:00:00+00', '2027-05-15', 'ACTIVA', 'eyJ0eXAiOiJKV1QiLCJhbGciOiJFZERTQSJ9.seed-demo-014', 213),
-- Cahuita: 1 certificacion
('b2000001-0000-0000-0000-000000000015', 'c3000001-0000-0000-0000-000000000015', 'a1000001-0000-0000-0000-000000000008', 'da69b327-e866-4960-a55f-1431abd1f958', 'REDUCCION_EMISIONES', '2025-04-01 00:00:00+00', '2027-04-01', 'ACTIVA', 'eyJ0eXAiOiJKV1QiLCJhbGciOiJFZERTQSJ9.seed-demo-015', 214)
ON CONFLICT (id_auditoria) DO NOTHING;


-- ============ IMA SNAPSHOTS (mes actual y reciente) ============
-- Cada empresa con un IMA reciente (dentro de 3 meses) para que ImaClientImpl lo encuentre

INSERT INTO ima_snapshots (id, empresa_id, anio, mes, cobertura, puntaje_intensidad_sectorial, consistencia, ima, parcial, motivo_parcial, intensidad, calculated_at)
VALUES
-- Manuel Antonio: IMA 85 (excelente)
('d4000001-0000-0000-0000-000000000001', 'a1000001-0000-0000-0000-000000000001', 2025, 7, 95.0, 80.0, 90.0, 85.0, false, NULL, 0.000120, NOW()),
-- Selvatura Park: IMA 72
('d4000001-0000-0000-0000-000000000002', 'a1000001-0000-0000-0000-000000000002', 2025, 7, 85.0, 70.0, 75.0, 72.0, false, NULL, 0.000250, NOW()),
-- Tabacon: IMA 90 (líder)
('d4000001-0000-0000-0000-000000000003', 'a1000001-0000-0000-0000-000000000003', 2025, 7, 98.0, 88.0, 92.0, 90.0, false, NULL, 0.000085, NOW()),
-- Monteverde: IMA 78
('d4000001-0000-0000-0000-000000000004', 'a1000001-0000-0000-0000-000000000004', 2025, 7, 88.0, 75.0, 80.0, 78.0, false, NULL, 0.000180, NOW()),
-- Arenal: IMA 60
('d4000001-0000-0000-0000-000000000005', 'a1000001-0000-0000-0000-000000000005', 2025, 7, 75.0, 55.0, 65.0, 60.0, false, NULL, 0.000350, NOW()),
-- Tortuguero: IMA 82
('d4000001-0000-0000-0000-000000000006', 'a1000001-0000-0000-0000-000000000006', 2025, 7, 92.0, 78.0, 85.0, 82.0, false, NULL, 0.000140, NOW()),
-- Corcovado: IMA 68
('d4000001-0000-0000-0000-000000000007', 'a1000001-0000-0000-0000-000000000007', 2025, 7, 80.0, 65.0, 70.0, 68.0, false, NULL, 0.000280, NOW()),
-- Cahuita: IMA 55
('d4000001-0000-0000-0000-000000000008', 'a1000001-0000-0000-0000-000000000008', 2025, 7, 70.0, 50.0, 55.0, 55.0, false, NULL, 0.000420, NOW())
ON CONFLICT ON CONSTRAINT ukgcnaablb7iduc0hi8h478d52f DO NOTHING;


-- ============ AGREGADOS SECTORIALES (para benchmark) ============
-- Promedios sectoriales del mes actual para HOTELERIA y SERVICIOS

INSERT INTO agregados_sectoriales (id, sector, anio, mes, cantidad_empresas, promedio_cobertura, promedio_puntaje_intensidad_sectorial, promedio_consistencia, promedio_ima, intensidad_promedio, calculated_at)
VALUES
('e5000001-0000-0000-0000-000000000001', 'HOTELERIA', 2025, 7, 12, 78.0, 65.0, 72.0, 65.0, 0.000300, NOW()),
('e5000001-0000-0000-0000-000000000002', 'SERVICIOS', 2025, 7, 18, 75.0, 60.0, 68.0, 62.0, 0.000350, NOW())
ON CONFLICT ON CONSTRAINT ukqj5ru0mlr1d26phsruta75ga DO NOTHING;

-- ============ FIN DEL SEED ============
-- Después de insertar, generar un itinerario desde el frontend.
-- Las actividades que Gemini recomiende con nombres que contengan
-- "Manuel Antonio", "Selvatura", "Tabacon", "Monteverde", "Arenal",
-- "Tortuguero", "Corcovado" o "Cahuita" harán match y mostrarán
-- su EcoScore ambiental en el frontend.
