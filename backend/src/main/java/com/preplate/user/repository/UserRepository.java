package com.preplate.user.repository;

import com.preplate.common.enums.Role;
import com.preplate.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByRegistrationNumber(String registrationNumber);
    boolean existsByRegistrationNumber(String registrationNumber);
    long countByRole(Role role);
    List<User> findAllByRole(Role role);
}
