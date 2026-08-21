package com.fmi.domain.chatmessage.web.dto;

import java.util.List;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

public class ChatMessageRequestDTO {

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class SendImageRequestDTO {
        private String content;
        private List<MultipartFile> images;
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class SendMessageRequestDTO {
        private String content;
    }
}
