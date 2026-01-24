package com.nexters.sseotdabwa.domain.users.repository;

import com.nexters.sseotdabwa.domain.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
