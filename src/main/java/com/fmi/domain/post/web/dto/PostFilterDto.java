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
    private LocalDate startDate; // 시작일
    private LocalDate endDate;   // 종료일

    private boolean favoriteStatus; // 즐찾 상태

    @Builder.Default
    private Status itemStatus = Status.SEARCHING;// 물건 찾는중/찾음 상태
    @Builder.Default
    private SortType sortType = SortType.LATEST;
}
