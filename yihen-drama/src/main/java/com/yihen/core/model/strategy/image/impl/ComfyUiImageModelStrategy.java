package com.yihen.core.model.strategy.image.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.yihen.controller.vo.ImageModelRequestVO;
import com.yihen.core.model.strategy.comfyui.ComfyUiClient;
import com.yihen.core.model.strategy.comfyui.ComfyUiWorkflowBuilder;
import com.yihen.core.model.strategy.image.ImageModelStrategy;
import com.yihen.entity.Characters;
import com.yihen.entity.ModelDefinition;
import com.yihen.entity.ModelInstance;
import com.yihen.entity.Scene;
import com.yihen.entity.Storyboard;
import com.yihen.mapper.ModelDefinitionMapper;
import com.yihen.service.ModelManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ComfyUiImageModelStrategy implements ImageModelStrategy {

    private static final String STRATEGY_TYPE = "comfyui";
    private static final long DEFAULT_TIMEOUT_MS = 180_000L;
    private static final long DEFAULT_POLL_INTERVAL_MS = 3_000L;
    private static final int MAX_REFERENCE_IMAGES = 5;

    private final ModelManageService modelManageService;
    private final ModelDefinitionMapper modelDefinitionMapper;
    private final ComfyUiClient comfyUiClient;
    private final ComfyUiWorkflowBuilder workflowBuilder;

    @Override
    public String create(ImageModelRequestVO imageModelRequestVO) throws Exception {
        ModelInstance modelInstance = modelManageService.getModelInstanceById(imageModelRequestVO.getModelInstanceId());
        String baseUrl = modelDefinitionMapper.getBaseUrlById(modelInstance.getModelDefId());

        Map<String, Object> runtimeInputs = new HashMap<>();
        applyPromptAliases(runtimeInputs, resolvePrompt(imageModelRequestVO.getDescription(), null));
        applyResolution(runtimeInputs, modelInstance);

        ComfyUiWorkflowBuilder.PromptBuildResult promptBuildResult =
                workflowBuilder.buildPrompt(baseUrl, modelInstance.getApiKey(), modelInstance, runtimeInputs);
        String promptId = comfyUiClient.queuePrompt(
                baseUrl,
                workflowBuilder.getQueuePath(modelInstance),
                modelInstance.getApiKey(),
                promptBuildResult.prompt()
        );
        return waitForImage(baseUrl, modelInstance, promptId, promptBuildResult.outputNodeIds());
    }

    @Override
    public String createByTextAndImage(ImageModelRequestVO imageModelRequestVO) throws Exception {
        ModelInstance modelInstance = modelManageService.getModelInstanceById(imageModelRequestVO.getModelInstanceId());
        String baseUrl = modelDefinitionMapper.getBaseUrlById(modelInstance.getModelDefId());
        Storyboard storyboard = (Storyboard) imageModelRequestVO.getObject();

        Map<String, Object> runtimeInputs = new HashMap<>();
        applyPromptAliases(runtimeInputs, resolvePrompt(storyboard == null ? null : storyboard.getImagePrompt(),
                storyboard == null ? null : storyboard.getDescription()));
        applyResolution(runtimeInputs, modelInstance);

        List<String> referenceImages = collectReferenceImages(storyboard);
        if (!referenceImages.isEmpty()) {
            runtimeInputs.put("images", referenceImages);
            runtimeInputs.put("refImages", referenceImages);
            runtimeInputs.put("image", referenceImages.get(0));
        }

        ComfyUiWorkflowBuilder.PromptBuildResult promptBuildResult =
                workflowBuilder.buildPrompt(baseUrl, modelInstance.getApiKey(), modelInstance, runtimeInputs);
        String promptId = comfyUiClient.queuePrompt(
                baseUrl,
                workflowBuilder.getQueuePath(modelInstance),
                modelInstance.getApiKey(),
                promptBuildResult.prompt()
        );
        return waitForImage(baseUrl, modelInstance, promptId, promptBuildResult.outputNodeIds());
    }

    @Override
    public String getStrategyType() {
        return STRATEGY_TYPE;
    }

    @Override
    public boolean supports(ModelInstance modelInstance) {
        ModelDefinition modelDefinition = modelManageService.getById(modelInstance.getModelDefId());
        return modelDefinition != null && STRATEGY_TYPE.equalsIgnoreCase(modelDefinition.getProviderCode());
    }

    private String waitForImage(
            String baseUrl,
            ModelInstance modelInstance,
            String promptId,
            Collection<String> outputNodeIds
    ) throws Exception {
        long timeoutMs = getLongParam(modelInstance, "historyTimeoutMs", DEFAULT_TIMEOUT_MS);
        long pollIntervalMs = getLongParam(modelInstance, "pollIntervalMs", DEFAULT_POLL_INTERVAL_MS);
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            JsonNode history = comfyUiClient.getHistory(baseUrl, modelInstance.getApiKey(), promptId);
            JsonNode historyItem = comfyUiClient.resolveHistoryItem(history, promptId);
            String status = resolveHistoryStatus(historyItem);
            if ("SUCCESS".equals(status)) {
                return comfyUiClient.extractImageOutput(history, promptId, outputNodeIds)
                        .map(file -> comfyUiClient.buildViewUrl(baseUrl, file))
                        .orElseThrow(() -> new IllegalStateException("ComfyUI finished but no image output was found"));
            }
            if ("FAILED".equals(status)) {
                throw new IllegalStateException(resolveHistoryError(historyItem));
            }
            Thread.sleep(pollIntervalMs);
        }

        throw new IllegalStateException("Timed out waiting for ComfyUI image output");
    }

    private String resolvePrompt(String prompt, String fallback) {
        if (StringUtils.hasText(prompt)) {
            return prompt;
        }
        if (StringUtils.hasText(fallback)) {
            return fallback;
        }
        return "Generate a storyboard image";
    }

    private List<String> collectReferenceImages(Storyboard storyboard) {
        if (storyboard == null) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> referenceImages = new LinkedHashSet<>();
        if (storyboard.getScenes() != null) {
            storyboard.getScenes().stream()
                    .map(Scene::getThumbnail)
                    .filter(StringUtils::hasText)
                    .forEach(referenceImages::add);
        }
        if (storyboard.getCharacters() != null) {
            storyboard.getCharacters().stream()
                    .map(Characters::getAvatar)
                    .filter(StringUtils::hasText)
                    .forEach(referenceImages::add);
        }
        if (referenceImages.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(referenceImages).subList(0, Math.min(referenceImages.size(), MAX_REFERENCE_IMAGES));
    }

    private void applyPromptAliases(Map<String, Object> runtimeInputs, String prompt) {
        runtimeInputs.put("prompt", prompt);
        runtimeInputs.put("text", prompt);
        runtimeInputs.put("description", prompt);
    }

    private void applyResolution(Map<String, Object> runtimeInputs, ModelInstance modelInstance) {
        Object resolution = safeParams(modelInstance).get("resolution");
        if (!(resolution instanceof String text) || !text.contains("x")) {
            return;
        }
        String[] parts = text.toLowerCase(Locale.ROOT).split("x");
        if (parts.length != 2) {
            return;
        }
        try {
            runtimeInputs.put("width", Integer.parseInt(parts[0].trim()));
            runtimeInputs.put("height", Integer.parseInt(parts[1].trim()));
        } catch (NumberFormatException ignore) {
        }
    }

    private String resolveHistoryStatus(JsonNode historyItem) {
        if (historyItem == null || historyItem.isMissingNode() || historyItem.isNull()) {
            return "RUNNING";
        }
        JsonNode status = historyItem.path("status");
        String statusStr = status.path("status_str").asText("");
        if ("success".equalsIgnoreCase(statusStr)) {
            return "SUCCESS";
        }
        if ("error".equalsIgnoreCase(statusStr)) {
            return "FAILED";
        }
        if (historyItem.path("outputs").size() > 0) {
            return "SUCCESS";
        }
        return "RUNNING";
    }

    private String resolveHistoryError(JsonNode historyItem) {
        if (historyItem == null) {
            return "ComfyUI execution failed";
        }
        JsonNode messages = historyItem.path("status").path("messages");
        if (messages.isArray()) {
            for (JsonNode message : messages) {
                if (message.isArray() && message.size() > 1) {
                    String event = message.get(0).asText("");
                    if ("execution_error".equalsIgnoreCase(event) && message.get(1).isObject()) {
                        String exceptionMessage = message.get(1).path("exception_message").asText("");
                        if (StringUtils.hasText(exceptionMessage)) {
                            return exceptionMessage;
                        }
                    }
                }
            }
        }
        return "ComfyUI execution failed";
    }

    private long getLongParam(ModelInstance modelInstance, String key, long defaultValue) {
        Object value = safeParams(modelInstance).get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Map<String, Object> safeParams(ModelInstance modelInstance) {
        return modelInstance.getParams() == null ? Collections.emptyMap() : modelInstance.getParams();
    }
}
