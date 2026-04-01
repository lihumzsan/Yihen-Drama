UPDATE model_instance
SET instance_name = 'ComfyUI CHARACTER_GEN | image-generate/zimage | regenerate=image-edit/qwen-single',
    params = '{"quality":"standard","resolution":"1024x1024","workflowMode":"auto","workflowGroup":"image-generate","workflowName":"zimage / official","workflowPath":"D:\\comfui\\workflows\\baseimage\\图片生成\\zimage\\场景文生图-造相ZImageTurbo官方版-RunningHub原始工作流.json","outputMapping":{"nodeId":"10"},"pollIntervalMs":3000,"historyTimeoutMs":180000,"regenerateWorkflowGroup":"image-edit","regenerateWorkflowName":"qwen / single-edit","regenerateWorkflowPath":"D:\\comfui\\workflows\\baseimage\\图片编辑\\qwen\\单图编辑.json","regenerateInputMapping":{"image":{"nodeId":"49","field":"image","type":"image","filenamePrefix":"character-regenerate"},"prompt":{"nodeId":"44","field":"prompt","type":"string"}},"regenerateOutputMapping":{"nodeId":"46"}}'
WHERE id = 25;

UPDATE model_instance
SET instance_name = 'ComfyUI SCENE_GEN | image-generate/zimage',
    params = '{"quality":"standard","resolution":"1920x1080","workflowMode":"auto","workflowGroup":"image-generate","workflowName":"zimage / official","workflowPath":"D:\\comfui\\workflows\\baseimage\\图片生成\\zimage\\场景文生图-造相ZImageTurbo官方版-RunningHub原始工作流.json","outputMapping":{"nodeId":"10"},"pollIntervalMs":3000,"historyTimeoutMs":180000}'
WHERE id = 26;

UPDATE model_instance
SET instance_name = 'ComfyUI FIRST_FRAME_GEN | image-edit/flux-five',
    params = '{"quality":"standard","resolution":"768x1360","workflowMode":"auto","workflowGroup":"image-edit","workflowName":"flux / five-image-edit","workflowPath":"D:\\comfui\\workflows\\baseimage\\图片编辑\\flux\\参考编辑-5图编辑创作Flux2多图编辑-RunningHub原始工作流.json","inputMapping":{"images":[{"type":"image","field":"image","nodeId":"22","subfolder":"comfy-input","filenamePrefix":"firstframe-1"},{"type":"image","field":"image","nodeId":"23","subfolder":"comfy-input","filenamePrefix":"firstframe-2"},{"type":"image","field":"image","nodeId":"21","subfolder":"comfy-input","filenamePrefix":"firstframe-3"},{"type":"image","field":"image","nodeId":"27","subfolder":"comfy-input","filenamePrefix":"firstframe-4"},{"type":"image","field":"image","nodeId":"26","subfolder":"comfy-input","filenamePrefix":"firstframe-5"}],"prompt":{"field":"text","nodeId":"25"}},"outputMapping":{"nodeId":"35"},"pollIntervalMs":3000,"historyTimeoutMs":240000}'
WHERE id = 27;
