package com.fmi.domain.findcompletion.web.dto.response;

import com.fmi.domain.findcompletion.data.FindCompletion;

public record FindCompletionResponse(Long findCompletionId) {

    public static FindCompletionResponse from(FindCompletion findCompletion) {
        return new FindCompletionResponse(findCompletion.getId());
    }
}
