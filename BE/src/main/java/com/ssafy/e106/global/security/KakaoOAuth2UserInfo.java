package com.ssafy.e106.global.security;

import java.util.Map;

public class KakaoOAuth2UserInfo implements OAuth2UserInfo {

  private final Map<String, Object> attributes;

  public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  @Override
  @SuppressWarnings("unchecked")
  public String getEmail() {
    Object kakaoAccount = attributes.get("kakao_account");
    if (kakaoAccount instanceof Map<?, ?> map) {
      Object email = ((Map<String, Object>) map).get("email");
      return email == null ? null : String.valueOf(email);
    }
    return null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public String getName() {
    Object kakaoAccount = attributes.get("kakao_account");
    if (kakaoAccount instanceof Map<?, ?> map) {
      Object profile = ((Map<String, Object>) map).get("profile");
      if (profile instanceof Map<?, ?> profileMap) {
        Object nickname = ((Map<String, Object>) profileMap).get("nickname");
        return nickname == null ? null : String.valueOf(nickname);
      }
    }
    return null;
  }
}
