-- Flujo completo de validación de auditores: configuración inicial de perfil,
-- consulta de la propia solicitud, y aprobación/rechazo con documentos de credenciales.
-- El campo sitio_web es nullable para compatibilidad con perfiles existentes.

ALTER TABLE perfiles_auditor ADD COLUMN sitio_web VARCHAR(300);

CREATE TABLE documentos_credenciales_auditor (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    solicitud_id UUID NOT NULL REFERENCES solicitudes_validacion(id),
    nombre_archivo VARCHAR(255) NOT NULL,
    tipo_contenido VARCHAR(100) NOT NULL,
    tamanio_bytes BIGINT NOT NULL,
    contenido BYTEA NOT NULL,
    fecha_carga TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_documentos_credenciales_auditor_solicitud ON documentos_credenciales_auditor(solicitud_id);
