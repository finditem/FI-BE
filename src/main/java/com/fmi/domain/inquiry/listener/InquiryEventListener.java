package com.fmi.domain.inquiry.listener;

import com.fmi.domain.inquiry.event.InquiryEvent;
import com.fmi.global.service.SlackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class InquiryEventListener {

    private final SlackService slackService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleInquiryEvent(InquiryEvent event) {

        slackService.sendInquiryNotification(event);
    }
}
