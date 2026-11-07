package com.fmi.external.storage;

import java.util.Arrays;
import java.util.Optional;

/** 파일 내용(시그니처)으로 판별한 업로드 이미지 형식 */
enum ImageFormat {
    JPEG("jpg", "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
    PNG("png", "image/png", new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});

    private final String extension;
    private final String contentType;
    private final byte[] signature;

    ImageFormat(String extension, String contentType, byte[] signature) {
        this.extension = extension;
        this.contentType = contentType;
        this.signature = signature;
    }

    static Optional<ImageFormat> detect(byte[] image) {
        return Arrays.stream(values()).filter(format -> format.matches(image)).findFirst();
    }

    String extension() {
        return extension;
    }

    String contentType() {
        return contentType;
    }

    private boolean matches(byte[] image) {
        return image.length >= signature.length
                && Arrays.equals(image, 0, signature.length, signature, 0, signature.length);
    }
}
