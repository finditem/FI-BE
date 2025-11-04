package com.fmi.domain.comment.web.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentDto {
    private String content;
    private Long parentId;
}
