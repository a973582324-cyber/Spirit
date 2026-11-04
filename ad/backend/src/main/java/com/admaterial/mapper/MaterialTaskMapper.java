package com.admaterial.mapper;

import com.admaterial.common.enums.TaskStatus;
import com.admaterial.entity.MaterialTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MaterialTaskMapper extends BaseMapper<MaterialTask> {

    /**
     * 统计某订单下还有多少任务没到 CLIENT_CONFIRMED 或 CANCELLED
     * —— 用于"订单是否可以进入 PENDING_DELIVERY"的前置检查
     */
    @Select("SELECT COUNT(*) FROM material_task " +
            "WHERE order_id = #{orderId} " +
            "AND task_status NOT IN ('CLIENT_CONFIRMED', 'CANCELLED')")
    long countUnfinishedByOrderId(@Param("orderId") Long orderId);

    /**
     * 统计某订单下已 CLIENT_CONFIRMED 的任务数
     * —— 用于工作台展示 "2/3 已确认"
     */
    @Select("SELECT COUNT(*) FROM material_task " +
            "WHERE order_id = #{orderId} " +
            "AND task_status = 'CLIENT_CONFIRMED'")
    long countConfirmedByOrderId(@Param("orderId") Long orderId);

    /**
     * 获取某订单下的全部任务数
     */
    @Select("SELECT COUNT(*) FROM material_task WHERE order_id = #{orderId}")
    long countByOrderId(@Param("orderId") Long orderId);
}
