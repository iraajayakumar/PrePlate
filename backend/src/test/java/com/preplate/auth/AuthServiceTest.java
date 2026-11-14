package com.preplate.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.preplate.common.exception.ConflictException;
import com.preplate.common.exception.UnauthorizedException;
import com.preplate.user.entity.User;
import com.preplate.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository users;
    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(users, new TokenStore());
    }

    @Test
    void shouldHashPasswordAndAuthenticateWithRawPassword() {
        String hash = service.hashPassword("student123");
        User user = new User("RA001", "Test Student", hash,
                com.preplate.common.enums.Role.STUDENT, "CSE", "Kaveri", "101");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(users.findByRegistrationNumber("RA001")).thenReturn(Optional.of(user));

        var result = service.login("RA001", "student123");

        assertNotEquals("student123", hash);
        assertTrue(service.passwordMatches("student123", hash));
        assertEquals(user, result.user());
        assertNotNull(result.token());
    }

    @Test
    void shouldRejectWrongPasswordWhenLoggingIn() {
        User user = new User("RA001", "Test Student", service.hashPassword("student123"),
                com.preplate.common.enums.Role.STUDENT, "CSE", "Kaveri", "101");
        when(users.findByRegistrationNumber("RA001")).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedException.class, () -> service.login("RA001", "wrongpass"));
    }

    @Test
    void shouldRejectDuplicateRegistrationWhenRegistrationNumberExists() {
        when(users.existsByRegistrationNumber("RA001")).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.register("RA001", "Test Student",
                "CSE", "Kaveri", "101", "student123"));
        verify(users, never()).save(any());
    }
}
