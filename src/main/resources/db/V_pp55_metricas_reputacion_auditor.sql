-- PP-55: Metricas automaticas de reputacion del auditor.
-- auditorias_completadas permite NULL para representar "Sin datos" en perfiles sin auditorias.

ALTER TABLE perfiles_auditor ALTER COLUMN auditorias_completadas DROP NOT NULL;

ALTER TABLE perfiles_auditor
    ADD COLUMN IF NOT EXISTS tiempo_promedio_respuesta_dias NUMERIC(5, 1);

CREATE TABLE IF NOT EXISTS perfil_auditor_distribucion_sectores (
    perfil_auditor_id UUID NOT NULL,
    orden INTEGER NOT NULL DEFAULT 0,
    sector VARCHAR(50) NOT NULL,
    cantidad INTEGER NOT NULL DEFAULT 0,
    porcentaje NUMERIC(5, 1) NOT NULL,
    PRIMARY KEY (perfil_auditor_id, orden),
    CONSTRAINT fk_perfil_auditor_distribucion_sectores_perfil
        FOREIGN KEY (perfil_auditor_id) REFERENCES perfiles_auditor(id)
);
