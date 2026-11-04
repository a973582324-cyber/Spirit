package com.admaterial.controller;

import com.admaterial.common.Result;
import com.admaterial.dto.LoginDTO;
import com.admaterial.service.AuthService;
import com.admaterial.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 登录认证 Controller
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 登录
     * —— 前端拿到 userId 后，后续请求 Header 带 X-User-Id: xxx 即可
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        LoginVO vo = authService.login(dto);
        return Result.ok(vo);
    }
}
