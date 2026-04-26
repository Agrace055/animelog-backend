package com.animelog.backend.config;

import com.animelog.backend.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器，从请求头中提取 Bearer Token 并构建 Spring Security 认证上下文。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;

    public JwtAuthenticationFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        // 从 Authorization 请求头中提取 Bearer Token
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                // 解析 JWT 获取用户主体信息
                JwtUtils.JwtPrincipal principal = jwtUtils.parse(header.substring(7));
                // 构建 Spring Security 认证令牌
                var auth = new UsernamePasswordAuthenticationToken(
                    principal.userId(),
                    null,
                    List.of(new SimpleGrantedAuthority(principal.role()))
                );
                // 设置认证上下文
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) {
                // Token 无效或过期时清除认证上下文
                SecurityContextHolder.clearContext();
            }
        }
        // 继续执行过滤器链
        filterChain.doFilter(request, response);
    }
}
