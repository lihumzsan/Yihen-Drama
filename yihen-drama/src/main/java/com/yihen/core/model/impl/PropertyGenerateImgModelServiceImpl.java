package com.yihen.core.model.impl;

import com.yihen.controller.vo.CharactersRequestVO;
import com.yihen.controller.vo.ImageModelRequestVO;
import com.yihen.controller.vo.SceneRequestVO;
import com.yihen.core.model.PropertyGenerateImgModelService;
import com.yihen.core.model.support.PromptContextSupport;
import com.yihen.entity.Characters;
import com.yihen.entity.ModelInstance;
import com.yihen.entity.PromptTemplate;
import com.yihen.entity.Scene;
import com.yihen.enums.SceneCode;
import com.yihen.service.CharacterService;
import com.yihen.service.PromptTemplateService;
import com.yihen.service.SceneService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class PropertyGenerateImgModelServiceImpl extends ImgModelServiceImpl implements PropertyGenerateImgModelService {

    private static final String ACTION_REGENERATE = "regenerate";
    private static final String DEFAULT_CHARACTER_REGENERATE_WORKFLOW_PATH =
            "D:\\comfui\\workflows\\baseimage\\02_character_consistency\\P1_QwenImageEdit2511三工作流组合_QwenImageEdit2511\\角色一致性-QwenImageEdit2511三工作流组合-RunningHub原始工作流.json";
    private static final String CHARACTER_REGENERATE_OUTPUT_NODE_ID = "46";
    private static final String CHARACTER_REGENERATE_LOAD_IMAGE_NODE_ID = "49";
    private static final String CHARACTER_REGENERATE_PROMPT_NODE_ID = "44";

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired
    private PromptContextSupport promptContextSupport;

    @Autowired
    private CharacterService characterService;

    @Autowired
    private SceneService sceneService;

    @Override
    public Characters generateCharacter(CharactersRequestVO charactersRequestVO) throws Exception {
        Characters characters = characterService.getById(charactersRequestVO.getCharacterId());
        Long episodeId = characters == null ? null : characters.getEpisodeId();
        String stylePrompt = buildCharacterStylePrompt(charactersRequestVO.getProjectId(), episodeId);

        PromptTemplate promptTemplate = promptTemplateService.getDefaultTemplateBySceneCode(SceneCode.CHARACTER_GEN);
        String message = promptTemplate.getPromptContent()
                .replace("{{input}}", charactersRequestVO.getDescription())
                .replace("{{style_template}}", stylePrompt);

        String imgUrl;
        if (shouldUseCharacterRegenerateWorkflow(charactersRequestVO, characters)) {
            ImageModelRequestVO requestVO = new ImageModelRequestVO();
            requestVO.setModelInstanceId(charactersRequestVO.getModelInstanceId());
            requestVO.setDescription(message);
            requestVO.setObject(characters);
            requestVO.setParamsOverride(buildCharacterRegenerateParams(charactersRequestVO.getModelInstanceId()));
            imgUrl = generate(requestVO);
        } else {
            imgUrl = generate(charactersRequestVO.getModelInstanceId(), message);
        }

        characters.setAvatar(imgUrl);
        return characters;
    }

    @Override
    public Scene generateScene(SceneRequestVO sceneRequestVO) throws Exception {
        Scene scene = sceneService.getById(sceneRequestVO.getSceneId());
        Long episodeId = scene == null ? null : scene.getEpisodeId();
        String stylePrompt = buildSceneStylePrompt(sceneRequestVO.getProjectId(), episodeId);

        PromptTemplate promptTemplate = promptTemplateService.getDefaultTemplateBySceneCode(SceneCode.SCENE_GEN);
        String message = promptTemplate.getPromptContent()
                .replace("{{input}}", sceneRequestVO.getDescription())
                .replace("{{style_template}}", stylePrompt);

        String imgUrl = generate(sceneRequestVO.getModelInstanceId(), message);
        scene.setThumbnail(imgUrl);
        return scene;
    }

    private String buildCharacterStylePrompt(Long projectId, Long episodeId) {
        String baseStyle = promptContextSupport.buildStylePrompt(projectId, episodeId);
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(baseStyle)) {
            parts.add(baseStyle.trim());
        }
        parts.add("photorealistic live-action Chinese character design, realistic skin texture, realistic human proportions");
        parts.add("strictly preserve the age, body shape, face shape, eyes, hairstyle, clothing, and temperament from the input description");
        parts.add("do not make the character younger, slimmer, sexier, trendier, cartoonish, anime, toy-like, or stylized");
        parts.add("adult realistic human anatomy only, no childlike face, no oversized eyes, no plastic skin");
        parts.add("clothing, hairstyle, physique, and age impression must remain believable in a real-world setting");
        return String.join("; ", parts);
    }

    private String buildSceneStylePrompt(Long projectId, Long episodeId) {
        String baseStyle = promptContextSupport.buildStylePrompt(projectId, episodeId);
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(baseStyle)) {
            parts.add(baseStyle.trim());
        }
        parts.add("cinematic realistic environment, believable lighting, believable materials");
        parts.add("no cartoon, anime, illustration, game-like render, or exaggerated concept-art style");
        return String.join("; ", parts);
    }

    private boolean shouldUseCharacterRegenerateWorkflow(CharactersRequestVO charactersRequestVO, Characters characters) {
        return characters != null
                && StringUtils.hasText(characters.getAvatar())
                && ACTION_REGENERATE.equalsIgnoreCase(charactersRequestVO.getActionType());
    }

    private Map<String, Object> buildCharacterRegenerateParams(Long modelInstanceId) {
        ModelInstance modelInstance = getById(modelInstanceId);
        Map<String, Object> baseParams = modelInstance == null || modelInstance.getParams() == null
                ? Map.of()
                : modelInstance.getParams();
        Map<String, Object> overrides = new LinkedHashMap<>();

        overrides.put("workflowPath", firstNonBlank(
                asText(baseParams.get("regenerateWorkflowPath")),
                DEFAULT_CHARACTER_REGENERATE_WORKFLOW_PATH
        ));

        String apiWorkflowPath = asText(baseParams.get("regenerateApiWorkflowPath"));
        if (StringUtils.hasText(apiWorkflowPath)) {
            overrides.put("apiWorkflowPath", apiWorkflowPath);
        }

        overrides.put("inputMapping", firstNonNull(
                baseParams.get("regenerateInputMapping"),
                defaultCharacterRegenerateInputMapping()
        ));
        overrides.put("outputMapping", firstNonNull(
                baseParams.get("regenerateOutputMapping"),
                Map.of("nodeId", CHARACTER_REGENERATE_OUTPUT_NODE_ID)
        ));

        return overrides;
    }

    private Map<String, Object> defaultCharacterRegenerateInputMapping() {
        Map<String, Object> imageMapping = new LinkedHashMap<>();
        imageMapping.put("nodeId", CHARACTER_REGENERATE_LOAD_IMAGE_NODE_ID);
        imageMapping.put("field", "image");
        imageMapping.put("type", "image");
        imageMapping.put("filenamePrefix", "character-regenerate");

        Map<String, Object> promptMapping = new LinkedHashMap<>();
        promptMapping.put("nodeId", CHARACTER_REGENERATE_PROMPT_NODE_ID);
        promptMapping.put("field", "prompt");
        promptMapping.put("type", "string");

        Map<String, Object> inputMapping = new LinkedHashMap<>();
        inputMapping.put("image", imageMapping);
        inputMapping.put("prompt", promptMapping);
        return inputMapping;
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String asText(Object value) {
        return value == null ? null : Objects.toString(value, null);
    }
}
