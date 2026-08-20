package com.fmi.domain.user.web.dto.response;

public record UserPostResponse(Long userId, String nickName, String profileImage, long postCount, long chattingCount) {}
