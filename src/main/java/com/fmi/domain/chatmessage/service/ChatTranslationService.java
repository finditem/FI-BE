package com.fmi.domain.chatmessage.service;

import com.fmi.domain.chatmessage.service.ChatTranslationSourceService.Source;
import com.fmi.domain.chatmessage.service.RedisTranslationQuotaStore.Outcome;
import com.fmi.domain.chatmessage.service.RedisTranslationQuotaStore.Status;
import com.fmi.domain.chatmessage.service.RedisTranslationQuotaStore.Ticket;
import com.fmi.domain.chatmessage.service.RedisTranslationQuotaStore.Usage;
import com.fmi.domain.chatmessage.web.dto.request.ChatTranslationRequest.TranslateRequest;
import com.fmi.domain.chatmessage.web.dto.response.ChatTranslationResponse.TranslationResponse;
import com.fmi.domain.chatmessage.web.dto.response.ChatTranslationResponse.UsageResponse;
import com.fmi.domain.user.service.UserQueryService;
import com.fmi.external.translation.client.TranslationClient;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatTranslationService {

    private final ChatTranslationSourceService chatTranslationSourceService;
    private final RedisTranslationQuotaStore translationQuotaStore;
    private final TranslationClient translationClient;
    private final UserQueryService userQueryService;

    public TranslationResponse translate(Long roomId, Long messageId, String email, TranslateRequest request) {
        Source source = chatTranslationSourceService.find(roomId, messageId, email);
        Ticket ticket = translationQuotaStore.reserve(source, roomId, messageId, request);
        Outcome outcome = ticket.outcome();

        if (outcome.status() == Status.SUCCEEDED) {
            return toResponse(messageId, request, outcome, translationQuotaStore.usage(source.userId()));
        }
        if (outcome.status() != Status.RESERVED) {
            throw toException(outcome);
        }

        String translatedText =
                translationQuotaStore.findCached(ticket).orElseGet(() -> translateOrRelease(ticket, source, request));

        outcome = translationQuotaStore.complete(ticket, translatedText);
        if (outcome.status() != Status.SUCCEEDED) {
            throw toException(outcome);
        }
        Usage usage = translationQuotaStore.usageAfterCompletion(source.userId(), outcome.usage());
        return toResponse(messageId, request, outcome, usage);
    }

    public UsageResponse usage(String email) {
        return toUsageResponse(
                translationQuotaStore.usage(userQueryService.findUser(email).getId()));
    }

    private String translateOrRelease(Ticket ticket, Source source, TranslateRequest request) {
        try {
            String translatedText = translationClient.translate(source.text(), source.language());
            if (translatedText == null || translatedText.isBlank()) {
                throw new GeneralException(ErrorStatus._TRANSLATION_API_ERROR);
            }
            return translatedText;
        } catch (RuntimeException translationFailure) {
            log.warn("채팅 번역 실패: requestId={}", request.requestId(), translationFailure);
            releaseQuietly(ticket, request);
            throw new GeneralException(ErrorStatus._TRANSLATION_API_ERROR);
        }
    }

    private void releaseQuietly(Ticket ticket, TranslateRequest request) {
        try {
            translationQuotaStore.fail(ticket);
        } catch (RuntimeException compensationFailure) {
            log.warn("채팅 번역 예약 반환 실패: requestId={}", request.requestId(), compensationFailure);
        }
    }

    private TranslationResponse toResponse(Long messageId, TranslateRequest request, Outcome outcome, Usage usage) {
        return new TranslationResponse(
                messageId,
                request.requestId(),
                request.roomVisitId(),
                outcome.language(),
                outcome.text(),
                toUsageResponse(usage));
    }

    private UsageResponse toUsageResponse(Usage usage) {
        return new UsageResponse(
                usage.usageDate(), usage.usedCount(), usage.limit(), usage.resetAt(), usage.canTranslate());
    }

    private GeneralException toException(Outcome outcome) {
        if (outcome.status() == Status.LIMIT) {
            return new GeneralException(
                    ErrorStatus._TRANSLATION_LIMIT_EXCEEDED,
                    Map.of(
                            "usedCount", outcome.usage().usedCount(),
                            "limit", outcome.usage().limit(),
                            "nextAvailableAt", outcome.usage().resetAt()));
        }
        return new GeneralException(
                switch (outcome.status()) {
                    case IN_PROGRESS -> ErrorStatus._TRANSLATION_IN_PROGRESS;
                    case CONFLICT -> ErrorStatus._TRANSLATION_REQUEST_CONFLICT;
                    default -> ErrorStatus._TRANSLATION_API_ERROR;
                });
    }
}
