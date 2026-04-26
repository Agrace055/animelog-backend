package com.animelog.backend.controller;

import com.animelog.backend.dto.AjaxResult;
import com.animelog.backend.service.AuthService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;


/**
 * 认证控制器，提供用户登录、注册和验证码发送接口。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户登录，支持用户名、邮箱或手机号 + 密码登录。
     *
     * @return 包含 JWT Token 和用户信息的 Map
     */
    @PostMapping("/login")
    public AjaxResult login(@RequestBody LoginRequest request) {
        return AjaxResult.success(authService.login(request.identifier(), request.password()));
    }

    /**
     * 用户注册。
     */
    @PostMapping("/register")
    public AjaxResult register(@RequestBody RegisterRequest request) {
        return AjaxResult.success(authService.register(
            request.username(),
            request.nickname(),
            request.password(),
            request.email(),
            request.phone(),
            request.code()
        ));
    }

    /**
     * 发送邮箱验证码。
     */
    @PostMapping("/code/email")
    public AjaxResult emailCode(@RequestBody CodeRequest request) {
        authService.sendEmailCode(request.target(), request.purpose());
        return AjaxResult.success();
    }

    /**
     * 发送短信验证码。
     */
    @PostMapping("/code/sms")
    public AjaxResult smsCode(@RequestBody CodeRequest request) {
        authService.sendSmsCode(request.target(), request.purpose());
        return AjaxResult.success();
    }

    /** 登录请求参数 */
    public record LoginRequest(@NotBlank String identifier, @NotBlank String password) {
    }

    /** 注册请求参数 */
    public record RegisterRequest(String username, String nickname, String password, String email, String phone, String code) {
    }

    /**
     * 验证码登录，支持邮箱或手机号 + 验证码登录。
     */
    @PostMapping("/login/code")
    public AjaxResult loginCode(@RequestBody LoginCodeRequest request) {
        return AjaxResult.success(authService.loginWithCode(request.identifier(), request.code()));
    }

    /**
     * 重设密码（忘记密码），通过验证码验证身份后设置新密码。
     */
    @PostMapping("/password/reset")
    public AjaxResult resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.identifier(), request.code(), request.newPassword());
        return AjaxResult.success();
    }

    /** 验证码请求参数 */
    public record CodeRequest(String target, String purpose) {
    }

    /** 验证码登录请求参数 */
    public record LoginCodeRequest(@NotBlank String identifier, @NotBlank String code) {
    }

    /** 重设密码请求参数 */
    public record ResetPasswordRequest(@NotBlank String identifier, @NotBlank String code, @NotBlank String newPassword) {
    }
}
