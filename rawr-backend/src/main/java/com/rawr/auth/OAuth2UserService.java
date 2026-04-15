package com.rawr.auth;

import com.rawr.auth.dto.OAuthAttributes;
import com.rawr.user.User;
import com.rawr.user.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public OAuth2UserService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) {
        OAuth2User oAuth2User = super.loadUser(request);
        String registrationId = request.getClientRegistration().getRegistrationId();
        Map<String, Object> attrs = oAuth2User.getAttributes();

        OAuthAttributes oauthAttrs = switch (registrationId) {
            case "google" -> OAuthAttributes.ofGoogle(attrs);
            case "kakao"  -> OAuthAttributes.ofKakao(attrs);
            default -> throw new IllegalArgumentException("Unsupported provider: " + registrationId);
        };

        User user = userRepository
                .findByOauthProviderAndOauthId(oauthAttrs.provider(), oauthAttrs.oauthId())
                .orElseGet(() -> userRepository.save(oauthAttrs.toUser()));

        user.updateProfile(oauthAttrs.name(), oauthAttrs.profileImage());
        userRepository.save(user);

        String jwt = jwtUtil.generate(user);
        return new DefaultOAuth2User(
                Set.of(() -> "ROLE_" + user.getRole().name()),
                Map.of("jwt", jwt, "sub", user.getId().toString()),
                "sub"
        );
    }
}
