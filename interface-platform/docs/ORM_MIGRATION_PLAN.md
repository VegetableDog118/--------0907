# ORM框架统一迁移计划

## 第一阶段：现状分析结果

### XML映射文件分布
1. **interface-service**:
   - InterfaceMapper.xml (238行) - 核心业务映射
   - InterfaceCategoryMapper.xml
   - InterfaceParameterMapper.xml

2. **approval-service**:
   - SubscriptionApplicationMapper.xml (66行)
   - UserInterfaceSubscriptionMapper.xml

### 关键问题识别
1. **参数绑定冲突**: XML中使用#{paramName}，MyBatis-Plus使用@Param注解
2. **复杂查询依赖**: 多表关联查询、动态条件查询、统计查询
3. **混合使用模式**: 基础CRUD用MyBatis-Plus，复杂查询用XML

## 第二阶段：迁移优先级

### 高优先级（立即处理）
1. **InterfaceMapper** - 已知问题源头
   - selectInterfacePage (分页查询，多表关联)
   - selectInterfaceById (关联查询)
   - batchUpdateStatus (批量更新)
   - selectInterfaceStatistics (统计查询)

2. **上架下架功能相关**
   - 已使用JdbcTemplate临时修复
   - 需要迁移到MyBatis-Plus标准方式

### 中优先级
1. **SubscriptionApplicationMapper**
   - selectApplicationPage (分页查询)
   - selectApplicationStatistics (统计查询)
   - batchUpdateStatus (批量更新)

### 低优先级
1. **其他Mapper的简单查询**
   - 可以逐步迁移
   - 不影响核心业务

## 第三阶段：迁移策略

### 1. 复杂查询迁移方案
- **多表关联**: 使用MyBatis-Plus的left join或手动构建SQL
- **动态条件**: 使用QueryWrapper和LambdaQueryWrapper
- **分页查询**: 使用IPage<T>和Page<T>
- **统计查询**: 使用selectCount、selectMaps等方法

### 2. 参数绑定统一
- 所有Mapper方法参数使用@Param注解
- 移除XML中的parameterType声明
- 统一使用MyBatis-Plus的参数绑定方式

### 3. 渐进式迁移
- 保持原有XML文件作为备份
- 逐个方法迁移并测试
- 确保功能完全正常后删除XML映射

## 第四阶段：风险控制

### 1. 回滚方案
- 保留原有XML文件
- Git分支管理
- 数据库备份

### 2. 测试策略
- 单元测试覆盖所有迁移方法
- 集成测试验证业务流程
- 性能测试确保无回退

### 3. 监控告警
- 数据库操作异常监控
- 业务功能可用性监控
- 性能指标监控

## 第五阶段：实施计划

### Day 1: InterfaceMapper核心方法迁移
- selectInterfacePage
- selectInterfaceById
- batchUpdateStatus

### Day 2: 上架下架功能优化
- 将JdbcTemplate方式改为MyBatis-Plus
- 完整测试上架下架流程

### Day 3: 统计查询迁移
- selectInterfaceStatistics
- 其他统计相关查询

### Day 4: approval-service迁移
- SubscriptionApplicationMapper迁移
- 业务流程测试

### Day 5: 配置清理和最终测试
- 移除XML配置
- 全面回归测试
- 性能验证

## 成功标准
1. 所有业务功能正常工作
2. 上架下架功能完全稳定
3. 无参数绑定错误
4. 性能无明显下降
5. 代码可维护性提升