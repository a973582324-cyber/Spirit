package com.admaterial.mapper;

import com.admaterial.entity.MaterialOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MaterialOrderMapper extends BaseMapper<MaterialOrder> {

    /**
     * 获取某天已使用的最大订单序号
     * —— 用于生成下一个订单号：MO + yyyyMMdd + 3位序号
     */
    @Select("SELECT order_no FROM material_order " +
            "WHERE order_no LIKE CONCAT('MO', #{datePrefix}, '%') " +
            "ORDER BY order_no DESC LIMIT 1")
    String findMaxOrderNoOfDay(String datePrefix);
}
