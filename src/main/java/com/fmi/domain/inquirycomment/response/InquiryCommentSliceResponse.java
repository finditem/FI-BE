package com.fmi.domain.inquirycomment.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class InquiryCommentSliceResponse {
    private List<InquiryCommentResponse> comments;
    private boolean hasNext;
    private Long cursor;
}
