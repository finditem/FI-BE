package com.fmi.domain.inquiry.inquiry.web.dto.request;

import com.fmi.domain.inquiry.inquiry.data.enums.InquiryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InquiryStatusUpdateRequest {
    @NotNull private InquiryStatus status; // RECEIVED, PENDING, ANSWERED
}
