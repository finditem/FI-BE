package com.fmi.domain.post.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fmi.domain.Enum.Status;
import com.fmi.domain.Enum.Type;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostListResponse {
    private Long postId;
    private String title;
    private String summary; // content 앞부분 , 미리보기
    private String thumbnailUrl; // 대표 이미지 1장
    private String address;
    private Status itemStatus;
    private Type postType;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

}
