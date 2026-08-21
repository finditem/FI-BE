package com.fmi.domain.inquirycomment.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InquiryCommentSliceResponse {
    private List<InquiryCommentResponse> comments;
    private boolean hasNext;
    private Long cursor;
}
