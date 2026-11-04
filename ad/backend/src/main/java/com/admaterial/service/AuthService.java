package com.admaterial.service;

import com.admaterial.common.BusinessException;
import com.admaterial.dto.LoginDTO;
import com.admaterial.entity.SysUser;
import com.admaterial.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 登录认证 Service
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserService userService;

    /**
     * 登录验证
     * —— 课程 demo 阶段密码比对用明文；生产必须 BCrypt
     */
    public LoginVO login(LoginDTO dto) {
        SysUser user = userService.getByUsername(dto.getUsername());
        if (user == null) {
            throw new BusinessException(401, "账号不存在");
        }
        if (!dto.getPassword().equals(user.getPassword())) {
            throw new BusinessException(401, "密码错误");
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException(403, "账号已停用");
        }
        return LoginVO.from(user);
    }
}
