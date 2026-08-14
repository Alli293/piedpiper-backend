package com.piedpiper.carbonhub.auditoria.models.enums;

public enum EstadoSolicitudAuditoria {

    SOLICITUD_ENVIADA("Solicitud enviada"),
    AUDITOR_ASIGNADO("Auditor asignado"),
    EN_REVISION("En revisión"),
    REPORTE_CARGADO("Reporte cargado"),
    OBSERVACIONES_PENDIENTES("Observaciones pendientes"),
    CERTIFICACION_EMITIDA("Certificación emitida");

    private final String descripcion;

    EstadoSolicitudAuditoria(String descripcion) {
        this.descripcion = descripcion;
    }

    /**
     * Texto para mostrar a una persona. Vive en el enum y no en cada consumidor para que el correo
     * de cambio de estado y la respuesta de la API nombren el mismo estado de la misma forma.
     */
    public String getDescripcion() {
        return descripcion;
    }
}
