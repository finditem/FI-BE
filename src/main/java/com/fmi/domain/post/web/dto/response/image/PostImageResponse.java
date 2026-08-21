package com.fmi.domain.post.web.dto.response.image;

import com.fmi.domain.post.data.ImageType;

public record PostImageResponse(Long id, String imgUrl, ImageType imageType) {}
