package com.steeltrade.shared.pagination;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Envelope de paginação do contrato REST: conteúdo + total, sempre no
 * mesmo formato, independente da entidade.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <E, T> PageResponse<T> de(Page<E> pagina, java.util.function.Function<E, T> mapper) {
        return new PageResponse<>(
                pagina.getContent().stream().map(mapper).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}
