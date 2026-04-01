# ComfyUI Baseimage Workflow Map

当前项目使用的是你本地这套目录分类：

- `D:\comfui\workflows\baseimage\图片生成`
- `D:\comfui\workflows\baseimage\图片编辑`
- `D:\comfui\workflows\baseimage\图片分镜`
- `D:\comfui\workflows\baseimage\图片视角切换`

前后端现在都会优先从工作流路径里解析分类，所以新环境只要恢复这套目录和 SQL，页面就能直接看出具体用了哪个工作流。

## 当前生效的图像工作流

- `CHARACTER_GEN`
  - 实例名: `ComfyUI 角色图生成`
  - 主工作流: `D:\comfui\workflows\baseimage\图片生成\ZImageTurbo造相.json`
  - 重生成工作流: `D:\comfui\workflows\baseimage\图片编辑\qwen单图编辑.json`
  - 展示分类: `图片生成 / ZImageTurbo造相`
  - 重生成分类: `图片编辑 / qwen单图编辑`
- `SCENE_GEN`
  - 实例名: `ComfyUI 场景图生成`
  - 主工作流: `D:\comfui\workflows\baseimage\图片生成\ZImageTurbo造相.json`
  - 展示分类: `图片生成 / ZImageTurbo造相`
- `FIRST_FRAME_GEN`
  - 实例名: `ComfyUI 首帧图生成`
  - 主工作流: `D:\comfui\workflows\baseimage\图片编辑\Flux2多图编辑.json`
  - 展示分类: `图片编辑 / Flux2多图编辑`

## 当前生效的视频工作流

- `VIDEO_GEN`
  - 实例名: `ComfyUI LTX2.3 I2V`
  - 主工作流: `D:\comfui\workflows\basevideo\图生视频\图生视频-LTX2.3图生视频优化版蒸馏Dev可切换-无ReservedVRAM本地可跑版.json`
  - 展示分类: `图生视频 / 图生视频-LTX2.3图生视频优化版蒸馏Dev可切换-无ReservedVRAM本地可跑版`

## 节点映射

- `图片生成 / ZImageTurbo造相`
  - 输出节点: `10`
- `图片编辑 / qwen单图编辑`
  - 提示词节点: `44`
  - 图片输入节点: `49`
  - 输出节点: `46`
- `图片编辑 / Flux2多图编辑`
  - 提示词节点: `25`
  - 图片输入节点: `22, 23, 21, 27, 26`
  - 输出节点: `35`
- `图生视频 / 图生视频-LTX2.3图生视频优化版蒸馏Dev可切换-无ReservedVRAM本地可跑版`
  - 提示词节点: `69`
  - 首帧输入节点: `191`
  - 输出节点: `375, 336`

## 部署说明

- `yihen-drama/sql/init_schema.sql` 现在已经包含当前默认 provider、model instance、workflow 路径和默认项。
- `yihen-drama/sql/patches/2026-04-01_sync_runtime_model_seed_data.sql` 用于把旧环境同步到当前配置。
- Git 中的第三方模型 `api_key` 统一保留为占位值或空串；新环境初始化后，需要在设置页补充真实密钥。
