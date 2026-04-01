# Yihen-Drama（AI 短剧生成平台）

![系统首页](./assets/image-20260224191748119.png)

Yihen-Drama 是一个面向短剧创作的全流程平台，覆盖：

**文本输入 → 信息提取 → 角色/场景资产生成 → 分镜管理 → 视频生成 → 视频编辑**

仓库包含：
- 后端：`yihen-drama`（Spring Boot）
- 前端：`yihen-ai-short-drama-front-end/frontend`（Vue 3 + Vite）
- 容器编排：支持**本地开发模式**与**一键全容器部署模式**

---

## 目录

- [1. 快速开始](#1-快速开始)
- [2. 支持的功能（完整）](#2-支持的功能完整)
- [3. 使用流程](#3-使用流程)
- [4. 运行模式](#4-运行模式)
- [5. 访问地址](#5-访问地址)
- [6. 数据与中间件说明](#6-数据与中间件说明)
- [7. 配置说明](#7-配置说明)
- [8. 常见问题](#8-常见问题)
- [9. 子模块文档](#9-子模块文档)

---

## 1. 快速开始

### 1.1 先做这件事：配置模型 API Key

在“模型管理”中为默认模型实例填写可用 API Key（至少文本、图像、视频模型）。

![模型管理](./assets/image-20260224191924589.png)
![填写默认模型](./assets/image-20260224192126991.png)

---

## 2. 支持的功能（完整）

> 以下为当前系统已支持的核心能力清单。

### 2.1 项目管理

- 创建/编辑/删除项目
- 项目列表分页（支持页码导航）
- 项目关键词搜索（Elasticsearch）
- 项目搜索补全建议（suggest）
- 项目状态筛选（草稿/处理中/已完成）
- 项目封面自动占位图

### 2.2 章节管理

- 创建章节（支持空内容创建）
- 更新章节（名称、内容等）
- 删除章节（主题风格确认弹窗）
- 章节步骤状态维护（current_step）
- 步骤条可点击跳转（受步骤进度约束）

### 2.3 信息提取

- 从章节文本提取角色与场景
- 支持“开始提取/重新提取”
- 提取结果回写章节资产
- 支持手动选择文本模型（默认自动选中默认模型）
- 结果页实时展示人数/场景数

![信息提取](./assets/image-20260224192636477.png)

### 2.4 生成图片（角色与场景）

- 角色图生成、重生、下载、删除、上传替换
- 场景图生成、重生、下载、删除、上传替换
- 名称与描述可编辑，支持防抖更新
- 新增角色/新增场景（抽屉表单）
- 角色/场景卡片统一主题样式
- 无图时自动首字占位图
- 图片支持单击放大预览
- 操作提示统一消息风格（成功/失败/警告）

![资产生成](./assets/image-20260224193009811.png)

### 2.5 资产管理（独立模块）

- 按项目统一管理角色和场景
- 角色/场景切换 Tab
- 角色/场景增删改查
- 按项目内搜索（Elasticsearch）
- 搜索补全建议（suggest）
- 卡片固定尺寸，超长内容滚动显示
- 每页最多 8 条，超出后分页
- 统一主题弹窗与消息提示

![资产管理](./assets/image-20260224193210593.png)

### 2.6 分镜管理

- 生成分镜（按章节）
- 生成分镜时可选“使用向量检索”（`usedVector`，默认关闭）
- 分镜描述可编辑并保存
- 删除单条分镜（确认弹窗）
- 分镜关联角色（可搜索，可多选，限制最多 3 个）
- 分镜关联场景（可搜索，限制必须且仅 1 个）
- 关联结果保存后在后续步骤保持一致
- 分镜列表与镜头配置联动展示

![分镜管理](./assets/image-20260224193400719.png)
![角色选择弹窗](./assets/image-20260224193549438.png)

### 2.7 视频生成（分镜级）

- 生成首帧提示词（可编辑、可保存）
- 生成首帧图片（thumbnail 回显）
- 生成分镜视频提示词
- 生成分镜视频任务提交
- 时长参数可调，并以数字传入 `params.duration`
- 支持默认视频模型与手动切换视频模型
- 视频生成成功后自动切换预览
- 任务状态通过 WebSocket 回传
- 失败状态展示后端错误信息（`errorMessage`）

![视频生成](./assets/image-20260224193646820.png)

### 2.8 模型管理

- 厂商管理（分页）
- 模型实例管理（按类型分页）
- 文本/文生图/视频/语音/向量多类型管理
- 设置默认实例（按类型）
- 默认实例通过后端默认接口回显（不依赖前端临时字段）
- 新增/更新实例参数映射（如 `max_token`）
- 删除模型失败时展示后端原始错误信息（如“默认模型不可删除”）

![模型管理](./assets/image-20260224193838368.png)

### 2.9 提示词管理

- 多场景提示词模板管理
- 新增/编辑/删除提示词
- 设置默认提示词
- 场景枚举对齐后端 `SceneCode`
- 分镜/视频流程中调用与保存对应提示词

![提示词管理](./assets/image-20260224194051566.png)

### 2.10 实时通信与任务流转

- 进入项目后建立项目级 WebSocket 连接
- 后端轮询第三方任务状态并推送到前端
- 前端按 `targetId + taskType` 精准更新对应实体
- 支持多任务并发状态跟踪，按钮独立 loading

---

## 3. 使用流程

1. 创建项目与章节  
2. 在模型管理中确认默认模型与 API Key  
3. 提交章节文本，执行信息提取  
4. 在“生成图片”中完善角色/场景资产  
5. 在“生成分镜”中调整镜头描述及关联关系  
6. 在“生成视频”中生成提示词、首帧和分镜视频  
7. 在“视频编辑”中完成拼接与导出（持续迭代中）

---

## 4. 运行模式

### A. 本地开发模式（推荐）

前后端本机运行；中间件容器运行。

#### 1) 启动中间件
```powershell
.\infra-up.ps1
```
或
```bat
infra-up.bat
```
等价：
```bash
docker compose -f docker-compose.infra.yml up -d --build
```

#### 2) 启动后端
```bash
cd yihen-drama
mvn spring-boot:run
```

#### 3) 启动前端
```bash
cd yihen-ai-short-drama-front-end/frontend
npm install
npm run dev
```

说明：
- 前端已配置开发代理：`/api`、`/webSocket` 自动转发到 `localhost:8080`
- 不需要手工改前端请求端口

---

### B. 一键全容器模式（部署/演示）

```powershell
.\deploy.ps1
```
或
```bat
deploy.bat
```
等价：
```bash
docker compose -f docker-compose.full.yml up -d --build
```

---

## 5. 访问地址

- 前端：`http://localhost:3000`
- 后端：`http://localhost:8080`
- API 文档：`http://localhost:8080/doc.html`
- MinIO Console：`http://localhost:9001`
- RabbitMQ Console：`http://localhost:15672`
- Elasticsearch：`http://localhost:9200`
- Kibana：`http://localhost:5601`
- Qdrant HTTP：`http://localhost:6333`
- Qdrant gRPC：`localhost:6334`

---

## 6. 数据与中间件说明

### 6.1 MySQL 初始化

- 初始化脚本：`yihen-drama/sql/init_schema.sql`
- MySQL 数据卷为空时自动执行
- 该脚本已包含当前默认模型、ComfyUI 工作流路径和默认项，适用于全新环境初始化
- 如果是已有旧库升级，还需要手动执行：`yihen-drama/sql/patches/2026-04-01_sync_runtime_model_seed_data.sql`
- 当前 ComfyUI 工作流依赖本地目录：`D:\comfui\workflows\baseimage` 和 `D:\comfui\workflows\basevideo`
- Git 中提交的第三方模型 `api_key` 为占位值或空串，启动后需要在设置页补充真实密钥
- 若要重置：
```bash
docker compose -f docker-compose.full.yml down -v
docker compose -f docker-compose.full.yml up -d --build
```

### 6.2 Elasticsearch 中文插件

ES 镜像已集成 IK + pinyin 插件（构建自动安装）。

验证：
```bash
docker compose -f docker-compose.full.yml exec es elasticsearch-plugin list
```

### 6.3 Qdrant 向量检索

- 已接入 Qdrant 作为向量数据库（compose 已包含 `qdrant` 服务）
- 章节内容在提取流程后会进入向量化入库链路（异步消息）
- 分镜生成可通过前端开关决定是否使用向量检索增强上下文

---

## 7. 配置说明

后端配置文件：`yihen-drama/src/main/resources/application.yml`  
已支持“环境变量优先 + 本地默认值回退”。

核心变量：
- `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`
- `SPRING_DATA_REDIS_HOST/PORT/PASSWORD`
- `SPRING_RABBITMQ_HOST/PORT/USERNAME/PASSWORD`
- `SPRING_ELASTICSEARCH_URIS`
- `MINIO_END_POINT/MINIO_ACCESS_KEY/MINIO_SECRET_KEY`
- `QDRANT_HOST/QDRANT_PORT`
- `QDRANT_COLLECTION_NAME/QDRANT_DIMENSION`
- `QDRANT_MAX_SEGMENTSIZE_IN_TOKENS/QDRANT_MAX_OVERLAP_SIZE_IN_TOKENS`
- `QDRANT_MAX_RESULT/QDRANT_MIN_SCORE`

---

## 8. 常见问题

### Q1：后端报 Redis 连接失败
- 检查 `docker compose -f docker-compose.infra.yml ps`
- 确认 `redis` 服务健康后再启动后端

### Q2：前端看到请求是 3000，不是 8080
- 本地开发下这是 Vite 代理行为，属于正常
- 实际后端仍在 `8080`

### Q3：Docker 拉镜像超时
- 可切换镜像源
- 建议先手工 `docker pull` 关键镜像再构建

---

## 9. 子模块文档

- 后端：`yihen-drama/README.md`
- 前端：`yihen-ai-short-drama-front-end/frontend/README.md`

