package com.steeltrade.shared.config;

import java.time.Duration;

import com.steeltrade.ingestion.comexstat.dto.ComexStatRawResponse;
import com.steeltrade.shared.exception.ExternalSourceException;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Proteções da chamada ao Comex Stat.
 *
 * Retry: o rate limit da fonte é ~1 req/10s, então o backoff começa acima
 * disso (12s) e dobra com jitter — sem jitter, clientes que falham juntos
 * tentam de novo juntos.
 *
 * Circuit breaker: após falhas de comunicação seguidas, falha rápido por um
 * tempo em vez de segurar threads esperando uma fonte que está fora.
 * 429 não conta como falha do circuito: limite de cota não significa
 * fonte caída.
 */
@Configuration
public class ResilienceConfig {

    @Bean
    public Retry comexStatRetry() {
        var config = RetryConfig.<ComexStatRawResponse>custom()
                .maxAttempts(3)
                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(
                        Duration.ofSeconds(12), 2.0, 0.5))
                .retryOnException(ex -> ex instanceof ExternalSourceException)
                .retryOnResult(resposta -> resposta.statusHttp() == 429 || resposta.statusHttp() >= 500)
                .build();
        return Retry.of("comexstat", config);
    }

    @Bean
    public CircuitBreaker comexStatCircuitBreaker() {
        var config = CircuitBreakerConfig.custom()
                .slidingWindowSize(4)
                .minimumNumberOfCalls(4)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .permittedNumberOfCallsInHalfOpenState(1)
                .recordException(ex -> ex instanceof ExternalSourceException)
                .build();
        return CircuitBreaker.of("comexstat", config);
    }
}
