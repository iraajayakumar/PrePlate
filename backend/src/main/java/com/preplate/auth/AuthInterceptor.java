package com.preplate.auth;

import com.preplate.common.enums.Role;
import com.preplate.common.exception.ForbiddenException;
import com.preplate.common.exception.UnauthorizedException;
import com.preplate.user.entity.User;
import com.preplate.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    public static final String USER_ATTRIBUTE = "authenticatedUser";
    private final TokenStore tokenStore;
    private final UserRepository userRepository;

    public AuthInterceptor(TokenStore tokenStore, UserRepository userRepository) {
        this.tokenStore = tokenStore;
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException("Please sign in again.");
        }
        Long userId = tokenStore.userId(authorization.substring(7).trim());
        User user = userId == null ? null : userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new UnauthorizedException("Please sign in again.");
        }
        if (request.getRequestURI().startsWith("/api/admin/") && user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Admin access required.");
        }
        request.setAttribute(USER_ATTRIBUTE, user);
        return true;
    }
}
