package com.mcp.gateway.model;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long total,
        int totalPages
) {
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;

    public static <T> PageResponse<T> of(List<T> allItems, Integer page, Integer size) {
        var normalizedPage = page == null || page < 0 ? 0 : page;
        var normalizedSize = size == null || size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        var total = allItems.size();
        var totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / normalizedSize);
        var fromIndex = Math.min(normalizedPage * normalizedSize, total);
        var toIndex = Math.min(fromIndex + normalizedSize, total);
        return new PageResponse<>(
                allItems.subList(fromIndex, toIndex),
                normalizedPage,
                normalizedSize,
                total,
                totalPages
        );
    }
}
