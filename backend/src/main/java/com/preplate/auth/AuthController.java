package com.preplate.auth;

import static com.preplate.common.dto.ApiDtos.*;

import com.preplate.auth.AuthService.AuthResult;
import com.preplate.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authResponse(authService.login(request.registrationNumber(), request.password()));
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authResponse(authService.register(request.registrationNumber(), request.fullName(),
                request.department(), request.hostelName(), request.hostelRoom(), request.password()));
    }

    @PostMapping("/logout")
    public MessageResponse logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(authorization);
        return new MessageResponse("Logged out");
    }

    @GetMapping("/me")
    public UserResponse me(HttpServletRequest request) {
        return userResponse((User) request.getAttribute(AuthInterceptor.USER_ATTRIBUTE));
    }

    private AuthResponse authResponse(AuthResult result) {
        return new AuthResponse(result.token(), userResponse(result.user()));
    }

    private UserResponse userResponse(User user) {
        return new UserResponse(user.getId(), user.getRegistrationNumber(), user.getFullName(), user.getRole(),
                user.getDepartment(), user.getHostelName(), user.getHostelRoom());
    }
}
