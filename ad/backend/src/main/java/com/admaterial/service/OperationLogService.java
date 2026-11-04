package com.admaterial.service;

import com.admaterial.common.enums.BusinessType;
import com.admaterial.common.enums.OperationType;
import com.admaterial.entity.OperationHistory;
import com.admaterial.mapper.OperationHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 操作历史记录 Service
 * —— 所有需要审计的动作统一走这里
 */
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationHistoryMapper historyMapper;

    /**
     * 记一笔操作
     *
     * @param businessType 业务类型（ORDER / TASK / ...）
     * @param businessId   业务对象 id
     * @param operationType 操作类型（CREATE / CONFIRM / ASSIGN / ...）
     * @param desc         人类可读的操作描述
     * @param operatorId   操作人
     * @param operatorName 操作人姓名
     */
    public void log(BusinessType businessType, Long businessId,
                    OperationType operationType, String desc,
                    Long operatorId, String operatorName) {
        OperationHistory h = new OperationHistory();
        h.setBusinessType(businessType);
        h.setBusinessId(businessId);
        h.setOperationType(operationType);
        h.setOperationDesc(desc);
        h.setOperatorId(operatorId);
        h.setOperatorName(operatorName);
        historyMapper.insert(h);
    }
}
