package com.fmi.domain.notice.web.dto;

import com.fmi.domain.notice.data.enums.NoticeCategory;
import com.fmi.domain.noticecomment.response.NoticeCommentResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "공지사항 상세 응답")
public class NoticeResponseDTO {
    @Schema(description = "공지사항 ID", example = "1")
    private Long noticeId;
    @Schema(description = "제목", example = "서비스 점검 안내")
    private String title;
    @Schema(description = "내용", example = "서비스 점검으로 인해 일시적으로 이용이 제한됩니다.")
    private String content;
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
    @Schema(description = "작성자", example = "관리자")
    private String authorName;
    @Schema(description = "썸네일 이미지 URL")
    private String thumbnailUrl;
    @Schema(description = "이미지 URL 목록")
    private List<String> images;
    @Schema(description = "NEW 표시 (24시간 이내)", example = "true")
    private Boolean isNew;
    @Schema(description = "HOT 표시 (인기 공지)", example = "false")
    private Boolean isHot;
    @Schema(description = "내가 좋아요 눌렀는지 여부", example = "false")
    private Boolean likeStatus;
    @Schema(description = "댓글 목록")
    private List<NoticeCommentResponse> comments;
    @Schema(description = "생성 시간", example = "2024-01-01T00:00:00")
    private LocalDateTime createdAt;
    @Schema(description = "수정 시간", example = "2024-01-01T00:00:00")
    private LocalDateTime updatedAt;
}

