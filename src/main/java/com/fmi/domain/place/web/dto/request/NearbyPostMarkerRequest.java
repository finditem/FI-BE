package com.fmi.domain.place.web.dto.request;

import com.fmi.domain.post.post.data.enums.Category;
import com.fmi.domain.post.post.data.enums.PostStatus;
import com.fmi.domain.post.post.data.enums.PostType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NearbyPostMarkerRequest {

    private PostType postType;
    private PostStatus postStatus;
    private Category category;
}
