//package com.fashion.auth.security;
//
//import com.fashion.auth.model.User;
//import com.fashion.auth.repository.UserRepository;
//import jakarta.servlet.http.*;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.oauth2.core.user.OAuth2User;
//import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
//import org.springframework.stereotype.Component;
//import org.springframework.web.util.UriComponentsBuilder;
//
//import java.io.IOException;
//import java.util.UUID;
//
//@Component
//public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
//
//    private final JwtUtils jwtUtils;
//    private final UserRepository userRepository;
//
//    @Value("${app.frontend-url}")
//    private String frontendUrl;
//
//    public OAuth2SuccessHandler(JwtUtils jwtUtils, UserRepository userRepository) {
//        this.jwtUtils       = jwtUtils;
//        this.userRepository = userRepository;
//    }
//
//    @Override
//    public void onAuthenticationSuccess(HttpServletRequest request,
//                                        HttpServletResponse response,
//                                        Authentication authentication) throws IOException {
//        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
//
//        String email    = oauthUser.getAttribute("email");
//        String name     = oauthUser.getAttribute("name");
//        String picture  = oauthUser.getAttribute("picture");
//
//        // Find or create user
//        User user = userRepository.findByEmail(email).orElseGet(() -> {
//            User newUser = User.builder()
//                    .fullName(name != null ? name : email.split("@")[0])
//                    .email(email)
//                    .passwordHash(UUID.randomUUID().toString()) // placeholder
//                    .avatarUrl(picture)
//                    .role(User.Role.buyer)
//                    .isActive(true)
//                    .build();
//            return userRepository.save(newUser);
//        });
//
//        String token = jwtUtils.generateToken(user.getFullName(), user.getEmail(), user.getRole().name());
//
//        String redirectUrl = UriComponentsBuilder
//                .fromUriString(frontendUrl + "/oauth2/redirect")
//                .queryParam("token", token)
//                .build().toUriString();
//
//        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
//    }
//}
package com.fashion.auth.security;

import com.fashion.auth.model.User;
import com.fashion.auth.repository.UserRepository;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public OAuth2SuccessHandler(JwtUtils jwtUtils, UserRepository userRepository) {
        this.jwtUtils       = jwtUtils;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        // ── Email ──────────────────────────────────────────────────────────────
        String email = oauthUser.getAttribute("email");
        if (email == null) {
            getRedirectStrategy().sendRedirect(request, response,
                    frontendUrl + "/oauth2/redirect?error=email_not_provided");
            return;
        }

        // ── Name ───────────────────────────────────────────────────────────────
        String name = oauthUser.getAttribute("name");
        if (name == null || name.isBlank()) {
            name = email.split("@")[0];
        }

        // ── Avatar URL ─────────────────────────────────────────────────────────
        // Google  → "picture" is a plain String
        // Facebook → "picture" is Map { "data": { "url": "...", ... } }
        String avatarUrl = null;
        Object pictureRaw = oauthUser.getAttribute("picture");
        if (pictureRaw instanceof String) {
            avatarUrl = (String) pictureRaw;
        } else if (pictureRaw instanceof Map<?, ?> pictureMap) {
            try {
                Object data = pictureMap.get("data");
                if (data instanceof Map<?, ?> dataMap) {
                    Object url = dataMap.get("url");
                    if (url instanceof String) avatarUrl = (String) url;
                }
            } catch (Exception ignored) { /* keep null */ }
        }

        // ── Find or create user ────────────────────────────────────────────────
        final String finalName      = name;
        final String finalAvatarUrl = avatarUrl;

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .fullName(finalName)
                    .email(email)
                    .passwordHash("{oauth2}" + UUID.randomUUID()) // placeholder, not used for login
                    .avatarUrl(finalAvatarUrl)
                    .role(User.Role.buyer)
                    .isActive(true)
                    .build();
            return userRepository.save(newUser);
        });

        // Update avatar if it changed
        if (finalAvatarUrl != null && !finalAvatarUrl.equals(user.getAvatarUrl())) {
            user.setAvatarUrl(finalAvatarUrl);
            userRepository.save(user);
        }

        String token = jwtUtils.generateToken(
                user.getFullName(), user.getEmail(), user.getRole().name());

        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendUrl + "/oauth2/redirect")
                .queryParam("token", token)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}