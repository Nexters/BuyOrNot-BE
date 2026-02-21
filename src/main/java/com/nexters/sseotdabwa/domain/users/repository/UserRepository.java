package com.nexters.sseotdabwa.domain.users.repository;

import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findBySocialIdAndSocialAccount(String socialId, SocialAccount socialAccount);

    boolean existsByNickname(String nickname);

    List<User> findByIdIn(List<Long> ids);
}
