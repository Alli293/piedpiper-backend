package com.piedpiper.carbonhub.exceptions;

import org.springframework.http.HttpStatus;

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
        return new ApiException(HttpStatus.NOT_FOUND,
                "El enlace de verificación no es válido.");
    }

    public static ApiException tokenVerificacionExpirado() {
        return new ApiException(HttpStatus.GONE,
                "El enlace de verificación expiró. Solicita uno nuevo.");
    }

    public static ApiException accesoDenegado(String mensaje) {
        return new ApiException(HttpStatus.FORBIDDEN, mensaje);
    }

    public static ApiException valorNoSoportado(String mensaje) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, mensaje);
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

    public static ApiException anioFueraDeRango(int anioMaximo) {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "El año debe estar entre 2000 y " + anioMaximo + ".");
    }

    public static ApiException mesInvalido() {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "El mes debe estar entre 1 y 12.");
    }

    public static ApiException periodoFuturo() {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "El período no puede ser futuro.");
    }

    public static ApiException combinacionVehiculoInvalida() {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "Seleccione un combustible válido para este tipo de vehículo.");
    }

    public static ApiException metodoTransporteNoSoportado() {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "Seleccione un método de transporte válido.");
    }

    public static ApiException limiteConflicto() {
        return new ApiException(HttpStatus.CONFLICT,
                "Conflicto al guardar el límite. Intente nuevamente.");
    }
}
