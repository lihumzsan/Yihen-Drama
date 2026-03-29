package com.yihen.core.model.strategy.video.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.yihen.controller.vo.CharactersRequestVO;
import com.yihen.controller.vo.VideoModelRequestVO;
import com.yihen.core.model.strategy.comfyui.ComfyUiClient;
import com.yihen.core.model.strategy.comfyui.ComfyUiWorkflowBuilder;
import com.yihen.core.model.strategy.video.VideoModelStrategy;
import com.yihen.core.model.strategy.video.dto.VideoTaskDTO;
import com.yihen.entity.Characters;
import com.yihen.entity.ModelDefinition;
import com.yihen.entity.ModelInstance;
import com.yihen.entity.Storyboard;
import com.yihen.mapper.ModelDefinitionMapper;
import com.yihen.service.CharacterService;
import com.yihen.service.ModelManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ComfyUiVideoModelStrategy implements VideoModelStrategy {

    private static final String STRATEGY_TYPE = "comfyui";

    private final ModelManageService modelManageService;
    private final ModelDefinitionMapper modelDefinitionMapper;
    private final CharacterService characterService;
    private final ComfyUiClient comfyUiClient;
    private final ComfyUiWorkflowBuilder workflowBuilder;

    @Override
    public String create(CharactersRequestVO charactersRequestVO) throws Exception {
        ModelInstance modelInstance = modelManageService.getModelInstanceById(charactersRequestVO.getModelInstanceId());
        String baseUrl = modelDefinitionMapper.getBaseUrlById(modelInstance.getModelDefId());
        Characters characters = characterService.getById(charactersRequestVO.getCharacterId());

        Map<String, Object> runtimeInputs = new HashMap<>();
        applyPromptAliases(runtimeInputs, resolvePrompt(charactersRequestVO.getDescription(), characters));
        if (characters != null && StringUtils.hasText(characters.getAvatar())) {
            runtimeInputs.put("image", characters.getAvatar());
        }
        applyVideoParams(runtimeInputs, modelInstance, null);

        ComfyUiWorkflowBuilder.PromptBuildResult promptBuildResult =
                workflowBuilder.buildPrompt(baseUrl, modelInstance.getApiKey(), modelInstance, runtimeInputs);
        return comfyUiClient.queuePrompt(
                baseUrl,
                workflowBuilder.getQueuePath(modelInstance),
                modelInstance.getApiKey(),
                promptBuildResult.prompt()
        );
    }

    @Override
    public String createShotVideoTask(VideoModelRequestVO videoModelRequestVO) throws Exception {
        ModelInstance modelInstance = modelManageService.getModelInstanceById(videoModelRequestVO.getModelInstanceId());
        String baseUrl = modelDefinitionMapper.getBaseUrlById(modelInstance.getModelDefId());
        Storyboard storyboard = (Storyboard) videoModelRequestVO.getObject();

        Map<String, Object> runtimeInputs = new HashMap<>();
        applyPromptAliases(runtimeInputs, resolvePrompt(storyboard == null ? null : storyboard.getVideoPrompt(), storyboard));
        if (storyboard != null && StringUtils.hasText(storyboard.getThumbnail())) {
            runtimeInputs.put("image", storyboard.getThumbnail());
        }
        applyVideoParams(runtimeInputs, modelInstance, videoModelRequestVO.getParams());

        ComfyUiWorkflowBuilder.PromptBuildResult promptBuildResult =
                workflowBuilder.buildPrompt(baseUrl, modelInstance.getApiKey(), modelInstance, runtimeInputs);
        return comfyUiClient.queuePrompt(
                baseUrl,
                workflowBuilder.getQueuePath(modelInstance),
                modelInstance.getApiKey(),
                promptBuildResult.prompt()
        );
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

    @Override
    public VideoTaskDTO queryTask(String taskId, Long modelInstanceId) throws Exception {
        ModelInstance modelInstance = modelManageService.getModelInstanceById(modelInstanceId);
        String baseUrl = modelDefinitionMapper.getBaseUrlById(modelInstance.getModelDefId());
        JsonNode history = comfyUiClient.getHistory(baseUrl, modelInstance.getApiKey(), taskId);
        JsonNode historyItem = comfyUiClient.resolveHistoryItem(history, taskId);

        VideoTaskDTO dto = new VideoTaskDTO();
        String status = resolveHistoryStatus(historyItem);
        dto.setStatus(status);

        if ("SUCCESS".equals(status)) {
            dto.setVideoUrl(
                    comfyUiClient.extractVideoOutput(history, taskId, workflowBuilder.getOutputNodeIds(modelInstance))
                            .map(file -> comfyUiClient.buildViewUrl(baseUrl, file))
                            .orElse(null)
            );
        } else if ("FAILED".equals(status)) {
            dto.setMessage(resolveHistoryError(historyItem));
        }
        return dto;
    }

    private String resolvePrompt(String prompt, Storyboard storyboard) {
        if (StringUtils.hasText(prompt)) {
            return prompt;
        }
        if (storyboard != null && StringUtils.hasText(storyboard.getDescription())) {
            return storyboard.getDescription();
        }
        return "Generate a storyboard video";
    }

    private String resolvePrompt(String prompt, Characters characters) {
        if (StringUtils.hasText(prompt)) {
            return prompt;
        }
        if (characters != null && StringUtils.hasText(characters.getDescription())) {
            return characters.getDescription();
        }
        return "Generate a character video";
    }

    private void applyPromptAliases(Map<String, Object> runtimeInputs, String prompt) {
        runtimeInputs.put("prompt", prompt);
        runtimeInputs.put("text", prompt);
        runtimeInputs.put("description", prompt);
    }

    private void applyVideoParams(Map<String, Object> runtimeInputs, ModelInstance modelInstance, Map<String, Object> requestParams) {
        Map<String, Object> params = modelInstance.getParams() == null ? Collections.emptyMap() : modelInstance.getParams();
        Object fps = firstNonNull(requestParams == null ? null : requestParams.get("fps"), params.get("fps"));
        Object duration = firstNonNull(requestParams == null ? null : requestParams.get("duration"), params.get("duration"));
        if (fps != null) {
            runtimeInputs.put("fps", fps);
        }
        if (duration != null) {
            runtimeInputs.put("duration", duration);
        }
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private String resolveHistoryStatus(JsonNode historyItem) {
        if (historyItem == null || historyItem.isMissingNode() || historyItem.isNull()) {
            return "QUEUED";
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
}
