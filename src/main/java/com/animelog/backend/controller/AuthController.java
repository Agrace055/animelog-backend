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

    /** 验证码请求参数 */
    public record CodeRequest(String target, String purpose) {
    }
}
