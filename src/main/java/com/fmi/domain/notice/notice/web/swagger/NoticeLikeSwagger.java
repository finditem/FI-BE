package com.fmi.domain.notice.notice.web.swagger;

import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.userdetails.UserDetails;

@Tag(name = "Notice", description = "공지사항 API")
public interface NoticeLikeSwagger {

    @Operation(summary = "공지사항 추천 추가", description = "공지사항을 추천합니다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추천 성공")})
    ApiResponse<Void> addLike(Long noticeId, UserDetails userDetails);

    @Operation(summary = "공지사항 추천 취소", description = "공지사항 추천을 취소합니다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추천 취소 성공")
    })
    ApiResponse<Void> removeLike(Long noticeId, UserDetails userDetails);
}
