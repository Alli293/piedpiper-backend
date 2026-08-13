package com.piedpiper.carbonhub.exceptions;

import org.springframework.http.HttpStatus;

import java.util.List;

public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ApiException tokenInvalido() {
        return new ApiException(HttpStatus.UNAUTHORIZED,
                "No fue posible verificar tu cuenta de Google. Por favor, intenta nuevamente.");
    }

    public static ApiException correoNoVerificado() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Tu correo de Google no está verificado. Verifica tu cuenta de Google antes de continuar.");
    }

    public static ApiException cuentaDuplicada(String mensaje) {
        return new ApiException(HttpStatus.CONFLICT, mensaje);
    }

    public static ApiException googleTimeout() {
        return new ApiException(HttpStatus.GATEWAY_TIMEOUT,
                "El servicio de autenticación no está disponible en este momento. Intenta más tarde.");
    }

    public static ApiException errorInterno(String mensaje) {
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, mensaje);
    }

    public static ApiException tokenVerificacionInvalido() {
        return new ApiException(HttpStatus.GONE,
                "Este enlace de verificación no es válido o expiró. Solicita uno nuevo.");
    }

    public static ApiException tokenVerificacionMalFormado() {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "El formato del enlace de verificación no es válido.");
    }

    public static ApiException correoYaVerificado() {
        return new ApiException(HttpStatus.CONFLICT,
                "Tu correo ya fue verificado. Inicia sesión.");
    }

    public static ApiException reenviosVerificacionExcedidos() {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                "Has solicitado demasiados reenvíos. Intenta de nuevo en una hora.");
    }

    public static ApiException tokenResetInvalido() {
        return new ApiException(HttpStatus.GONE,
                "Este enlace no es válido o expiró. Solicita uno nuevo.");
    }

    public static ApiException tokenResetMalFormado() {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "El formato del enlace no es válido.");
    }

    public static ApiException cuentaNoDisponible() {
        return new ApiException(HttpStatus.CONFLICT,
                "Esta cuenta no está disponible para verificación. Contacta a soporte.");
    }

    public static ApiException accesoDenegado(String mensaje) {
        return new ApiException(HttpStatus.FORBIDDEN, mensaje);
    }

    public static ApiException valorNoSoportado(String mensaje) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, mensaje);
    }

    public static ApiException anioInvalido() {
        return new ApiException(HttpStatus.BAD_REQUEST, "Año inválido.");
    }

    public static ApiException recursoNoEncontrado(String mensaje) {
        return new ApiException(HttpStatus.NOT_FOUND, mensaje);
    }

    public static ApiException calculoInvalido(String mensajeServicio) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "No se pudo calcular la huella: " + mensajeServicio + ". Verifique los datos ingresados.");
    }

    public static ApiException calculoConfiguracion() {
        return new ApiException(HttpStatus.BAD_GATEWAY,
                "Error de configuración del servicio de cálculo. Contacte al administrador.");
    }

    public static ApiException calculoSaturado() {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                "El servicio de cálculo está temporalmente saturado. Intente de nuevo en unos minutos.");
    }

    public static ApiException calculoNoDisponible() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo conectar con el servicio de cálculo de huella. Intente nuevamente más tarde.");
    }

    public static ApiException calculoRespuestaInvalida() {
        return new ApiException(HttpStatus.BAD_GATEWAY,
                "El servicio de cálculo devolvió una respuesta incompleta. Intente nuevamente más tarde.");
    }

    public static ApiException calculoUnidadNoSoportada(String unidad) {
        return new ApiException(HttpStatus.BAD_GATEWAY,
                "El servicio de cálculo devolvió una unidad no soportada (" + unidad + ").");
    }

    public static ApiException calculoVueloInvalido(String mensaje) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, mensaje);
    }

    public static ApiException empresaNoConfigurada() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Debes completar la configuración de tu empresa antes de realizar esta acción.");
    }

    public static ApiException invitacionCorreoYaEnEmpresa() {
        return new ApiException(HttpStatus.CONFLICT,
                "Esta persona ya forma parte de tu empresa.");
    }

    public static ApiException invitacionPendiente() {
        return new ApiException(HttpStatus.CONFLICT,
                "Ya existe una invitación pendiente para este correo.");
    }

    public static ApiException invitacionSinEmpresa() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Debes completar la configuración de tu empresa antes de invitar colaboradores.");
    }

    public static ApiException invitacionNoEncontrada() {
        return new ApiException(HttpStatus.NOT_FOUND,
                "La invitación no existe.");
    }

    public static ApiException invitacionNoRevocable() {
        return new ApiException(HttpStatus.CONFLICT,
                "Solo se pueden revocar invitaciones pendientes.");
    }

    public static ApiException invitacionInvalida() {
        return new ApiException(HttpStatus.NOT_FOUND,
                "Este enlace de invitación no es válido.");
    }

    public static ApiException invitacionNoDisponible() {
        return new ApiException(HttpStatus.CONFLICT,
                "Esta invitación ya no está disponible. Solicita una nueva al administrador de tu empresa.");
    }

    public static ApiException invitacionExpirada() {
        return new ApiException(HttpStatus.GONE,
                "Esta invitación ha expirado. Solicita una nueva al administrador de tu empresa.");
    }

    public static ApiException invitacionYaUtilizada() {
        return new ApiException(HttpStatus.CONFLICT,
                "Esta invitación ya fue utilizada.");
    }

    public static ApiException solicitudNoEncontrada() {
        return new ApiException(HttpStatus.NOT_FOUND,
                "Esta solicitud no fue encontrada.");
    }

    public static ApiException solicitudYaProcesada() {
        return new ApiException(HttpStatus.CONFLICT,
                "Esta solicitud ya fue procesada por otro administrador.");
    }

    public static ApiException solicitudConflictoConcurrente() {
        return new ApiException(HttpStatus.CONFLICT,
                "Esta solicitud ya fue procesada. Recarga la página para ver el estado actualizado.");
    }

    public static ApiException invitacionCorreoNoCoincide() {
        return new ApiException(HttpStatus.FORBIDDEN,
                "La cuenta de Google que seleccionaste no corresponde al correo de esta invitación. "
                        + "Inicia sesión con la cuenta indicada.");
    }

    public static ApiException combinacionVehiculoInvalida() {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "Seleccione un combustible válido para este tipo de vehículo.");
    }

    public static ApiException anioConsultaInvalido() {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "El año indicado no es válido.");
    }

    public static ApiException metodoTransporteNoSoportado() {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "Seleccione un método de transporte válido.");
    }

    public static ApiException categoriaEmisionInvalida() {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "Categoría de emisión inválida.");
    }

    public static ApiException ordenamientoAuditoresInvalido() {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "El criterio de ordenamiento no es válido.");
    }

    public static ApiException especialidadAuditorInvalida(String valor) {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "La especialidad '" + valor + "' no es válida.");
    }

    public static ApiException zonaAuditorInvalida(String valor) {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "La zona geográfica '" + valor + "' no es válida.");
    }

    public static ApiException mesInvalido() {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "El mes debe estar entre 1 y 12.");
    }

    public static ApiException limiteConflicto() {
        return new ApiException(HttpStatus.CONFLICT,
                "Conflicto al guardar el límite. Intente nuevamente.");
    }

    public static ApiException perfilNoPropio() {
        return new ApiException(HttpStatus.FORBIDDEN,
                "No tiene permiso para editar este perfil.");
    }

    public static ApiException cuentaNoValidada() {
        return new ApiException(HttpStatus.FORBIDDEN,
                "Tu cuenta debe estar validada para actualizar tu perfil de directorio.");
    }

    public static ApiException especialidadesInvalidas(List<String> invalidas) {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "Las siguientes especialidades no son válidas: " + String.join(", ", invalidas) + ".");
    }

    public static ApiException zonasInvalidas(List<String> invalidas) {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "Las siguientes zonas de cobertura no son válidas: " + String.join(", ", invalidas) + ".");
    }

    public static ApiException preferenciasViajeConflicto() {
        return new ApiException(HttpStatus.CONFLICT,
                "Conflicto al guardar tus preferencias. Intenta nuevamente.");
    }

    public static ApiException itinerarioGeneracionTimeout() {
        return new ApiException(HttpStatus.GATEWAY_TIMEOUT,
                "Ocurrió un error al generar el itinerario. Intenta nuevamente más tarde.");
    }

    public static ApiException itinerarioRespuestaInvalida() {
        return new ApiException(HttpStatus.BAD_GATEWAY,
                "No fue posible generar una propuesta válida. Intenta nuevamente más tarde.");
    }

    public static ApiException itinerarioGeneracionesExcedidas() {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                "Has alcanzado el límite de itinerarios generados. Intenta de nuevo en una hora.");
    }

    /** Rate limit del chat de refinamiento (PP-88) — ver {@code ItinerarioCuotaService.reservarRefinamiento}. */
    public static ApiException itinerarioRefinamientosExcedidos() {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                "Has alcanzado el límite de mensajes de ajuste. Intenta de nuevo en una hora.");
    }

    /**
     * El cliente reenvía {@code versionItinerario} en cada mensaje del chat de refinamiento
     * (PP-88); si no coincide con la versión actual del itinerario en el servidor, alguien más
     * (otra pestaña, otra sesión) ya lo modificó entretanto — 409 en vez de sobrescribir en
     * silencio con un contexto desactualizado.
     */
    public static ApiException itinerarioVersionDesactualizada() {
        return new ApiException(HttpStatus.CONFLICT,
                "Este itinerario cambió en otra sesión. Recargalo para ver los cambios más recientes "
                        + "antes de seguir editando.");
    }

    /**
     * Cubre tanto el timeout de la llamada a Gemini como una respuesta que se agota reintentando
     * durante el refinamiento conversacional (PP-88) — el AC les da la misma redacción a ambos.
     */
    public static ApiException itinerarioRefinamientoFallido() {
        return new ApiException(HttpStatus.GATEWAY_TIMEOUT,
                "No fue posible actualizar el itinerario. Intenta nuevamente.");
    }

    public static ApiException periodoImaInvalido(String mensaje) {
        return new ApiException(HttpStatus.BAD_REQUEST, mensaje);
    }

    public static ApiException datosInvalidos(String mensaje) {
        return new ApiException(HttpStatus.BAD_REQUEST, mensaje);
    }

    public static ApiException periodoAuditoriaFuturo() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "El período a auditar no puede iniciar en una fecha futura.");
    }

    public static ApiException periodoAuditoriaFinInvalido() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "La fecha de fin del período debe ser posterior a la fecha de inicio.");
    }

    public static ApiException periodoAuditoriaExcedeDoceMeses() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "El período a auditar no puede exceder 12 meses.");
    }

    public static ApiException documentosRespaldoRequeridos() {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "Debes adjuntar al menos un documento de respaldo.");
    }

    public static ApiException documentosRespaldoExcedenMaximo() {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "Puedes adjuntar un máximo de 10 documentos.");
    }

    public static ApiException documentoRespaldoExcedeTamanio() {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "El archivo no puede superar 15 MB.");
    }

    public static ApiException documentoRespaldoNoEsPdf() {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "Solo se aceptan archivos en formato PDF.");
    }

    public static ApiException reporteAuditoriaNoEsPdf() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Solo se aceptan archivos en formato PDF.");
    }

    public static ApiException reporteAuditoriaExcedeTamanio() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "El archivo no puede superar 25 MB.");
    }

    public static ApiException reporteAuditoriaNoProcesable() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "El archivo no pudo ser procesado. Verifica que no esté dañado y vuelve a intentarlo.");
    }

    public static ApiException fechaAuditoriaRealizadaInvalida() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "La fecha de la auditoría debe estar entre la fecha de aceptación y la fecha actual.");
    }

    public static ApiException cargaReporteAuditoriaNoDisponible() {
        return new ApiException(HttpStatus.CONFLICT,
                "No es posible cargar el reporte en el estado actual de la solicitud.");
    }

    public static ApiException cargaReporteAuditoriaAjena() {
        return new ApiException(HttpStatus.FORBIDDEN,
                "No tienes permiso para cargar el reporte de esta solicitud de auditoría.");
    }

    public static ApiException solicitudAuditoriaTraslapada() {
        return new ApiException(HttpStatus.CONFLICT,
                "Ya existe una solicitud de auditoría en curso para un período que se traslapa "
                        + "con el seleccionado.");
    }

    public static ApiException solicitudAuditoriaNoEncontrada() {
        return new ApiException(HttpStatus.NOT_FOUND,
                "Esta solicitud de auditoría no fue encontrada.");
    }

    public static ApiException solicitudAuditoriaAjena() {
        return new ApiException(HttpStatus.FORBIDDEN,
                "No tienes permiso para gestionar esta solicitud de auditoría.");
    }

    public static ApiException asignacionAuditorPendiente() {
        return new ApiException(HttpStatus.CONFLICT,
                "Ya existe una solicitud de revisión pendiente con otro auditor.");
    }

    public static ApiException auditorNoDisponible() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Este auditor no está disponible actualmente.");
    }

    public static ApiException origenAsignacionInvalido() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "El origen de la asignación debe ser 'manual' o 'recomendacion_ia'.");
    }

    public static ApiException documentoRespaldoNoEncontrado() {
        return new ApiException(HttpStatus.NOT_FOUND,
                "Este documento de respaldo no fue encontrado.");
    }

    public static ApiException transicionAuditoriaInvalida() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Esta acción no es válida para el estado actual de la solicitud de auditoría.");
    }

    public static ApiException resultadoAuditoriaInvalido() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "El resultado debe ser 'aprobada' u 'observaciones'.");
    }

    public static ApiException resultadoAuditoriaNoDisponible() {
        return new ApiException(HttpStatus.CONFLICT,
                "No es posible emitir el resultado en el estado actual de la solicitud.");
    }

    public static ApiException resultadoAuditoriaAjena() {
        return new ApiException(HttpStatus.FORBIDDEN,
                "No tienes permiso para emitir el resultado de esta solicitud de auditor\u00eda.");
    }

    public static ApiException decisionAuditorInvalida() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "La decisión debe ser 'aceptada' o 'rechazada'.");
    }

    public static ApiException decisionAuditorNoDisponible() {
        return new ApiException(HttpStatus.CONFLICT,
                "Esta solicitud ya no está disponible para tu respuesta.");
    }

    public static ApiException motivoRechazoRequerido() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Indica el motivo del rechazo, entre 10 y 300 caracteres.");
    }

    public static ApiException decisionAuditorAjena() {
        return new ApiException(HttpStatus.FORBIDDEN,
                "No tienes permiso para responder a esta solicitud de auditoría.");
    }

    public static ApiException resultadoAuditoriaNoAprobado() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Solo se emite una certificacion para auditorias con resultado 'aprobada'.");
    }

    public static ApiException fechaVencimientoCertInvalida() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "La fecha de vencimiento de la certificacion debe ser posterior a la fecha de la auditoria.");
    }

    public static ApiException auditorNoValido() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "El usuario indicado no tiene el rol de auditor certificado activo.");
    }

    public static ApiException certificacionNoDisponible() {
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                "No fue posible descargar la certificación. Intenta nuevamente.");
    }

    public static ApiException fechaLimiteMetaInvalida() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "La fecha límite debe ser una fecha futura.");
    }
}
