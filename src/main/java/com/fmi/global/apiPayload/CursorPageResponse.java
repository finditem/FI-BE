package com.fmi.global.apiPayload;

import java.util.List;

public record CursorPageResponse<T>(
        List<T> content,
        Long nextCursor,
        boolean hasNext
) {
}
