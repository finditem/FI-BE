package com.fmi.domain.post.findcompletion.web.controller;

import com.fmi.domain.post.findcompletion.data.FindCompletion;
import com.fmi.domain.post.findcompletion.service.FindCompletionService;
import com.fmi.domain.post.findcompletion.web.dto.response.FindCompletionResponse;
import com.fmi.domain.post.findcompletion.web.swagger.FindCompletionSwagger;
import com.fmi.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FindCompletionController implements FindCompletionSwagger {

    private final FindCompletionService findCompletionService;

    @Override
    @PostMapping("/chats/{roomId}/find-completion")
    public ApiResponse<FindCompletionResponse> complete(
            @PathVariable Long roomId, @AuthenticationPrincipal UserDetails userDetails) {
        FindCompletion findCompletion = findCompletionService.complete(roomId, userDetails.getUsername());
        return ApiResponse.onSuccess(FindCompletionResponse.from(findCompletion));
    }
}
