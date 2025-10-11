package com.fmi.domain.post.data;

import com.fmi.domain.Enum.Status;
import com.fmi.domain.Enum.Type;
import com.fmi.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "latitude")
    private double latitude;

    @Column(name = "longitude")
    private double longitude;

    @Column(name = "view_cnt")
    private long viewCnt;

    @Column(name = "item_status")
    private Status itemStatus;

    @Column(name = "post_type")
    private Type postType;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "p_content", nullable = false)
    private String pContent;

    @Column(name = "temporarySave",nullable = false)
    private Boolean temporarySave;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "radius")
    private double radius;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<PostImage> images = new ArrayList<>();

}
