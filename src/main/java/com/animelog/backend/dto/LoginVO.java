package com.animelog.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应视图对象，包含 JWT Token 和用户信息。
 */
@Data
@AllArgsConstructor
public class LoginVO {
    private String token;
    private UserVO user;
}
