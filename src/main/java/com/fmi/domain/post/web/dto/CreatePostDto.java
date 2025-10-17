package com.fmi.domain.post.web.dto;

import com.fmi.domain.Enum.Status;
import com.fmi.domain.Enum.Type;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CreatePostDto {
    private Type postType; // 분실/습득
    private String title;
    private Status itemStatus; //찾는중 상태
    private LocalDate date;
    private String address;
    private double latitude;
    private double longitude;
    private String content;
    private double radius;

    private boolean temporarySave;

}
