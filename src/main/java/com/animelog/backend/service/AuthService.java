package com.animelog.backend.service;

import com.animelog.backend.domain.UserAccount;

import java.util.Map;

/**
 * 认证业务接口，提供登录、注册和验证码发送功能。
 */
public interface AuthService {
    /** 用户登录，返回包含 JWT Token 和用户信息的 Map。 */
    Map<String, Object> login(String identifier, String password);

    /** 用户注册，创建新账号并返回用户信息。 */
    UserAccount register(String username, String nickname, String password, String email, String phone, String code);

    /** 发送邮箱验证码。 */
    void sendEmailCode(String target, String purpose);

    /** 发送短信验证码。 */
    void sendSmsCode(String target, String purpose);
}
