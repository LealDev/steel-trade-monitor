package com.steeltrade.shared.exception;

/**
 * Falha de comunicação com uma fonte externa (rede, timeout, DNS).
 * Não cobre respostas HTTP de erro — essas chegam inteiras e viram
 * linha de staging com status ERRO (dead letter).
 */
public class ExternalSourceException extends RuntimeException {

    public ExternalSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
