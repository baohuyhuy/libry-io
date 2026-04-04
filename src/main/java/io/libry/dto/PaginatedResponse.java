package io.libry.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PaginatedResponse<T>(
        List<T> data,
        int currentPage,
        int totalPages,
        long totalItems,
        int pageSize,
        boolean hasNext,
        boolean hasPrevious
) {
    public static <T> PaginatedResponse<T> from(Page<T> page) {
        return new PaginatedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                page.hasNext(),
                page.hasPrevious()
        );
    }


}
