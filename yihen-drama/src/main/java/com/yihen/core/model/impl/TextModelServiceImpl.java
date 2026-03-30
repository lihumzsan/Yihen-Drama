package com.yihen.core.model.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yihen.core.model.TextModelService;
import com.yihen.entity.ModelInstance;
import com.yihen.entity.Project;
import com.yihen.entity.StyleTemplate;
import com.yihen.http.HttpExecutor;
import com.yihen.mapper.ModelDefinitionMapper;
import com.yihen.mapper.ModelInstanceMapper;
import com.yihen.mapper.ProjectMapper;
import com.yihen.service.PromptTemplateService;
import com.yihen.service.StyleTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Slf4j
@Service
public class TextModelServiceImpl extends ServiceImpl<ModelInstanceMapper, ModelInstance> implements TextModelService {
    @Autowired
    private ModelDefinitionMapper modelDefinitionMapper;

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired
    private HttpExecutor  httpExecutor;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private StyleTemplateService styleTemplateService;

    @Override
    public String generate(Long modelId , String text) throws Exception {

        // 1. 获取模型实例信息
        ModelInstance modelInstance = getById(modelId);
        if (modelInstance == null) {
            throw new Exception("未找到可用的文本模型实例");
        }

        // 2. 获取厂商定义的baseurl
        String baseUrl = modelDefinitionMapper.getBaseUrlById(modelInstance.getModelDefId());
        // 3. 拼接发送请求信息
        HashMap<String, Object> body = (HashMap<String, Object>) modelInstance.getParams();
        if (ObjectUtils.isEmpty(body)) {
            body= new HashMap<>();
        }
        body.put("model", modelInstance.getModelCode());


        // 3.1 构建消息发送
        List<Map<String, String>> messages = List.of(
                Map.of("role", "user", "content", text)
        );

        body.put("messages", messages);

        // 4. 发送请求
        // 5. 获取响应结果
        String response;
        try {
            response = httpExecutor.post(baseUrl, modelInstance.getPath(), modelInstance.getApiKey(), body).block();
        } catch (Exception e) {
            throw adaptRequestException(modelInstance, e);
        }

        // 解析结果
        String content = extractResponse(response);

        return content;
    }

    // 响应结果提取
    private String extractResponse(String response) throws Exception {

            // 1. 转成JSONObject对象
            JSONObject jsonObject = JSONObject.parseObject(response);
            if (jsonObject.containsKey("error")) {
                // 调用失败
                String errorMessage = jsonObject.getJSONObject("error").getString("message");
                throw new Exception(errorMessage);
            }

            String content = jsonObject.getJSONArray("choices")
                    .getJSONObject(0).getJSONObject("message").getString("content");

            if (org.springframework.util.ObjectUtils.isEmpty(content)) {
                throw new Exception("返回结果结构正确，但是返回数据为空！再次尝试");
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
        String message = safePromptText(template);
        ProjectPromptContext context = buildProjectPromptContext(projectId);
        boolean hasProjectPlaceholder = hasProjectContextPlaceholder(message);

        message = message
                .replace("{{style_template}}", context.stylePrompt())
                .replace("{{project_constraints}}", context.instructionPrompt())
                .replace("{{project_description}}", context.projectDescription())
                .replace("{{project_style_name}}", context.styleName())
                .replace("{{project_consistency}}", context.consistencyPrompt());

        message = clearResidualProjectPlaceholders(message);

        if (!hasProjectPlaceholder && StringUtils.hasText(context.instructionPrompt())) {
            message = injectInstructionBlock(message, context.instructionPrompt());
        }
        return message;
    }

    protected String safePromptText(String value) {
        return value == null ? "" : value;
    }

    private ProjectPromptContext buildProjectPromptContext(Long projectId) {
        if (projectId == null) {
            return ProjectPromptContext.empty();
        }

        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            return ProjectPromptContext.empty();
        }

        StyleTemplate styleTemplate = project.getStyleId() == null
                ? null
                : styleTemplateService.getById(project.getStyleId());

        String styleName = safePromptText(styleTemplate != null ? styleTemplate.getName() : null).trim();
        String styleDescription = safePromptText(styleTemplate != null ? styleTemplate.getDescription() : null).trim();
        String projectDescription = safePromptText(project.getDescription()).trim();
        String consistencyPrompt = buildConsistencyPrompt(project.getConsistency());

        List<String> stylePromptParts = new ArrayList<>();
        if (StringUtils.hasText(styleName)) {
            stylePromptParts.add("整体视觉风格遵循" + styleName);
        }
        if (StringUtils.hasText(styleDescription)) {
            stylePromptParts.add(styleDescription);
        }
        if (StringUtils.hasText(projectDescription)) {
            stylePromptParts.add("额外遵循以下项目要求：" + projectDescription);
        }
        if (StringUtils.hasText(consistencyPrompt)) {
            stylePromptParts.add(consistencyPrompt);
        }

        List<String> instructionLines = new ArrayList<>();
        if (StringUtils.hasText(styleName)) {
            instructionLines.add("- 保持整体视觉风格为" + styleName);
        }
        if (StringUtils.hasText(styleDescription)) {
            instructionLines.add("- 风格细则：" + styleDescription);
        }
        if (StringUtils.hasText(projectDescription)) {
            instructionLines.add("- 项目补充要求：" + projectDescription);
        }
        if (StringUtils.hasText(consistencyPrompt)) {
            instructionLines.add("- 一致性要求：" + consistencyPrompt);
        }

        return new ProjectPromptContext(
                styleName,
                projectDescription,
                consistencyPrompt,
                String.join("，", stylePromptParts),
                String.join("\n", instructionLines)
        );
    }

    private String buildConsistencyPrompt(Integer consistency) {
        if (consistency == null || consistency <= 0) {
            return "";
        }
        return "保持跨镜头的角色外观、场景空间与整体视觉一致性，目标强度约为" + consistency + "%";
    }

    private boolean hasProjectContextPlaceholder(String template) {
        return template.contains("{{style_template}}")
                || template.contains("{{project_constraints}}")
                || template.contains("{{project_description}}")
                || template.contains("{{project_style_name}}")
                || template.contains("{{project_consistency}}");
    }

    private String clearResidualProjectPlaceholders(String template) {
        return template
                .replace("{{style_template}}", "")
                .replace("{{project_constraints}}", "")
                .replace("{{project_description}}", "")
                .replace("{{project_style_name}}", "")
                .replace("{{project_consistency}}", "");
    }

    private String injectInstructionBlock(String template, String instructionPrompt) {
        String block = "【项目级固定约束（必须继承到最终输出，不得遗漏）】\n"
                + "- 以下约束必须自然融入最终提示词正文，至少保留核心风格词，不得只在内部理解后省略\n"
                + instructionPrompt;
        String[] markers = {"【现在开始生成】", "【现在开始】", "【现在开始输出】", "【现在开始创作】"};
        for (String marker : markers) {
            int index = template.indexOf(marker);
            if (index >= 0) {
                return template.substring(0, index).trim() + "\n\n" + block + "\n\n" + template.substring(index);
            }
        }
        return template + "\n\n" + block;
    }

    private record ProjectPromptContext(
            String styleName,
            String projectDescription,
            String consistencyPrompt,
            String stylePrompt,
            String instructionPrompt
    ) {
        private static ProjectPromptContext empty() {
            return new ProjectPromptContext("", "", "", "", "");
        }
    }
}
