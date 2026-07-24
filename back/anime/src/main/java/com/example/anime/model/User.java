package com.example.anime.model;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.util.Collection;
import java.util.Collections;

@Data
@Entity
@Table(name = "users")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, columnDefinition = "ENUM('user', 'admin') default 'user'")
    private String role; // user: 普通用户, admin: 管理员

    @Column
    private String email;

    @Column
    private java.util.Date birthday;

    @Column
    private String avatar;

    @Column
    private String gender;

    @Column
    private String region;

    @Column
    private String signature;

    @Column
    private String favorite;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean deleted;

    @Column
    private java.util.Date deletedAt;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean activated;

    @Column
    private String activationCode;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activated != null && activated;
    }
}
