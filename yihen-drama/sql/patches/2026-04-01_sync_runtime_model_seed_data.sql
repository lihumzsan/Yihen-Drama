-- 让已有环境与 2026-04-01 当前运行中的模型配置保持一致。
-- 注意：Git 中仅保留占位密钥，执行后仍需在模型设置页补充真实 API Key。

INSERT INTO `model_definition` (`id`, `provider_code`, `base_url`, `status`, `create_time`, `update_time`, `is_deleted`) VALUES
    (1, 'Deepseek', 'https://api.deepseek.com', 1, '2026-01-20 12:57:40', '2026-01-22 06:12:59', 0),
    (2, '阿里百炼平台', 'https://coding.dashscope.aliyuncs.com/v1', 1, '2026-01-20 13:48:20', '2026-03-29 18:38:23', 0),
    (3, 'Kimi', 'https://api.moonshot.cn/v1', 1, '2026-01-21 03:57:22', '2026-01-22 12:03:58', 0),
    (4, '火山引擎', 'https://ark.cn-beijing.volces.com/api/v3', 1, '2026-01-22 12:03:28', '2026-01-22 12:03:28', 0),
    (17, 'comfyui', 'http://127.0.0.1:18188', 1, '2026-03-29 18:00:53', '2026-03-29 18:07:16', 0),
    (18, 'comfyui', 'http://127.0.0.1:8188', 1, '2026-03-29 18:07:16', '2026-03-29 18:07:16', 0)
ON DUPLICATE KEY UPDATE
    `provider_code` = VALUES(`provider_code`),
    `base_url` = VALUES(`base_url`),
    `status` = VALUES(`status`),
    `update_time` = VALUES(`update_time`),
    `is_deleted` = VALUES(`is_deleted`);

INSERT INTO `model_instance` (`id`, `model_def_id`, `model_code`, `model_type`, `instance_name`, `scene_code`, `path`, `api_key`, `params`, `status`, `create_time`, `update_time`, `is_deleted`) VALUES
    (2, 2, 'cosyvoice-v3-flash', 4, 'Cosyvoice-v3-flash', 6, '/', 'sk-', '{"speed": 1, "voice": "zh-CN-Xiaoxiao"}', 1, '2026-01-20 13:51:42', '2026-02-02 13:41:06', 0),
    (7, 4, 'doubao-seedream-4-5-251128', 2, 'Doubao-Seedream-4.5', 3, '/images/generations', 'sk-', '{"size": "2K", "watermark": false}', 1, '2026-01-23 12:13:28', '2026-02-04 13:47:58', 0),
    (9, 4, 'doubao-seedance-1-5-pro-251215', 3, 'Doubao-seedance-1-5-pro', 5, '/contents/generations/tasks', 'sk-', '{"duration": 5}', 1, '2026-01-24 09:37:53', '2026-01-24 12:38:03', 0),
    (11, 4, 'doubao-seedance-1-0-lite-i2v-250428', 3, 'Doubao-seedance-1-0-lite-i2v', 5, '/contents/generations/tasks', 'sk-', '{"fps": 30, "duration": 5, "watermark": false}', 1, '2026-02-05 09:06:15', '2026-02-05 09:37:26', 0),
    (22, 2, 'qwen3.5-plus', 1, 'qwen3.5-plus', 1, '/chat/completions', 'sk-', '{"max_tokens": 4000, "temperature": 0.7}', 1, '2026-03-29 14:42:19', '2026-03-29 18:38:23', 0),
    (23, 2, 'glm-5', 1, 'glm-5', 1, '/chat/completions', 'sk-', '{"max_tokens": 4000, "temperature": 0.7}', 1, '2026-03-29 14:43:11', '2026-03-29 18:38:23', 0),
    (24, 2, 'kimi-k2.5', 1, 'kimi-k2.5', 1, '/chat/completions', 'sk-', '{"max_tokens": 4000, "temperature": 0.7}', 1, '2026-03-29 14:43:52', '2026-03-29 18:38:23', 0),
    (25, 17, 'comfyui-zimage-character', 2, 'ComfyUI 角色图生成', 3, '/prompt', '', '{"quality": "standard", "resolution": "1024x1024", "workflowMode": "auto", "workflowGroup": "图片生成", "workflowName": "ZImageTurbo造相", "workflowPath": "D:\\\\comfui\\\\workflows\\\\baseimage\\\\图片生成\\\\ZImageTurbo造相.json", "outputMapping": {"nodeId": "10"}, "pollIntervalMs": 3000, "historyTimeoutMs": 180000, "regenerateWorkflowGroup": "图片编辑", "regenerateWorkflowName": "qwen单图编辑", "regenerateWorkflowPath": "D:\\\\comfui\\\\workflows\\\\baseimage\\\\图片编辑\\\\qwen单图编辑.json", "regenerateInputMapping": {"image": {"nodeId": "49", "field": "image", "type": "image", "filenamePrefix": "character-regenerate"}, "prompt": {"nodeId": "44", "field": "prompt", "type": "string"}}, "regenerateOutputMapping": {"nodeId": "46"}}', 1, '2026-03-29 18:01:56', '2026-04-01 00:00:00', 0),
    (26, 17, 'comfyui-zimage-scene', 2, 'ComfyUI 场景图生成', 1, '/prompt', '', '{"quality": "standard", "resolution": "1920x1080", "workflowMode": "auto", "workflowGroup": "图片生成", "workflowName": "ZImageTurbo造相", "workflowPath": "D:\\\\comfui\\\\workflows\\\\baseimage\\\\图片生成\\\\ZImageTurbo造相.json", "outputMapping": {"nodeId": "10"}, "pollIntervalMs": 3000, "historyTimeoutMs": 180000}', 1, '2026-03-29 18:01:56', '2026-04-01 00:00:00', 0),
    (27, 17, 'comfyui-flux2-firstframe', 2, 'ComfyUI 首帧图生成', 7, '/prompt', '', '{"quality": "standard", "resolution": "768x1360", "workflowMode": "auto", "workflowGroup": "图片编辑", "workflowName": "Flux2多图编辑", "workflowPath": "D:\\\\comfui\\\\workflows\\\\baseimage\\\\图片编辑\\\\Flux2多图编辑.json", "inputMapping": {"images": [{"type": "image", "field": "image", "nodeId": "22", "subfolder": "comfy-input", "filenamePrefix": "firstframe-1"}, {"type": "image", "field": "image", "nodeId": "23", "subfolder": "comfy-input", "filenamePrefix": "firstframe-2"}, {"type": "image", "field": "image", "nodeId": "21", "subfolder": "comfy-input", "filenamePrefix": "firstframe-3"}, {"type": "image", "field": "image", "nodeId": "27", "subfolder": "comfy-input", "filenamePrefix": "firstframe-4"}, {"type": "image", "field": "image", "nodeId": "26", "subfolder": "comfy-input", "filenamePrefix": "firstframe-5"}], "prompt": {"field": "text", "nodeId": "25"}}, "outputMapping": {"nodeId": "35"}, "pollIntervalMs": 3000, "historyTimeoutMs": 240000}', 1, '2026-03-29 18:01:56', '2026-04-01 00:00:00', 0),
    (28, 17, 'comfyui-ltx23-i2v', 3, 'ComfyUI LTX2.3 I2V', 5, '/prompt', '', '{"fps": 24, "duration": 10, "inputMapping": {"image": {"type": "image", "field": "image", "nodeId": 191, "subfolder": "comfy-input", "filenamePrefix": "video-start"}, "prompt": {"field": "text", "nodeId": 69}}, "workflowMode": "auto", "workflowGroup": "图生视频", "workflowName": "图生视频-LTX2.3图生视频优化版蒸馏Dev可切换-无ReservedVRAM本地可跑版", "workflowPath": "D:\\\\comfui\\\\workflows\\\\basevideo\\\\图生视频\\\\图生视频-LTX2.3图生视频优化版蒸馏Dev可切换-无ReservedVRAM本地可跑版.json", "outputMapping": {"nodeIds": [375, 336]}, "pollIntervalMs": 5000, "historyTimeoutMs": 600000}', 1, '2026-03-29 18:01:56', '2026-03-29 18:01:56', 0)
ON DUPLICATE KEY UPDATE
    `model_def_id` = VALUES(`model_def_id`),
    `model_code` = VALUES(`model_code`),
    `model_type` = VALUES(`model_type`),
    `instance_name` = VALUES(`instance_name`),
    `scene_code` = VALUES(`scene_code`),
    `path` = VALUES(`path`),
    `api_key` = VALUES(`api_key`),
    `params` = VALUES(`params`),
    `status` = VALUES(`status`),
    `update_time` = VALUES(`update_time`),
    `is_deleted` = VALUES(`is_deleted`);

UPDATE `model_instance`
SET `status` = 0,
    `is_deleted` = 1,
    `update_time` = '2026-04-01 00:00:00'
WHERE `id` IN (1, 3, 4, 10);

DELETE FROM `model_instance_default`
WHERE (`model_type` = 1 AND (`remark` IS NULL OR `remark` = ''))
   OR (`model_type` = 2 AND ((`remark` IS NULL OR `remark` = '') OR `remark` IN ('CHARACTER_GEN', 'SCENE_GEN', 'FIRST_FRAME_GEN')))
   OR (`model_type` = 3 AND ((`remark` IS NULL OR `remark` = '') OR `remark` = 'VIDEO_GEN'))
   OR (`model_type` = 4 AND (`remark` IS NULL OR `remark` = ''));

INSERT INTO `model_instance_default` (`id`, `model_type`, `model_instance_id`, `status`, `remark`, `create_time`, `update_time`, `is_deleted`) VALUES
    (2, 1, 22, 1, NULL, '2026-01-22 02:55:32', '2026-03-29 14:42:26', 0),
    (5, 4, 2, 1, NULL, '2026-02-02 13:22:59', '2026-02-02 13:22:59', 0),
    (6, 2, 25, 1, 'CHARACTER_GEN', '2026-03-29 18:01:56', '2026-03-29 18:01:56', 0),
    (7, 2, 26, 1, 'SCENE_GEN', '2026-03-29 18:01:56', '2026-03-29 18:01:56', 0),
    (8, 2, 27, 1, 'FIRST_FRAME_GEN', '2026-03-29 18:01:56', '2026-03-29 18:01:56', 0),
    (9, 3, 28, 1, 'VIDEO_GEN', '2026-03-29 18:01:56', '2026-03-29 18:01:56', 0)
ON DUPLICATE KEY UPDATE
    `model_type` = VALUES(`model_type`),
    `model_instance_id` = VALUES(`model_instance_id`),
    `status` = VALUES(`status`),
    `remark` = VALUES(`remark`),
    `update_time` = VALUES(`update_time`),
    `is_deleted` = VALUES(`is_deleted`);
