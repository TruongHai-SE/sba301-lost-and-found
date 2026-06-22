package com.sba301.lostandfound.entity;

import lombok.Builder;
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

import lombok.AllArgsConstructor;

@Entity
@Table(name = "users")
@Builder
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

    @Column(name = "create_at")
    private LocalDate createAt;

    protected User() {
    }

    public static User createUser(String name, String mail, String encodedPassword,
                                  String phone, UserType type) {
        User user = new User();
        user.name = name;
        user.mail = mail;
        user.password = encodedPassword;
        user.phone = phone;
        user.type = type;
        user.createAt = LocalDate.now();
        return user;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public UserType getType() {
        return type;
    }

    public String getPhone() {
        return phone;
    }

    public String getMail() {
        return mail;
    }

    public String getSocialLink() {
        return socialLink;
    }

    public LocalDate getCreateAt() {
        return createAt;
    }
}
