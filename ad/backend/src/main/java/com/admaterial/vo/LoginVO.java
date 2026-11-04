package com.admaterial.vo;

import com.admaterial.common.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录结果 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {
    private Long userId;
    private String username;
    private String realName;
    private UserRole role;
    private String roleLabel;

    public static LoginVO from(com.admaterial.entity.SysUser user) {
        return new LoginVO(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getRole(),
                user.getRole().getLabel()
        );
    }
}
