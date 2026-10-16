package com.bonsaimarket.backend.category;

import jakarta.persistence.*;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categories", uniqueConstraints = {@UniqueConstraint(columnNames="slug"), @UniqueConstraint(columnNames="name_key")})
@Getter
@Setter
@NoArgsConstructor
public class Category {
    @Column(nullable=false)
    private long revision;

    @Column(name="name_key", length=180)
    private String nameKey;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "slug")
    private String slug;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;
}

