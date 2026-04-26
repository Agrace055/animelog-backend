package com.animelog.backend.domain;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户账号实体，映射 user_account 表。
 */
@Data
@TableName("user_account")
public class UserAccount {
    private Long id;                        // 主键 ID
    private String username;                 // 用户名
    private String passwordHash;             // 密码哈希值（BCrypt 加密）
    private String nickname;                 // 昵称/显示名
    private String avatarUrl;                // 头像 URL
    private String role;                     // 角色：user（普通用户）、admin（管理员）
    private String email;                    // 邮箱
    private String phone;                    // 手机号
    private String nsfwStatus;               // NSFW 访问权限状态：none（未申请）、pending（审核中）、approved（已批准）
    private LocalDateTime createdAt;         // 创建时间
    private LocalDateTime updatedAt;         // 更新时间
    @TableLogic
    private Integer isDeleted;               // 逻辑删除标志（0=正常，1=已删除）
}
