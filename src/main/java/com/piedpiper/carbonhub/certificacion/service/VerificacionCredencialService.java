package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.models.dtos.VerificacionCredencialDTO;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.insignia.service.InsigniaEmpresaConsultaService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Punto unico de entrada de {@code VerificacionPublicaController} (PP-68,
 * extendido a insignias): un mismo codigo corto puede pertenecer a una
 * certificacion o a una insignia, y el codigo por si solo no lo distingue.
 * Intenta primero como certificacion (el caso mas comun) y, solo si esa
 * consulta no encuentra nada, intenta como insignia. Cualquier otro tipo de
 * error de la consulta de certificacion se propaga tal cual.
 *
 * <p>Deliberadamente sin {@code @Transactional}: {@code verificarPorCodigo}
 * en cada consulta ya abre su propia transaccion de lectura. Si esta clase
 * tambien fuera transaccional, ambas llamadas compartirian esa misma
 * transaccion (propagacion {@code REQUIRED}) y la de certificacion, al
 * lanzar {@code ApiException} para el caso "no encontrada", la marcaria
 * {@code rollback-only} -- el catch de aqui abajo atrapa la excepcion, pero
 * no revierte esa marca. La consulta de insignia entonces si encontraria el
 * resultado, pero al intentar hacer commit de la transaccion ya marcada,
 * Spring lanza {@code UnexpectedRollbackException} (un 500, no el 404/200
 * esperado). Sin transaccion propia aca, cada consulta corre en su propia
 * transaccion independiente y esto no ocurre.
 */
@Service
public class VerificacionCredencialService {

    private final ConsultaCertificacionService consultaCertificacionService;
    private final InsigniaEmpresaConsultaService insigniaEmpresaConsultaService;

    public VerificacionCredencialService(ConsultaCertificacionService consultaCertificacionService,
                                         InsigniaEmpresaConsultaService insigniaEmpresaConsultaService) {
        this.consultaCertificacionService = consultaCertificacionService;
        this.insigniaEmpresaConsultaService = insigniaEmpresaConsultaService;
    }

    public VerificacionCredencialDTO verificar(String codigo) {
        try {
            return consultaCertificacionService.verificarPorCodigo(codigo);
        } catch (ApiException noEncontradaComoCertificacion) {
            if (noEncontradaComoCertificacion.getStatus() != HttpStatus.NOT_FOUND) {
                throw noEncontradaComoCertificacion;
            }
            return insigniaEmpresaConsultaService.verificarPorCodigo(codigo);
        }
    }
}
