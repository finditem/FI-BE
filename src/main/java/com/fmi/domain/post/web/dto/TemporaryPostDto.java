package com.fmi.domain.post.web.dto;

import com.fmi.domain.Enum.Category;
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
public class TemporaryPostDto {
    private Type postType;
    private String title;
    private Status itemStatus;
    private LocalDate date;
    private String address;
    private double latitude;
    private double longitude;
    private String content;
    private double radius;
    private boolean temporary_save;
    private Category category;

    private List<Long> deleteImageIds;
}
