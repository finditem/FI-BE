package com.fmi.domain.post.post.web.dto.response;

import com.fmi.domain.post.post.data.enums.ImageType;

public record PostImageResponse(Long id, String imgUrl, ImageType imageType) {}
