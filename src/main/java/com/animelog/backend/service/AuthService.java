package com.animelog.backend.service;

import com.animelog.backend.dto.LoginVO;
import com.animelog.backend.dto.UserVO;

/**
 * 认证业务接口，提供登录、注册和验证码发送功能。
 */
public interface AuthService {
    /** 用户登录，返回包含 JWT Token 和用户信息的 LoginVO。 */
    LoginVO login(String identifier, String password);

    /** 用户注册，创建新账号并返回用户视图对象。 */
    UserVO register(String username, String nickname, String password, String email, String phone, String code);

    /** 验证码登录，通过邮箱或手机号 + 验证码验证身份，返回 JWT Token 和用户信息。 */
    LoginVO loginWithCode(String identifier, String code);

    /** 重设密码（忘记密码），通过验证码验证身份后设置新密码。 */
    void resetPassword(String identifier, String code, String newPassword);

    /** 发送邮箱验证码。 */
    void sendEmailCode(String target, String purpose);

    /** 发送短信验证码。 */
    void sendSmsCode(String target, String purpose);
}
