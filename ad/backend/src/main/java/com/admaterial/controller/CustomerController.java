package com.admaterial.controller;

import com.admaterial.common.Result;
import com.admaterial.dto.CreateCustomerDTO;
import com.admaterial.service.CustomerService;
import com.admaterial.vo.CustomerVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 客户管理 Controller（客户经理用）
 */
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    /** 新建客户 */
    @PostMapping
    public Result<CustomerVO> create(@Valid @RequestBody CreateCustomerDTO dto) {
        return Result.ok(customerService.create(dto));
    }

    /** 查客户详情 */
    @GetMapping("/{id}")
    public Result<CustomerVO> getById(@PathVariable Long id) {
        return Result.ok(CustomerVO.from(customerService.getById(id)));
    }
}
