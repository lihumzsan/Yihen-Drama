package com.yihen.core.model.impl;

import com.alibaba.fastjson.JSON;
import com.yihen.controller.vo.TextModelRequestVO;
import com.yihen.core.model.ShotVideoGenerateTextModelService;
import com.yihen.entity.PromptTemplate;
import com.yihen.entity.Storyboard;
import com.yihen.service.PromptTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ShotVideoGenerateTextModelServiceImpl extends TextModelServiceImpl implements ShotVideoGenerateTextModelService {
    @Autowired
    private PromptTemplateService promptTemplateService;

    @Override
    public String extract(TextModelRequestVO textModelRequestVO) throws Exception {
        PromptTemplate promptTemplate =
                promptTemplateService.getDefaultTemplateBySceneCode(textModelRequestVO.getSceneCode());

        Storyboard storyboard = (Storyboard) textModelRequestVO.getObject();

        String message = applyProjectPromptContext(promptTemplate.getPromptContent(), textModelRequestVO.getProjectId())
                .replace("{{shot_description}}", safePromptText(storyboard.getDescription()))
                .replace("{{first_frame_prompt}}", safePromptText(storyboard.getImagePrompt()))
                .replace("{{characters_json}}", JSON.toJSONString(storyboard.getCharacters() == null ? List.of() : storyboard.getCharacters()))
                .replace("{{scenes_json}}", JSON.toJSONString(storyboard.getScenes() == null ? List.of() : storyboard.getScenes()));

        textModelRequestVO.setText(message);
        return generate(textModelRequestVO.getModelId(), message);
    }
}
