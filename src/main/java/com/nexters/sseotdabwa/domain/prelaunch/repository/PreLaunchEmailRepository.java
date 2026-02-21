package com.nexters.sseotdabwa.domain.prelaunch.repository;

import com.nexters.sseotdabwa.domain.prelaunch.entity.PreLaunchEmail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreLaunchEmailRepository extends JpaRepository<PreLaunchEmail, Long> {

    boolean existsByEmail(String email);
}
