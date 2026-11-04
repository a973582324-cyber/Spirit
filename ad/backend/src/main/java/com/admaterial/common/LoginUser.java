package com.admaterial.common;

import com.admaterial.common.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户上下文（ThreadLocal 持有）
 * —— Controller 层随时能拿到 userId / role / realName
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {
    private Long userId;
    private String username;
    private String realName;
    private UserRole role;
}
