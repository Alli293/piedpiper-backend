package com.piedpiper.carbonhub.notification;

public final class EmailPlantillaHtml {

    private static final String COLOR_BOTON_DEFECTO = "#1f8a5b";

    private static final String CABECERA = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>%s</title>
            </head>
            <body style="margin:0; padding:0; background-color:#f0f2f5; font-family:Arial, Helvetica, sans-serif;">
              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f0f2f5; padding:32px 16px;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px; width:100%%; background-color:#ffffff; border-radius:12px; overflow:hidden; border:1px solid #e2e8f0;">
                      <tr>
                        <td style="padding:32px 40px 8px 40px;" align="left">
                          <span style="font-size:22px; font-weight:700; color:#0e2a3b;">Carbon</span><span style="font-size:22px; font-weight:700; color:#1f8a5b;">Hub</span>
                        </td>
                      </tr>
            """;

    private static final String PIE = """
                      <tr>
                        <td style="padding:24px 40px; border-top:1px solid #e2e8f0;" align="center">
                          <p style="margin:0; font-size:12px; color:#8a9bae;">CarbonHub — Costa Rica</p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """;

    private static final String BOTON = """
                      <tr>
                        <td style="padding:0 40px 24px 40px;" align="center">
                          <table role="presentation" cellpadding="0" cellspacing="0">
                            <tr>
                              <td style="border-radius:8px; background-color:%s;">
                                <a href="%s" target="_blank" style="display:inline-block; padding:14px 32px; font-size:15px; font-weight:600; color:#ffffff; text-decoration:none; border-radius:8px;">
                                  %s
                                </a>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
            """;

    private static final String CUERPO_CON_BOTON_Y_AVISO = """
                      <tr>
                        <td style="padding:24px 40px 0 40px;">
                          <p style="margin:0 0 16px 0; font-size:16px; color:#0e2a3b;">%s</p>
                          <p style="margin:0 0 24px 0; font-size:15px; line-height:1.6; color:#334155;">
                            %s
                          </p>
                        </td>
                      </tr>
                      %s
                      <tr>
                        <td style="padding:0 40px 8px 40px;">
                          <p style="margin:0 0 8px 0; font-size:13px; color:#64748b;">
                            %s
                          </p>
                          <p style="margin:0; font-size:13px; color:#64748b;">
                            Si el botón no funciona, copia y pega este enlace en tu navegador:<br>
                            <a href="%s" style="color:#2ba6de; word-break:break-all;">%s</a>
                          </p>
                        </td>
                      </tr>
            """;

    private EmailPlantillaHtml() {
    }

    public static String documento(String titulo, String cuerpoHtml) {
        return CABECERA.formatted(titulo) + cuerpoHtml + PIE;
    }

    public static String boton(String url, String texto) {
        return boton(url, texto, COLOR_BOTON_DEFECTO);
    }

    public static String boton(String url, String texto, String color) {
        return BOTON.formatted(color, url, texto);
    }

    public static String cuerpoConBotonYAviso(String saludo, String introHtml, String botonHtml,
                                               String avisoHtml, String enlace) {
        return CUERPO_CON_BOTON_Y_AVISO.formatted(saludo, introHtml, botonHtml, avisoHtml, enlace, enlace);
    }
}
