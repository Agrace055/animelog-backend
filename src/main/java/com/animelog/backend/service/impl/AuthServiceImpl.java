package com.animelog.backend.service.impl;

import com.animelog.backend.domain.UserAccount;
import com.animelog.backend.mapper.UserAccountMapper;
import com.animelog.backend.service.AuthService;
import com.animelog.backend.utils.JwtUtils;
import com.animelog.backend.utils.VerificationCodeUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 认证业务实现。
 */
@Service
public class AuthServiceImpl implements AuthService {
    private final UserAccountMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final VerificationCodeUtils verificationCodeUtils;

    public AuthServiceImpl(
        UserAccountMapper userMapper,
        PasswordEncoder passwordEncoder,
        JwtUtils jwtUtils,
        VerificationCodeUtils verificationCodeUtils
    ) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.verificationCodeUtils = verificationCodeUtils;
    }

    @Override
    public Map<String, Object> login(String identifier, String password) {
        // 按用户名、邮筱或手机号查找未删除的用户（is_deleted=0 由 @TableLogic 自动附加）
        UserAccount user = userMapper.selectOne(new QueryWrapper<UserAccount>()
            .and(q -> q.eq("username", identifier).or().eq("email", identifier).or().eq("phone", identifier))
            .last("LIMIT 1"));
        // 校验密码
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("账号或密码不正确");
        }
        // 签发 JWT Token 并返回
        return Map.of("token", jwtUtils.issue(user.getId(), user.getRole()), "user", user);
    }

    @Override
    public UserAccount register(String username, String nickname, String password, String email, String phone, String code) {
        // 检查用户名、邮筱、手机号是否已存在（is_deleted=0 由 @TableLogic 自动附加）
        boolean exists = userMapper.selectCount(new QueryWrapper<UserAccount>()
            .and(q -> q.eq("username", username).or().eq("email", email).or().eq("phone", phone))) > 0;
        if (exists) {
            throw new IllegalArgumentException("用户名、邮箱或手机号已被使用");
        }
        // 构建新用户对象
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setNickname(nickname);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole("user");
        user.setAvatarUrl("https://picsum.photos/seed/" + username + "/100/100");
        user.setNsfwStatus("none");
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        return user;
    }

    @Override
    public void sendEmailCode(String target, String purpose) {
        verificationCodeUtils.sendEmailCode(target, purpose);
    }

    @Override
    public void sendSmsCode(String target, String purpose) {
        verificationCodeUtils.sendSmsCode(target, purpose);
    }
}
