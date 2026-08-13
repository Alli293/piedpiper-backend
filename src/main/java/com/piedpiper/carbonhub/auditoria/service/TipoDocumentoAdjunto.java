package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.exceptions.ApiException;

public enum TipoDocumentoAdjunto {

    RESPALDO {
        @Override
        ApiException documentosRequeridos() {
            return ApiException.documentosRespaldoRequeridos();
        }

        @Override
        ApiException documentosExcedenMaximo() {
            return ApiException.documentosRespaldoExcedenMaximo();
        }

        @Override
        ApiException documentoExcedeTamanio() {
            return ApiException.documentoRespaldoExcedeTamanio();
        }

        @Override
        ApiException documentoNoEsPdf() {
            return ApiException.documentoRespaldoNoEsPdf();
        }
    },

    CREDENCIAL_AUDITOR {
        @Override
        ApiException documentosRequeridos() {
            return ApiException.documentosCredencialesRequeridos();
        }

        @Override
        ApiException documentosExcedenMaximo() {
            return ApiException.documentosCredencialesExcedenMaximo();
        }

        @Override
        ApiException documentoExcedeTamanio() {
            return ApiException.documentoCredencialExcedeTamanio();
        }

        @Override
        ApiException documentoNoEsPdf() {
            return ApiException.documentoCredencialNoEsPdf();
        }
    };

    abstract ApiException documentosRequeridos();

    abstract ApiException documentosExcedenMaximo();

    abstract ApiException documentoExcedeTamanio();

    abstract ApiException documentoNoEsPdf();
}
