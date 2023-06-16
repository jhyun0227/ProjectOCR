package com.practiceocr.config.auth.service;

import com.practiceocr.config.auth.dto.OAuthAttributes;
import com.practiceocr.config.auth.dto.SessionUser;
import com.practiceocr.user.entity.User;
import com.practiceocr.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.Optional;

/**
 * 구글로부터 전달된 userRequest 데이터에 대한 후처리 되는 함수
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final HttpSession httpSession;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        //구글로 부터 전달된 데이터
        log.info("userRequest = {}", userRequest.toString());

        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();

        //loadUser를 통해 구글로부터 회원프로필을 받는다.
        OAuth2User oAuth2User = delegate.loadUser(userRequest);
        log.info("oAuth2User = {}", oAuth2User.toString());

        //제공하는 서비스명
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("registrationId = {}", registrationId);
        //PK...
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        log.info("userNameAttributeName = {}", userNameAttributeName);
        //Attribute
        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());
        log.info("attributes = {}", attributes.toString());

        //구글로부터 전달받은 정보를 저장하거나 수정
        User user = saveOrUpdate(attributes);

        //Dto에 정보를 담아 세션 저장
        httpSession.setAttribute("user", new SessionUser(user));

        //여긴 이해 안감웃
        return new DefaultOAuth2User(Collections.singleton(new SimpleGrantedAuthority(user.getRoleKey())),
                attributes.getAttributes(),
                attributes.getNameAttributeKey());
    }

    /**
     * 이미 정보가 DB에 저장되어있으면 수정, 아니면 저장
     * 수정의 경우 Dirty Checkin 기능을 사용할 수 없음... 나중에 고민해봐야할 문제일 것
     */
    private User saveOrUpdate(OAuthAttributes attributes) {
        User user = userRepository.findByEmail(attributes.getEmail())
                .map(entity -> entity.update(attributes.getName(),attributes.getPicture()))
                .orElse(attributes.toEntity());

        //수정의 경우 merge의 기능을 사용한다.
        return userRepository.save(user);

    }
}
