package com.fmi.domain.user.response;

import java.util.List;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageUploadResponse {
    private List<String> imageUrls;
}
