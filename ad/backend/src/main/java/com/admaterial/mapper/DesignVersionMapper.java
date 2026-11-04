package com.admaterial.mapper;

import com.admaterial.entity.DesignVersion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DesignVersionMapper extends BaseMapper<DesignVersion> {

    /**
     * 获取某任务当前最大的 version_no
     * —— 用于上传新版本时计算 versionNo = max + 1
     */
    @Select("SELECT COALESCE(MAX(version_no), 0) FROM design_version WHERE task_id = #{taskId}")
    int findMaxVersionNo(@Param("taskId") Long taskId);
}
