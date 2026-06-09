package com.fashion.auth.security;

import com.fashion.auth.model.User;
import com.fashion.auth.repository.UserRepository;
import jakarta.servlet.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

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
        String registrationId = null;
        if (authentication instanceof OAuth2AuthenticationToken token) {
            registrationId = token.getAuthorizedClientRegistrationId();
        }

        logger.debug("OAuth2 authentication success [{}], attributes: {}", registrationId, oauthUser.getAttributes());

        String email = oauthUser.getAttribute("email");
        if (email == null && "facebook".equalsIgnoreCase(registrationId)) {
            String facebookId = oauthUser.getAttribute("id");
            if (facebookId != null) {
                email = "facebook-" + facebookId + "@oauth2.local";
                logger.warn("Facebook OAuth2 login has no email; using synthetic email {}", email);
            }
        }
        if (email == null) {
            logger.warn("OAuth2 login: email not provided. attributes={}", oauthUser.getAttributes());
            getRedirectStrategy().sendRedirect(request, response,
                    frontendUrl + "/oauth2/redirect?error=email_not_provided");
            return;
        }

        String name = oauthUser.getAttribute("name");
        if (name == null || name.isBlank()) {
            name = email.split("@")[0];
        }

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

        final String finalName      = name;
        final String finalAvatarUrl = avatarUrl;
        final String finalEmail     = email;

        User user = userRepository.findByEmail(finalEmail).orElseGet(() -> {
            User newUser = User.builder()
                    .fullName(finalName)
                    .email(finalEmail)
                    .passwordHash("{oauth2}" + UUID.randomUUID()) 
                    .avatarUrl(finalAvatarUrl)
                    .role(User.Role.buyer)
                    .isActive(true)
                    .build();
            User saved = userRepository.save(newUser);
            logger.info("Created new OAuth2 user: {}", saved.getEmail());
            return saved;
        });

        if (finalAvatarUrl != null && !finalAvatarUrl.equals(user.getAvatarUrl())) {
            user.setAvatarUrl(finalAvatarUrl);
            userRepository.save(user);
            logger.debug("Updated avatar for user {}", user.getEmail());
        }

        String token = jwtUtils.generateToken(
                user.getFullName(), user.getEmail(), user.getRole().name());

        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendUrl + "/oauth2/redirect")
                .queryParam("token", token)
                .build().toUriString();

        logger.info("OAuth2 login succeeded for {}, redirecting to {}", user.getEmail(), redirectUrl);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}