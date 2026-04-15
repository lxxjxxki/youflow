package com.rawr.auth.dto;

import com.rawr.user.OAuthProvider;
import com.rawr.user.User;

import java.util.Map;

public record OAuthAttributes(
        String oauthId,
        String email,
        String name,
        String profileImage,
        OAuthProvider provider
) {
    public static OAuthAttributes ofGoogle(Map<String, Object> attrs) {
        return new OAuthAttributes(
                (String) attrs.get("sub"),
                (String) attrs.get("email"),
                (String) attrs.get("name"),
                (String) attrs.get("picture"),
                OAuthProvider.GOOGLE
        );
    }

    @SuppressWarnings("unchecked")
    public static OAuthAttributes ofKakao(Map<String, Object> attrs) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attrs.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        return new OAuthAttributes(
                String.valueOf(attrs.get("id")),
                (String) kakaoAccount.get("email"),
                (String) profile.get("nickname"),
                (String) profile.get("profile_image_url"),
                OAuthProvider.KAKAO
        );
    }

    public User toUser() {
        return new User(email, name, profileImage, provider, oauthId);
    }
}
