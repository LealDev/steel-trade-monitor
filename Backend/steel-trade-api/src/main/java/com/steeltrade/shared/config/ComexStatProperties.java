package com.steeltrade.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração da fonte Comex Stat.
 * O user-agent de navegador é obrigatório: o Cloudflare na frente da API
 * bloqueia user-agents de ferramenta (curl/Java) em alguns endpoints.
 * A pausa entre meses existe porque a coleta é sempre mensal — a API
 * trunca silenciosamente janelas longas — e o rate limit é ~1 req/10s.
 */
@ConfigurationProperties(prefix = "comexstat")
public record ComexStatProperties(
        String baseUrl,
        String userAgent,
        int mesesJanelaMovel,
        long pausaEntreMesesMs
) {
}
