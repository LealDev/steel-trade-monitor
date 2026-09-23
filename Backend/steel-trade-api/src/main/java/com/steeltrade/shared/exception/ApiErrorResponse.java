package com.steeltrade.shared.exception;

import java.time.OffsetDateTime;

/** Formato único de erro do contrato REST. */
public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String code,
        String message,
        String path
) {
}
