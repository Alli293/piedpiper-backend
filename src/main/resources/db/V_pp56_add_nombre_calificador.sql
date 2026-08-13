-- PP-56: Agregar nombre del calificador a calificaciones_auditoria
ALTER TABLE calificaciones_auditoria ADD COLUMN IF NOT EXISTS nombre_calificador VARCHAR(200);
