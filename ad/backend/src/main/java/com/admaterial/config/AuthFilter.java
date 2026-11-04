package com.admaterial.config;

import com.admaterial.common.LoginUser;
import com.admaterial.common.UserContext;
import com.admaterial.common.enums.UserRole;
import com.admaterial.entity.SysUser;
import com.admaterial.mapper.SysUserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * 简单的鉴权过滤器
 * —— 前端通过 Header 传 X-User-Id，后端查库拿到用户信息放入 ThreadLocal
 * —— 登录接口放行；其他接口没带 X-User-Id 或 userId 无效 → 401
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthFilter extends OncePerRequestFilter {

    private final SysUserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();

        // 登录接口放行
        if (uri.contains("/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 尝试从 Header 拿 userId
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr == null || userIdStr.isBlank()) {
            write401(response, "未登录：缺少 X-User-Id Header");
            return;
        }

        Long userId;
        try {
            userId = Long.parseLong(userIdStr.trim());
        } catch (NumberFormatException e) {
            write401(response, "未登录：X-User-Id 格式错误");
            return;
        }

        SysUser sysUser = userMapper.selectById(userId);
        if (sysUser == null || !sysUser.getEnabled()) {
            write401(response, "未登录：用户不存在或已停用");
            return;
        }

        LoginUser loginUser = new LoginUser(
                sysUser.getId(),
                sysUser.getUsername(),
                sysUser.getRealName(),
                sysUser.getRole()
        );
        UserContext.set(loginUser);
        log.debug("[Auth] 用户: {} (id={}, role={})", sysUser.getRealName(), userId, sysUser.getRole());

        // 请求结束后清理 ThreadLocal
        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.remove();
        }
    }

    private void write401(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> body = Map.of(
                "code", 401,
                "message", msg,
                "errorKey", "unauthorized"
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
