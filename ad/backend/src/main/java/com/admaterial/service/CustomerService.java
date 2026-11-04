package com.admaterial.service;

import com.admaterial.common.BusinessException;
import com.admaterial.common.UserContext;
import com.admaterial.dto.CreateCustomerDTO;
import com.admaterial.entity.Customer;
import com.admaterial.mapper.CustomerMapper;
import com.admaterial.vo.CustomerVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 客户 Service
 */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerMapper customerMapper;

    /** 创建客户 */
    public CustomerVO create(CreateCustomerDTO dto) {
        Customer customer = new Customer();
        customer.setCustomerName(dto.getCustomerName());
        customer.setContactPerson(dto.getContactPerson());
        customer.setContactPhone(dto.getContactPhone());
        customer.setContactEmail(dto.getContactEmail());
        customer.setAddress(dto.getAddress());
        customer.setNotes(dto.getNotes());
        customer.setCreateBy(UserContext.getUserId());
        customerMapper.insert(customer);
        return CustomerVO.from(customer);
    }

    /** 按 id 取客户 */
    public Customer getById(Long id) {
        Customer c = customerMapper.selectById(id);
        if (c == null) {
            throw BusinessException.notFound("客户不存在，id=" + id);
        }
        return c;
    }

    /**
     * 分页查询客户（支持 customerName 模糊搜索）
     * —— 给 OrderService.pageQuery 调用，也给前端客户管理页用
     */
    public Page<Customer> pageQuery(String customerName, int page, int size) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(customerName)) {
            wrapper.like(Customer::getCustomerName, customerName);
        }
        wrapper.orderByDesc(Customer::getCreateTime);
        return customerMapper.selectPage(Page.of(page, size), wrapper);
    }
}
