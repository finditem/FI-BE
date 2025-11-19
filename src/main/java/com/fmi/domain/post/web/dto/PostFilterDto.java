package com.fmi.domain.post.web.dto;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.Enum.SortType;
import com.fmi.domain.Enum.Status;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Builder
@Data
public class PostFilterDto {

    private Category category;   // 카테고리
    private String address;     // 지역
    private Status itemStatus;       // 물건 찾는중/찾음 상태
    private LocalDate StartDate; // 시작일
    private LocalDate EndDate;   // 종료일
    private SortType sortType;   // 정렬
}
