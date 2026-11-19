package br.com.encostasegura.shared.api.dto;

public record PaginationMetadata(
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
