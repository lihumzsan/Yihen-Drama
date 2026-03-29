# ComfyUI 适配第一版方案

## 1. 目标

第一版只打通项目当前已经真实用到的 4 条链路：

1. 角色图生成 `CHARACTER_GEN`
2. 场景图生成 `SCENE_GEN`
3. 分镜首帧图生成 `FIRST_FRAME_GEN`
4. 分镜视频生成 `VIDEO_GEN`

本版不改文本模型链路。文本模型继续负责生成提示词，ComfyUI 负责图片和视频执行。

## 2. 项目现状

当前项目的生成链路如下：

- 角色图、场景图：纯文生图
- 首帧图：提示词 + 场景图 + 角色图参考
- 分镜视频：首帧图 + 视频提示词
- 视频任务：已有异步轮询与任务表

当前模型管理仍然按“远程厂商 API”设计，不适合直接承载 ComfyUI 工作流执行。

## 3. 第一版范围

### 3.1 接入方式

第一版采用“ComfyUI 作为本地工作流执行器”的方式接入：

- `TEXT` 模型：保持现状
- `IMAGE` 模型：新增 `ComfyUiImageModelStrategy`
- `VIDEO` 模型：新增 `ComfyUiVideoModelStrategy`

### 3.2 不在第一版范围内

以下能力先不纳入第一版主链路：

- 局部重绘 / 蒙版编辑
- 首尾帧视频 UI
- 音频驱动视频
- 图片反推提示词工具化入口
- 多工作流自动编排

## 4. 工作流映射

### 4.1 图片工作流

#### A. 角色图 / 场景图

推荐主工作流：

- `D:\comfui\workflows\baseimage\01_scene_txt2img\P0_ZImage光速高清文生图_ZImageTurbo\场景文生图-ZImage光速高清文生图-RunningHub原始工作流.json`

备选：

- `D:\comfui\workflows\baseimage\01_scene_txt2img\P1_造相ZImageTurbo官方版_ZImageTurbo\场景文生图-造相ZImageTurbo官方版-RunningHub原始工作流.json`
- `D:\comfui\workflows\baseimage\01_scene_txt2img\P2_Flux2Klein4B9B文生图_Flux2Klein\场景文生图-Flux2Klein4B9B文生图-RunningHub原始工作流.json`

适配项目场景：

- `CHARACTER_GEN`
- `SCENE_GEN`

#### B. 分镜首帧图

推荐主工作流：

- `D:\comfui\workflows\baseimage\04_reference_edit\P2_5图编辑创作Flux2多图编辑_Flux2\参考编辑-5图编辑创作Flux2多图编辑-RunningHub原始工作流.json`

单图回退：

- `D:\comfui\workflows\baseimage\04_reference_edit\P0_QwenImageEdit2511图像编辑_QwenImageEdit2511\参考编辑-QwenImageEdit2511图像编辑-RunningHub原始工作流.json`

适配项目场景：

- `FIRST_FRAME_GEN`

说明：

- 当前项目首帧逻辑会把分镜关联的场景图和角色图全部作为参考输入
- 第一版建议做“最多 5 张参考图”的裁剪规则
- 规则建议：优先场景图，再按出场顺序取角色图

### 4.2 视频工作流

#### A. 分镜视频 / 角色视频

推荐主工作流：

- `D:\comfui\workflows\basevideo\01_图生视频\P0_LTX23主线图生视频_优化版蒸馏Dev可切换\图生视频-LTX2.3图生视频优化版蒸馏Dev可切换-无ReservedVRAM本地可跑版.json`

备选：

- `D:\comfui\workflows\basevideo\01_图生视频\P1_Wan22Remix次选图生视频_Wan2.2RemixI2V\图生视频-Wan2.2Remix图生视频I2V-RunningHub原始工作流.json`

适配项目场景：

- `VIDEO_GEN`
- 角色视频生成
- 分镜图生视频生成

#### B. 首尾帧视频

候选工作流：

- `D:\comfui\workflows\basevideo\01_图生视频\ltx2.3首尾帧.json`
- `D:\comfui\workflows\basevideo\01_图生视频\三图首尾帧自定义文案视频生成-LTX2.3-电商、广告、口播.json`

说明：

- 第一版先不接 UI
- 作为第二版扩展能力

### 4.3 工具工作流

图片反推提示词：

- `D:\comfui\workflows\tool\Qwen3.5最强反推 快速反推 可破限 26-03-20.json`

说明：

- 第一版不混入默认模型
- 建议作为“工具类工作流”独立入口

## 5. 模型配置方案

第一版建议保留现有表结构主干，但把 ComfyUI 所需配置放入 `model_instance.params`。

### 5.1 Provider

新增一个 provider：

- `provider_code = comfyui`
- `base_url = http://127.0.0.1:8188`

### 5.2 ModelInstance 约定

`model_instance` 仍保留：

- `model_def_id`
- `model_type`
- `instance_name`
- `scene_code`
- `path`
- `params`

约定如下：

- `path`：固定填 `/prompt`
- `api_key`：允许为空
- `model_code`：不再表示云模型名，可作为工作流代码，如 `comfyui-txt2img-zimage`
- `params`：存工作流元信息

### 5.3 第一版 params 建议格式

```json
{
  "workflowMode": "txt2img",
  "workflowPath": "D:\\comfui\\workflows\\baseimage\\01_scene_txt2img\\P0_ZImage光速高清文生图_ZImageTurbo\\场景文生图-ZImage光速高清文生图-RunningHub原始工作流.json",
  "apiWorkflowPath": "D:\\comfui\\workflows\\api\\image\\character_scene_zimage.json",
  "inputMapping": {
    "prompt": "13.value",
    "width": "16.value",
    "height": "17.value"
  },
  "outputMapping": {
    "type": "image",
    "nodeId": "9"
  },
  "defaults": {
    "width": 1920,
    "height": 1080
  }
}
```

视频实例示例：

```json
{
  "workflowMode": "i2v",
  "workflowPath": "D:\\comfui\\workflows\\basevideo\\01_图生视频\\P0_LTX23主线图生视频_优化版蒸馏Dev可切换\\图生视频-LTX2.3图生视频优化版蒸馏Dev可切换-无ReservedVRAM本地可跑版.json",
  "apiWorkflowPath": "D:\\comfui\\workflows\\api\\video\\ltx23_i2v.json",
  "inputMapping": {
    "image": "start_frame",
    "prompt": "prompt",
    "duration": "duration"
  },
  "outputMapping": {
    "type": "video",
    "nodeId": "save_video"
  },
  "defaults": {
    "duration": 5,
    "fps": 24
  }
}
```

## 6. 后端改造点

### 6.1 新增 ComfyUI 客户端

新增：

- `ComfyUiClient`

至少支持：

- `POST /prompt`
- `GET /history/{prompt_id}`
- `GET /view`
- `POST /upload/image`

### 6.2 新增策略类

新增：

- `ComfyUiImageModelStrategy`
- `ComfyUiVideoModelStrategy`

职责：

- 读取 `model_instance.params`
- 加载 API format 工作流 JSON
- 按 `inputMapping` 注入 prompt / image / duration 等参数
- 提交给 ComfyUI
- 按 `outputMapping` 提取结果

### 6.3 修改工厂选择逻辑

当前工厂按 provider baseUrl 判断 volcano。

第一版改法：

- `provider_code = comfyui` 时走 ComfyUI 策略
- 其他 provider 保持现状

### 6.4 修改模型实例校验

当前问题：

- `apiKey` 被强制要求非空

第一版改法：

- 当 provider 是 `comfyui` 时允许 `api_key` 为空
- 测试连接时不检查 Bearer Token

### 6.5 修改 HTTP 执行层

当前问题：

- 所有请求都带 `Authorization: Bearer xxx`

第一版改法：

- 新增无鉴权调用
- 或者在 `apiKey` 为空时不加 `Authorization`

### 6.6 图片结果持久化兼容

当前问题：

- ComfyUI `/view` URL 往往靠 query 参数带文件名
- 现有扩展名提取逻辑只看 path

第一版改法：

- `UrlUtils.extractFileExtension` 支持从 `filename` query 中提取后缀
- 否则按响应 content-type 推断

## 7. 前端改造点

### 7.1 设置页

第一版要新增以下配置项：

- 工作流模式 `workflowMode`
- 工作流文件路径 `workflowPath`
- API 工作流文件路径 `apiWorkflowPath`
- 输入映射 `inputMapping`
- 输出映射 `outputMapping`

现有设置页只够填：

- Provider
- Base URL
- Path
- API Key
- model
- 普通参数

这还不够支撑 ComfyUI。

### 7.2 默认模型选择

当前默认模型只按 `TEXT / IMAGE / VIDEO / AUDIO` 分类型。

第一版建议改为“按场景选默认实例”：

- `CHARACTER_GEN`
- `SCENE_GEN`
- `FIRST_FRAME_GEN`
- `VIDEO_GEN`

否则会出现：

- 角色图和首帧图共用同一个 `IMAGE` 默认实例
- 但二者实际需要不同工作流

## 8. 任务机制

### 8.1 图片任务

第一版建议：

- 图片工作流先同步返回
- 服务端提交后短轮询 `history`
- 拿到首张结果图后直接返回

适合：

- 角色图
- 场景图
- 首帧图

### 8.2 视频任务

视频沿用现有 `video_task` 表与动态轮询器。

映射规则：

- `task_id` 存 ComfyUI `prompt_id`
- `status` 映射 `QUEUED / RUNNING / SUCCEEDED / FAILED`
- 成功后拿 `/view` 或输出文件地址，再落 MinIO

## 9. 第一版建议实例

建议至少创建 4 个 ComfyUI 模型实例：

1. `ComfyUI-角色图-ZImage`
   - `model_type = IMAGE`
   - `scene_code = CHARACTER_GEN`
   - `workflowMode = txt2img`

2. `ComfyUI-场景图-ZImage`
   - `model_type = IMAGE`
   - `scene_code = SCENE_GEN`
   - `workflowMode = txt2img`

3. `ComfyUI-首帧图-Flux2多图参考`
   - `model_type = IMAGE`
   - `scene_code = FIRST_FRAME_GEN`
   - `workflowMode = multi_ref_edit`

4. `ComfyUI-视频-LTX23-I2V`
   - `model_type = VIDEO`
   - `scene_code = VIDEO_GEN`
   - `workflowMode = i2v`

## 10. 当前缺失项

### 10.1 工作流侧缺失

以下目录存在，但当前为空：

- `D:\comfui\workflows\basevideo\02_首尾帧连续镜头\P0_LTX23首尾帧与关键帧_图三图首尾帧`

这意味着：

- 你规划中的“规范化首尾帧目录”当前没有成品文件
- 现在只能先用 `01_图生视频` 下的首尾帧工作流顶上

### 10.2 项目侧缺失

当前项目还没有专门承载这些能力的数据结构：

- 多图参考输入上限控制
- 图片编辑类型区分
- 首尾帧双输入
- 工具工作流入口
- 工作流节点映射配置

## 11. 实施顺序

建议按这个顺序落地：

1. 新增 `comfyui` provider
2. 放通无 `apiKey` 的模型实例校验
3. 新增 `ComfyUiClient`
4. 新增 `ComfyUiImageModelStrategy`
5. 打通 `CHARACTER_GEN / SCENE_GEN / FIRST_FRAME_GEN`
6. 新增 `ComfyUiVideoModelStrategy`
7. 打通 `VIDEO_GEN`
8. 再改前端设置页，补工作流配置能力
9. 最后做首尾帧视频、图片反推、音频驱动等扩展

## 12. 第一版结论

第一版最稳的做法是：

- 文本提示词仍由现有文本模型生成
- 图片和视频执行改为走 ComfyUI
- 默认模型从“按类型”升级为“按场景”
- 使用 4 个独立 ComfyUI 实例覆盖当前项目主链路

这样改动范围可控，且能最大限度复用你现有的 ComfyUI 工作流资产。
