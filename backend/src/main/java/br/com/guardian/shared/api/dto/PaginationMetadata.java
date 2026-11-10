package br.com.guardian.shared.api.dto;

public record PaginationMetadata(
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
