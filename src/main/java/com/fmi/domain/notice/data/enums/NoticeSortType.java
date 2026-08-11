package com.fmi.domain.notice.data.enums;

import lombok.Getter;
import org.springframework.data.domain.Sort;

@Getter
public enum NoticeSortType {
    LATEST(Sort.by(Sort.Direction.DESC, "pinned").and(Sort.by(Sort.Direction.DESC, "createdAt"))),
    OLDEST(Sort.by(Sort.Direction.DESC, "pinned").and(Sort.by(Sort.Direction.ASC, "createdAt"))),
    MOST_VIEWED(Sort.by(Sort.Direction.DESC, "pinned").and(Sort.by(Sort.Direction.DESC, "viewCount")));

    private final Sort sort;

    NoticeSortType(Sort sort) {
        this.sort = sort;
    }
}
