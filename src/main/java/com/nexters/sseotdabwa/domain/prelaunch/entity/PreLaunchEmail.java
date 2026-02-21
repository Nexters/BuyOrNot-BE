package com.nexters.sseotdabwa.domain.prelaunch.entity;

import com.nexters.sseotdabwa.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pre_launch_emails")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PreLaunchEmail extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Builder
    public PreLaunchEmail(String email) {
        this.email = email;
    }
}
