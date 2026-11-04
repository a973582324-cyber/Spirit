package com.admaterial.service;

import com.admaterial.common.enums.UserRole;
import com.admaterial.entity.SysUser;
import com.admaterial.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户查询 Service（简单封装）
 */
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper userMapper;

    /**
     * 按角色查用户
     */
    public List<SysUser> listByRole(UserRole role) {
        return userMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRole, role)
                        .eq(SysUser::getEnabled, true)
                        .orderByAsc(SysUser::getId)
        );
    }

    /**
     * 按 id 查
     */
    public SysUser getById(Long id) {
        return userMapper.selectById(id);
    }

    /**
     * 按 username 查（登录用）
     */
    public SysUser getByUsername(String username) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );
    }
}
