package com.admaterial.controller;

import com.admaterial.common.Result;
import com.admaterial.common.enums.OrderStatus;
import com.admaterial.dto.CancelDTO;
import com.admaterial.dto.CreateChangeDTO;
import com.admaterial.dto.CreateOrderDTO;
import com.admaterial.dto.ReviewOrderDTO;
import com.admaterial.entity.MaterialOrder;
import com.admaterial.entity.OrderChangeRecord;
import com.admaterial.service.OrderService;
import com.admaterial.vo.OrderDetailVO;
import com.admaterial.vo.OrderListItemVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单 Controller
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ========================================================================
    // 创建 / 审核 / 查询
    // ========================================================================

    @PostMapping
    public Result<OrderDetailVO> create(@Valid @RequestBody CreateOrderDTO dto) {
        return Result.ok(orderService.create(dto));
    }

    @PostMapping("/{id}/confirm")
    public Result<OrderDetailVO> confirm(@PathVariable Long id,
                                         @RequestBody(required = false) ReviewOrderDTO dto) {
        return Result.ok(orderService.confirm(id, dto));
    }

    @GetMapping("/{id}")
    public Result<OrderDetailVO> getDetail(@PathVariable Long id) {
        return Result.ok(orderService.getDetail(id));
    }

    /**
     * 订单列表（完整查询能力）
     * GET /api/orders?customerName=&status=&deadlineFrom=&deadlineTo=&overdue=&page=&size=
     */
    @GetMapping
    public Result<Page<OrderListItemVO>> page(
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deadlineFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deadlineTo,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(orderService.pageQuery(customerName, status, deadlineFrom, deadlineTo, overdue, page, size));
    }

    // ========================================================================
    // 变更 / 完成 / 取消
    // ========================================================================

    @PostMapping("/{id}/changes")
    public Result<OrderChangeRecord> addChange(@PathVariable Long id,
                                               @Valid @RequestBody CreateChangeDTO dto) {
        return Result.ok(orderService.addChange(id, dto));
    }

    @GetMapping("/{id}/changes")
    public Result<List<OrderChangeRecord>> listChanges(@PathVariable Long id) {
        return Result.ok(orderService.listChangesByOrderId(id));
    }

    @PostMapping("/{id}/complete")
    public Result<MaterialOrder> complete(@PathVariable Long id) {
        return Result.ok(orderService.completeOrder(id));
    }

    @PostMapping("/{id}/cancel")
    public Result<MaterialOrder> cancel(@PathVariable Long id,
                                        @Valid @RequestBody CancelDTO dto) {
        return Result.ok(orderService.cancelOrder(id, dto.getReason()));
    }
}
