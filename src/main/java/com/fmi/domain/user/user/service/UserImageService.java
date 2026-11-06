package com.fmi.domain.user.user.service;

import com.fmi.domain.user.user.converter.UserConverter;
import com.fmi.domain.user.user.web.dto.response.ImageUploadResponse;
import com.fmi.external.storage.S3Service;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserImageService {

    private final S3Service s3Service;

    /**
     * 이미지 업로드 (여러 장)
     */
    public ImageUploadResponse uploadImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new GeneralException(ErrorStatus._NOT_EXIST_FILE);
        }

        List<String> imageUrls = s3Service.upload(files);
        return UserConverter.toImageUploadResponse(imageUrls);
    }
}
