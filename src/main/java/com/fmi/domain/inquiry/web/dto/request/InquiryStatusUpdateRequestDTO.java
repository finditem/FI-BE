package com.fmi.domain.inquiry.web.dto.request;

import com.fmi.domain.inquiry.data.enums.InquiryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InquiryStatusUpdateRequestDTO {
    @NotNull private InquiryStatus status; // RECEIVED, PENDING, ANSWERED
}
