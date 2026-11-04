package com.admaterial.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 全局配置
 * —— 分页插件 + 审计字段自动填充
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * 分页插件
     * Controller 层传 Page 对象就能自动分页
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 审计字段自动填充
     * —— Entity 里标注了 @TableField(fill = INSERT) 的字段会自动被填上
     * —— 课程 demo 阶段 createBy 等"谁创建的"需要在 Service 层手动设（因为要从 token 拿 userId）
     *    这里只负责自动填 createTime / updateTime 这种纯时间字段
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                // 自动填充所有 String 类型为 LocalDateTime 的 "createTime" / "uploadTime" / "recordTime" / "operateTime" / "changeTime"
                strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
                strictInsertFill(metaObject, "uploadTime", LocalDateTime.class, now);
                strictInsertFill(metaObject, "recordTime", LocalDateTime.class, now);
                strictInsertFill(metaObject, "operateTime", LocalDateTime.class, now);
                strictInsertFill(metaObject, "changeTime", LocalDateTime.class, now);
                strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
