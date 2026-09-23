package com.steeltrade.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração da fonte Comex Stat.
 * O user-agent de navegador é obrigatório: o Cloudflare na frente da API
 * bloqueia user-agents de ferramenta (curl/Java) em alguns endpoints.
 */
@ConfigurationProperties(prefix = "comexstat")
public record ComexStatProperties(
        String baseUrl,
        String userAgent,
        int mesesJanelaMovel
) {
}
