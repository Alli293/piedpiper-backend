package com.piedpiper.carbonhub.notification;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailPlantillaHtmlTest {

    @Test
    void botonEscapaUrlYTextoSinAlterarCaracteresUtf8() {
        String html = EmailPlantillaHtml.boton(
                "https://carbonhub.example/?dato=\"<script>",
                "Ver invitación <script>");

        assertThat(html)
                .contains("Ver invitación &lt;script&gt;")
                .contains("dato=&quot;&lt;script&gt;")
                .doesNotContain("<script>");
    }
}
