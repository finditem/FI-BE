package com.fmi.domain.post.response;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.Enum.Status;
import com.fmi.domain.Enum.Type;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponse {

    private Long postId;
    private String title;
    private String content;
    private String address;
    private double latitude;
    private double longitude;
    private Type postType;
    private Status itemStatus;
    private List<String> imageUrls;
    private double radius;
    private Category category;
    private Integer favoriteCount;

    private boolean favoriteStatus;

    private Long viewcount;
}
