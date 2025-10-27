package com.fmi.domain.inquiry.web.dto.request;

import com.fmi.domain.inquiry.data.enums.InquiryCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InquiryPrivateRequestDTO {
    
    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 200, message = "제목은 200자 이하로 입력해주세요.")
    private String title;
    
    @NotBlank(message = "내용을 입력해주세요.")
    @Size(min = 10, max = 2000, message = "내용은 10자 이상 2000자 이하로 입력해주세요.")
    private String content;
    
    @NotNull(message = "카테고리를 선택해주세요.")
    private InquiryCategory category;
}

