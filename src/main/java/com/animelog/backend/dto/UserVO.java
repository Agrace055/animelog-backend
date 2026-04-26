package com.animelog.backend.dto;

import com.animelog.backend.domain.UserAccount;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息视图对象，屏蔽 passwordHash、isDeleted 等敏感/内部字段。
 */
@Data
public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String role;
    private String email;
    private String phone;
    private String nsfwStatus;
    private LocalDateTime createdAt;

    /** 从 UserAccount 实体转换，仅暴露前端所需字段。 */
    public static UserVO from(UserAccount user) {
        if (user == null) return null;
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setRole(user.getRole());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setNsfwStatus(user.getNsfwStatus());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
