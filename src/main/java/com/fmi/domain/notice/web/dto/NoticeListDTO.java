package com.fmi.domain.notice.web.dto;

import com.fmi.domain.notice.data.enums.NoticeCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "공지사항 목록 응답")
public class NoticeListDTO {
    @Schema(description = "공지사항 ID", example = "1")
    private Long noticeId;
    @Schema(description = "제목", example = "서비스 점검 안내")
    private String title;
    @Schema(description = "카테고리", example = "GENERAL")
    private NoticeCategory category;
    @Schema(description = "상단 고정 여부", example = "true")
    private Boolean pinned;
    @Schema(description = "조회수", example = "100")
    private Integer viewCount;
    @Schema(description = "추천수", example = "12")
    private Integer likeCount;
    @Schema(description = "댓글수", example = "5")
    private Integer commentCount;
    @Schema(description = "썸네일 이미지 URL")
    private String thumbnailUrl;
    @Schema(description = "내용 요약 (최대 100자)", example = "서비스 점검으로 인해 일시적으로 이용이 제한됩니다.")
    private String summary;
    @Schema(description = "NEW 표시 (24시간 이내)", example = "true")
    private Boolean isNew;
    @Schema(description = "HOT 표시 (인기 공지)", example = "false")
    private Boolean isHot;
    @Schema(description = "생성 시간", example = "2024-01-01T00:00:00")
    private LocalDateTime createdAt;
}

