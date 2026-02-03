package com.luna.security;

import com.luna.user.entity.User;
import org.springframework.security.core.Authentication;

public class SecurityUtils {

    private SecurityUtils() {
        // Utility class
    }

    /**
     * Extract the user ID from the authenticated user
     * @param authentication the authentication object from the security context
     * @return the user ID
     * @throws IllegalStateException if the principal is not a User object
     */
    public static Long getUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User)) {
            throw new IllegalStateException("Principal is not a User object");
        }

        return ((User) principal).getId();
    }

    /**
     * Extract the User object from the authenticated user
     * @param authentication the authentication object from the security context
     * @return the User object
     * @throws IllegalStateException if the principal is not a User object
     */
    public static User getUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User)) {
            throw new IllegalStateException("Principal is not a User object");
        }

        return (User) principal;
    }
}
