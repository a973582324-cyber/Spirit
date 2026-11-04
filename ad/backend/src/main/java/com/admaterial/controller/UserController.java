package com.admaterial.controller;

import com.admaterial.common.Result;
import com.admaterial.common.enums.UserRole;
import com.admaterial.entity.SysUser;
import com.admaterial.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户查询 Controller
 * —— 给负责人分派设计人员下拉框用
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService userService;

    /**
     * 按角色查可用用户（设计人员下拉框）
     * GET /api/users?role=DESIGNER
     */
    @GetMapping
    public Result<List<SysUser>> listByRole(@RequestParam UserRole role) {
        List<SysUser> users = userService.listByRole(role);
        // 脱敏：不返回 password
        users.forEach(u -> u.setPassword(null));
        return Result.ok(users);
    }
}
