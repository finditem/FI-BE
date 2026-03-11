package com.fmi.domain.inquiry.web.dto.response;

import com.fmi.domain.inquiry.data.enums.InquiryStatus;
import com.fmi.domain.inquirycomment.response.InquiryCommentResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "문의 상세 응답")
public class InquiryDetailDTO {
    @Schema(description = "유저 닉네임", example = "홍길동")
    private String nickname;
    @Schema(description = "비회원 이메일", example = "guest@example.com")
    private String email;
    @Schema(description = "문의 ID", example = "1")
    private Long inquiryId;
    @Schema(description = "제목", example = "계정 관련 문의")
    private String title;
    @Schema(description = "내용", example = "비밀번호를 변경하고 싶습니다.")
    private String content;
    @Schema(description = "처리 상태", example = "ANSWERED")
    private InquiryStatus status;
    @Schema(description = "생성 시간", example = "2024-01-01T00:00:00")
    private LocalDateTime createdAt;
    @Schema(description = "답변 여부", example = "false")
    private boolean answered;
    @Schema(description = "댓글 목록")
    private List<InquiryCommentResponse> comments;
}
