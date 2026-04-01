# yihen-drama（后端服务）

## 技术栈
- Java 17
- Spring Boot 3
- MyBatis-Plus
- MySQL 8 / Redis / RabbitMQ
- Elasticsearch 8 + IK/pinyin
- MinIO

## 本地运行

```bash
cd yihen-drama
mvn spring-boot:run
```

默认端口：`8080`

## 依赖服务

建议使用根目录编排启动中间件：

```bash
docker compose -f ../docker-compose.infra.yml up -d --build
```

## 关键配置

配置文件：`src/main/resources/application.yml`  
支持环境变量覆盖，核心项：
- `SPRING_DATASOURCE_*`
- `SPRING_DATA_REDIS_*`
- `SPRING_RABBITMQ_*`
- `SPRING_ELASTICSEARCH_URIS`
- `MINIO_*`

## API 文档

启动后访问：
- `http://localhost:8080/doc.html`

## 初始化 SQL

- 文件：`sql/init_schema.sql`
- Docker MySQL 首次启动时自动执行
- 该脚本已包含当前默认模型、ComfyUI 工作流路径和默认项，适用于全新环境初始化
- 如果是已有旧库升级，还需要手动执行：`sql/patches/2026-04-01_sync_runtime_model_seed_data.sql`
- 当前 ComfyUI 工作流依赖本地目录：`D:\comfui\workflows\baseimage` 和 `D:\comfui\workflows\basevideo`
- Git 中提交的第三方模型 `api_key` 为占位值或空串，启动后需要在设置页补充真实密钥

