# 实现任务列表

- [x] 1. 项目基础设施搭建
  - [x] 1.1 在 service-business/pom.xml 中添加 Caffeine、Redis、RabbitMQ、Sentinel 依赖
  - [x] 1.2 更新 application.yml 配置（Redis、RabbitMQ、HikariCP、Sentinel）
- [x] 2. 多级缓存模块
  - [x] 2.1 创建 MultiLevelCache 接口和 MultiLevelCacheManager 实现类
  - [x] 2.2 创建 CacheKeys 缓存键工具类
  - [x] 2.3 创建 Caffeine + Redis 缓存配置类
- [x] 3. RabbitMQ 消息队列模块
  - [x] 3.1 创建 OrderMessage 消息体和 RabbitMQ 配置类（Exchange、Queue、Binding、DLX）
  - [x] 3.2 创建 OrderMessageProducer 生产者实现
  - [x] 3.3 创建 OrderMessageConsumer 消费者实现（含幂等处理）
- [x] 4. 读写分离模块
  - [x] 4.1 创建 @ReadOnly 注解、DataSourceType 枚举、DynamicDataSource 路由类
  - [x] 4.2 创建 DataSourceAspect 切面和 DataSourceConfig 配置类
- [x] 5. 业务集成
  - [x] 5.1 改造 OrderService 集成多级缓存和 @ReadOnly 注解
  - [x] 5.2 改造 OrderController 添加异步创建订单接口和 Sentinel 限流注解
