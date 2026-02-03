package com.example.reservation.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * DTO générique pour les réponses paginées.
 *
 * @param <T> Type des éléments de la page
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    /**
     * Crée une PageResponse à partir d'une Page Spring Data.
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    /**
     * Crée une PageResponse à partir d'une Page Spring Data avec transformation des éléments.
     */
    public static <T, R> PageResponse<R> from(Page<T> page, Function<T, R> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    /**
     * Crée une PageResponse pour une liste non paginée (unpaged).
     * Utilisé pour la rétro-compatibilité avec ?unpaged=true.
     */
    public static <T> PageResponse<T> unpaged(List<T> content) {
        return new PageResponse<>(
                content,
                0,
                content.size(),
                content.size(),
                1,
                true,
                true
        );
    }
}
