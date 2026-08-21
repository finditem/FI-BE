package com.fmi.domain.comment.converter;

import com.fmi.domain.comment.data.CommentImage;
import com.fmi.domain.comment.web.dto.response.CommentImageResponse;
import java.util.List;

public final class CommentImageConverter {

    public static CommentImageResponse toCommentImageResponse(CommentImage commentImage) {
        return new CommentImageResponse(commentImage.getId(), commentImage.getImgUrl());
    }

    public static List<CommentImageResponse> toCommentImageResponseList(List<CommentImage> commentImageList) {
        return commentImageList.stream()
                .map(CommentImageConverter::toCommentImageResponse)
                .toList();
    }

    private CommentImageConverter() {}
}
