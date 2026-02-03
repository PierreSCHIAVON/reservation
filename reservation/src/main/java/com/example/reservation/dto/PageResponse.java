package com.example.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * DTO générique pour les réponses paginées.
 *
 * @param <T> Type des éléments de la page
 */
@Schema(description = "Réponse paginée")
public record PageResponse<T>(
        @Schema(description = "Contenu de la page")
        List<T> content,

        @Schema(description = "Numéro de la page (0-indexé)", example = "0")
        int page,

        @Schema(description = "Taille de la page", example = "20")
        int size,

        @Schema(description = "Nombre total d'éléments", example = "100")
        long totalElements,

        @Schema(description = "Nombre total de pages", example = "5")
        int totalPages,

        @Schema(description = "Est-ce la première page ?")
        boolean first,

        @Schema(description = "Est-ce la dernière page ?")
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
