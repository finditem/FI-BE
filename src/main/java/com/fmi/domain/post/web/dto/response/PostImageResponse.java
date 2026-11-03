package com.fmi.domain.post.web.dto.response;

import com.fmi.domain.post.data.enums.ImageType;

public record PostImageResponse(Long id, String imgUrl, ImageType imageType) {}
