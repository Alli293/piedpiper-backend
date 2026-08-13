package com.piedpiper.carbonhub.notification;

public final class EnlaceTokenCorreo {

    private EnlaceTokenCorreo() {
    }

    /**
     * Coloca el token en el fragmento para que no viaje al servidor web al abrir el enlace.
     * El frontend lo retira de la barra de direcciones antes de intercambiarlo con la API.
     */
    public static String construir(String urlBase, String token) {
        return "%s#token=%s".formatted(urlBase, token);
    }
}
