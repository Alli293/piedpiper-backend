package com.piedpiper.carbonhub.perfilpublico.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.perfilpublico.models.dtos.EnlacePerfilDTO;

import net.jqwik.api.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based test: Consistencia de URL entre todos los campos del DTO.
 *
 * Para empresas generadas, verifica igualdad entre urlCanonica, ogUrl,
 * URL decodificada del QR y href del fragmento HTML.
 *
 * Validates: Requirements 7.1, 2.5
 */
class EnlacePerfilUrlConsistenciaPropertyTest {

    private static final String BASE_URL = "https://carbonhub.app";
    private static final String OG_IMAGEN_FALLBACK = "https://carbonhub.app/images/og-default.png";
    private static final Pattern HREF_PATTERN = Pattern.compile("href=\"([^\"]+)\"");

    private final QrGeneradorService realQrService = new QrGeneradorService();

    /**
     * Property 2: Consistencia de URL entre todos los campos del DTO
     *
     * For any EnlacePerfilDTO generated for an active empresa, the values of
     * urlCanonica, ogUrl, the decoded QR URL, and the href attribute of the <a>
     * tag inside codigoIncrustar SHALL all be identical.
     *
     * Validates: Requirements 7.1, 2.5
     */
    @Property(tries = 50)
    @Tag("Feature: PP-69-enlace-comparticion-perfil, Property 2: Consistencia de URL entre todos los campos del DTO")
    void urlConsistenteEntreTodosLosCamposDelDto(
            @ForAll("slugValido") String slug,
            @ForAll("nombreEmpresa") String nombreEmpresa,
            @ForAll("nivelEcologico") String nivelEcologico,
            @ForAll("logoUrl") String logoUrl) throws Exception {

        // Arrange: mock slugResolver, use real QrGeneradorService
        SlugResolverService slugResolver = mock(SlugResolverService.class);

        Empresa empresa = Empresa.builder()
                .id(UUID.randomUUID())
                .slug(slug)
                .nombreEmpresa(nombreEmpresa)
                .cedulaJuridica("3101000001")
                .sectorIndustrial(SectorIndustrial.MANUFACTURA)
                .pais("Costa Rica")
                .cantidadEmpleados(50)
                .correoCorporativo("info@test.com")
                .logoUrl(logoUrl)
                .nivelEcologico(nivelEcologico)
                .estado(EstadoEmpresa.ACTIVO)
                .fechaRegistro(Instant.now())
                .build();

        when(slugResolver.resolver(eq(slug))).thenReturn(empresa);

        EnlacePerfilService service = new EnlacePerfilService(
                slugResolver,
                realQrService,
                BASE_URL,
                OG_IMAGEN_FALLBACK
        );

        // Act
        EnlacePerfilDTO dto = service.obtenerEnlacePerfil(slug);

        // Assert: all URL fields must be identical
        String urlCanonica = dto.getUrlCanonica();
        String ogUrl = dto.getOgUrl();

        // 1. urlCanonica == ogUrl
        assertThat(ogUrl)
                .as("ogUrl debe ser idéntico a urlCanonica")
                .isEqualTo(urlCanonica);

        // 2. Decode QR and verify URL matches urlCanonica
        String qrBase64 = dto.getQrBase64();
        assertThat(qrBase64)
                .as("qrBase64 no debe estar vacío para esta prueba")
                .isNotEmpty();

        String qrDecodedUrl = decodeQrBase64(qrBase64);
        assertThat(qrDecodedUrl)
                .as("URL decodificada del QR debe ser idéntica a urlCanonica")
                .isEqualTo(urlCanonica);

        // 3. Extract href from codigoIncrustar HTML fragment
        String codigoIncrustar = dto.getCodigoIncrustar();
        String hrefValue = extractHrefFromHtml(codigoIncrustar);
        assertThat(hrefValue)
                .as("href del enlace en codigoIncrustar debe ser idéntico a urlCanonica")
                .isEqualTo(urlCanonica);
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    /**
     * Decodes a QR Base64 data URI string and returns the text encoded in the QR.
     */
    private String decodeQrBase64(String qrBase64) throws Exception {
        // Remove the data URI prefix
        String base64Data = qrBase64.replace("data:image/png;base64,", "");
        byte[] imageBytes = Base64.getDecoder().decode(base64Data);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        BinaryBitmap bitmap = new BinaryBitmap(
                new HybridBinarizer(new BufferedImageLuminanceSource(image)));

        java.util.Map<com.google.zxing.DecodeHintType, Object> hints = new java.util.EnumMap<>(com.google.zxing.DecodeHintType.class);
        hints.put(com.google.zxing.DecodeHintType.PURE_BARCODE, Boolean.TRUE);

        Result result = new MultiFormatReader().decode(bitmap, hints);
        return result.getText();
    }

    /**
     * Extracts the href attribute value from the first <a> tag in the HTML fragment.
     */
    private String extractHrefFromHtml(String html) {
        Matcher matcher = HREF_PATTERN.matcher(html);
        assertThat(matcher.find())
                .as("codigoIncrustar debe contener un elemento <a> con atributo href")
                .isTrue();
        return matcher.group(1);
    }

    // ========================================================================
    // Arbitraries / Providers
    // ========================================================================

    @Provide
    Arbitrary<String> slugValido() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .numeric()
                .withChars('-')
                .ofMinLength(3)
                .ofMaxLength(30)
                .filter(s -> s.matches("^[a-z0-9-]{3,120}$"));
    }

    @Provide
    Arbitrary<String> nombreEmpresa() {
        return Arbitraries.strings()
                .withCharRange('A', 'Z')
                .withCharRange('a', 'z')
                .withChars(' ')
                .ofMinLength(3)
                .ofMaxLength(40)
                .filter(s -> !s.isBlank());
    }

    @Provide
    Arbitrary<String> nivelEcologico() {
        return Arbitraries.of("BRONCE", "PLATA", "ORO", "PLATINO", "SIN_NIVEL");
    }

    @Provide
    Arbitrary<String> logoUrl() {
        return Arbitraries.of(
                "https://cdn.example.com/logo.png",
                "https://images.carbonhub.app/empresa/logo.jpg",
                "",
                null
        );
    }
}
