-- NOTA: Script de referencia. El proyecto usa ddl-auto=update; este archivo
-- documenta el esquema esperado pero no se ejecuta automáticamente.

-- PP-56: Crear tabla calificaciones_auditoria
-- Almacena las calificaciones verificadas que empresas otorgan a auditores
-- tras la emisión de certificación de una auditoría.

CREATE TABLE IF NOT EXISTS calificaciones_auditoria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    auditoria_id UUID NOT NULL REFERENCES solicitudes_auditoria(id),
    auditor_id UUID NOT NULL REFERENCES usuarios(id),
    empresa_id UUID NOT NULL REFERENCES empresas(id),
    calificacion INTEGER NOT NULL,
    comentario VARCHAR(500),
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT chk_calificacion_rango CHECK (calificacion >= 1 AND calificacion <= 5),
    CONSTRAINT uk_calificaciones_auditoria_empresa UNIQUE (auditoria_id, empresa_id)
);
