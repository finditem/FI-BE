package com.fmi.domain.notice.converter;

import com.fmi.domain.notice.data.Notice;
import com.fmi.domain.notice.web.dto.NoticeListDTO;
import com.fmi.domain.notice.web.dto.NoticeResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class NoticeConverter {
    
    public NoticeListDTO toListDTO(Notice notice) {
        return NoticeListDTO.builder()
                .noticeId(notice.getNoticeId())
                .title(notice.getTitle())
                .category(notice.getCategory())
                .pinned(notice.getPinned())
                .viewCount(notice.getViewCount())
                .createdAt(notice.getCreatedAt())
                .build();
    }
    
    public NoticeResponseDTO toResponseDTO(Notice notice) {
        return NoticeResponseDTO.builder()
                .noticeId(notice.getNoticeId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .category(notice.getCategory())
                .pinned(notice.getPinned())
                .viewCount(notice.getViewCount())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .build();
    }
}

