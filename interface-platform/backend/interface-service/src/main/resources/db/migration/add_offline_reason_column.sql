-- 添加offline_reason字段到interfaces表
ALTER TABLE interfaces ADD COLUMN offline_reason VARCHAR(500) COMMENT '下架原因';