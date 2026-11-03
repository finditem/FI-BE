package com.fmi.domain.post.findcompletion.web.dto.response;

import com.fmi.domain.post.findcompletion.data.FindCompletion;

public record FindCompletionResponse(Long findCompletionId) {

    public static FindCompletionResponse from(FindCompletion findCompletion) {
        return new FindCompletionResponse(findCompletion.getId());
    }
}
