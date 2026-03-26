package com.fmi.domain.post.data;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.auth.data.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Consumer;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 50)
    private String title;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "view_cnt")
    private long viewCount;

    @Column(name = "item_status")
    @Enumerated(EnumType.STRING)
    private PostStatus postStatus;

    @Column(name = "post_type")
    @Enumerated(EnumType.STRING)
    private PostType postType;

    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    @Column(name = "temporary_save", nullable = false)
    private boolean temporarySave;

    @Column(name = "date")
    private LocalDateTime date;

    @Column(name = "radius")
    private Radius radius;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    private Post(String title, String address, double latitude, double longitude, PostType postType, Category category, String content, boolean temporarySave, LocalDateTime date, Radius radius, User user) {
        this.title = title;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.postType = postType;
        this.category = category;
        this.content = content;
        this.temporarySave = temporarySave;
        this.date = date;
        this.radius = radius;
        this.postStatus = PostStatus.SEARCHING;
        this.createdAt = LocalDateTime.now();
        this.user = user;
        this.viewCount = 0;
    }

    public static Post create(String title, String address, double latitude, double longitude, PostType postType, Category category, String content, boolean temporarySave, LocalDateTime date, Radius radius, User user) {
        return new Post(title, address, latitude, longitude, postType, category, content, temporarySave, date, radius, user);
    }

    public static Post createTemporary(User user) {
        Post post = new Post();
        post.user = user;
        post.temporarySave = true;
        post.title = "";
        post.address = "";
        post.content = "";
        post.postStatus = PostStatus.SEARCHING;
        post.createdAt = LocalDateTime.now();
        post.viewCount = 0;

        return post;
    }

    public void updateTemporary(PostType postType,
                                Category category,
                                Radius radius,
                                LocalDateTime date,
                                String title,
                                String address,
                                String content,
                                Double latitude,
                                Double longitude) {

        applyIfNotNull(postType, this::setPostType);
        applyIfNotNull(category, this::setCategory);
        applyIfNotNull(radius, this::setRadius);
        applyIfNotNull(date, this::setDate);

        applyIfNotBlank(title, this::setTitle);
        applyIfNotBlank(address, this::setAddress);
        applyIfNotBlank(content, this::setContent);

        applyIfNotNull(latitude, this::setLatitude);
        applyIfNotNull(longitude, this::setLongitude);

        this.temporarySave = true;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isNew() {
        return this.createdAt.isAfter(LocalDateTime.now().minusHours(24));
    }

    public void updateRadius(Radius radius) {
        applyIfNotNull(radius, this::setRadius);
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePostStatus(PostStatus postStatus) {
        applyIfNotNull(postStatus, this::setPostStatus);
        this.updatedAt = LocalDateTime.now();
    }

    public void update(PostType postType,
                       String title,
                       PostStatus postStatus,
                       LocalDateTime date,
                       String address,
                       Double latitude,
                       Double longitude,
                       String content,
                       Boolean temporarySave,
                       Radius radius,
                       Category category) {

        applyIfNotNull(postType, this::setPostType);
        applyIfNotNull(title, this::setTitle);
        applyIfNotNull(postStatus, this::setPostStatus);
        applyIfNotNull(date, this::setDate);
        applyIfNotNull(address, this::setAddress);
        applyIfNotNull(latitude, this::setLatitude);
        applyIfNotNull(longitude, this::setLongitude);
        applyIfNotNull(content, this::setContent);
        applyIfNotNull(temporarySave, this::setTemporarySave);
        applyIfNotNull(radius, this::setRadius);
        applyIfNotNull(category, this::setCategory);

        this.updatedAt = LocalDateTime.now();
    }

    public String makeSummary() {
        if (Objects.isNull(this.content)) {
            return "";
        }
        return content.length() <= 50 ? content : content.substring(0, 50) + "...";
    }

    public void softDelete() {
        this.deleted = true;
        this.updatedAt = LocalDateTime.now();
    }

    private <T> void applyIfNotNull(T value, Consumer<T> setter) {
        if (Objects.nonNull(value)) setter.accept(value);
    }

    private void applyIfNotBlank(String value, Consumer<String> setter) {
        if (Objects.nonNull(value) && !value.isBlank()) setter.accept(value);
    }


}
