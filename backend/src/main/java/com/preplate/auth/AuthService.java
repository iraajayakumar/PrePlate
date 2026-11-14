package com.preplate.auth;

import com.preplate.common.enums.Role;
import com.preplate.common.exception.ConflictException;
import com.preplate.common.exception.UnauthorizedException;
import com.preplate.user.entity.User;
import com.preplate.user.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final TokenStore tokens;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository users, TokenStore tokens) {
        this.users = users;
        this.tokens = tokens;
    }

    @Transactional
    public AuthResult login(String registrationNumber, String password) {
        User user = users.findByRegistrationNumber(registrationNumber)
                .filter(candidate -> passwordEncoder.matches(password, candidate.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Invalid registration number or password"));
        return new AuthResult(tokens.create(user.getId()), user);
    }

    @Transactional
    public AuthResult register(String registrationNumber, String fullName, String department,
            String hostelName, String hostelRoom, String password) {
        if (users.existsByRegistrationNumber(registrationNumber)) {
            throw new ConflictException("This registration number is already registered.");
        }
        User user = users.save(new User(registrationNumber, fullName, passwordEncoder.encode(password),
                Role.STUDENT, department, hostelName, hostelRoom));
        return new AuthResult(tokens.create(user.getId()), user);
    }

    public void logout(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            tokens.remove(authorization.substring(7).trim());
        }
    }

    public boolean passwordMatches(String rawPassword, String passwordHash) {
        return passwordEncoder.matches(rawPassword, passwordHash);
    }

    public String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public record AuthResult(String token, User user) {
    }
}
