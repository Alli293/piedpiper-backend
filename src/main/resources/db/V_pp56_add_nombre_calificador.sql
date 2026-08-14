-- NOTA: Script de referencia. El proyecto usa ddl-auto=update; este archivo
-- documenta el esquema esperado pero no se ejecuta automáticamente.

-- PP-56: Agregar nombre del calificador a calificaciones_auditoria
ALTER TABLE calificaciones_auditoria ADD COLUMN IF NOT EXISTS nombre_calificador VARCHAR(200);
