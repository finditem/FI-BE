package com.fmi.domain.chat.message.web.dto.request;

import java.util.List;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

public class ChatMessageRequest {

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class SendImageRequest {
        private String content;
        private List<MultipartFile> images;
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class SendMessageRequest {
        private String content;
    }
}
