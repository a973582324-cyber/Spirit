package com.admaterial.vo;

import com.admaterial.entity.DesignVersion;
import com.admaterial.entity.MaterialTask;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 物料任务详情聚合 VO
 * —— 设计人员/客户经理查看任务时需要：任务主信息 + 全部历史版本 + 全部客户意见
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskDetailVO {
    private MaterialTask task;
    private String orderNo;
    private String orderStatusLabel;
    private String assigneeName;
    private List<DesignVersion> versions;       // 所有历史版本，按 versionNo 升序
    private List<FeedbackVO> feedbacks;         // 所有客户修改意见
}
