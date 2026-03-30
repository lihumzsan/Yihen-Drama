package com.yihen.core.model.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yihen.core.model.TextModelService;
import com.yihen.core.model.support.PromptContextSupport;
import com.yihen.entity.ModelInstance;
import com.yihen.http.HttpExecutor;
import com.yihen.mapper.ModelDefinitionMapper;
import com.yihen.mapper.ModelInstanceMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TextModelServiceImpl extends ServiceImpl<ModelInstanceMapper, ModelInstance> implements TextModelService {

    @Autowired
    private ModelDefinitionMapper modelDefinitionMapper;

    @Autowired
    private HttpExecutor httpExecutor;

    @Autowired
    private PromptContextSupport promptContextSupport;

    @Override
    public String generate(Long modelId, String text) throws Exception {
        ModelInstance modelInstance = getById(modelId);
        if (modelInstance == null) {
            throw new Exception("未找到可用的文本模型实例");
        }

        String baseUrl = modelDefinitionMapper.getBaseUrlById(modelInstance.getModelDefId());
        HashMap<String, Object> body = modelInstance.getParams() == null
                ? new HashMap<>()
                : new HashMap<>(modelInstance.getParams());
        body.put("model", modelInstance.getModelCode());

        List<Map<String, String>> messages = List.of(
                Map.of("role", "user", "content", text)
        );
        body.put("messages", messages);

        String response;
        try {
            response = httpExecutor.post(baseUrl, modelInstance.getPath(), modelInstance.getApiKey(), body).block();
        } catch (Exception e) {
            throw adaptRequestException(modelInstance, e);
        }

        return extractResponse(response);
    }

    private String extractResponse(String response) throws Exception {
        JSONObject jsonObject = JSONObject.parseObject(response);
        if (jsonObject.containsKey("error")) {
            String errorMessage = jsonObject.getJSONObject("error").getString("message");
            throw new Exception(errorMessage);
        }

        String content = jsonObject.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");

        if (org.springframework.util.ObjectUtils.isEmpty(content)) {
            throw new Exception("返回结果结构正确，但返回内容为空，请重试");
        }

        return content;
    }

    private Exception adaptRequestException(ModelInstance modelInstance, Exception exception) {
        String message = exception.getMessage();
        String modelName = modelInstance.getInstanceName() != null
                ? modelInstance.getInstanceName()
                : modelInstance.getModelCode();
        if (message != null && message.contains("401 Unauthorized")) {
            return new Exception("文本模型「" + modelName + "」认证失败，请检查 API Key 是否有效");
        }
        if (message != null && message.contains("404 Not Found")) {
            return new Exception("文本模型「" + modelName + "」接口地址配置错误，请检查 Base URL 和 Path");
        }
        return exception;
    }

    protected String applyProjectPromptContext(String template, Long projectId) {
        return promptContextSupport.applyContext(template, projectId, null);
    }

    protected String applyProjectPromptContext(String template, Long projectId, Long episodeId) {
        return promptContextSupport.applyContext(template, projectId, episodeId);
    }

    protected String safePromptText(String value) {
        return value == null ? "" : value;
    }
}
