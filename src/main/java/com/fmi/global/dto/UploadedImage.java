package com.fmi.global.dto;

// 원본 이미지 URL과 목록용 썸네일 이미지 URL 쌍
public record UploadedImage(String originalUrl, String thumbnailUrl) {}
