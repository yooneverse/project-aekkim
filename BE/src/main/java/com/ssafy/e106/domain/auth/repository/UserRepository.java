package com.ssafy.e106.domain.auth.repository;

import java.util.Optional;

import com.ssafy.e106.domain.auth.entity.User;
import com.ssafy.e106.domain.auth.enums.AuthProvider;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    Optional<User> findByEmail(String email);

}
