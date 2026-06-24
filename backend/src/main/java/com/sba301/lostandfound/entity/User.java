package com.sba301.lostandfound.entity;

import com.sba301.lostandfound.entity.enums.UserType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(columnDefinition = "text")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private UserType type;

    @Column(length = 10, unique = true)
    private String phone;

    @Column(unique = true)
    private String mail;

    @Column(name = "social_link", columnDefinition = "text")
    private String socialLink;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDate createdAt = LocalDate.now();
}
