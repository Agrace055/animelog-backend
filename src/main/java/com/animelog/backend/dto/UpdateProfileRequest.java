package com.animelog.backend.dto;

import lombok.Data;

/**
 * 用户信息更新请求，所有字段均为可选（null 则不更新）。
 */
@Data
public class UpdateProfileRequest {
    private String nickname;
    private String email;
    private String phone;
    private String avatarUrl;
}
