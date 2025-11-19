package com.fmi.domain.post.web.dto;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.Enum.Status;

import java.time.LocalDate;

public class PostFilterDto {

    private Category category;
    private String location;
    private Status status;      //물건 찾는중/찾음 상태
    private LocalDate fromDate;
}
