package com.yihen.core.model.impl;

import com.yihen.controller.vo.CharactersRequestVO;
import com.yihen.controller.vo.SceneRequestVO;
import com.yihen.core.model.PropertyGenerateImgModelService;
import com.yihen.entity.Characters;
import com.yihen.entity.PromptTemplate;
import com.yihen.entity.Scene;
import com.yihen.entity.StyleTemplate;
import com.yihen.enums.SceneCode;
import com.yihen.mapper.ProjectMapper;
import com.yihen.service.CharacterService;
import com.yihen.service.PromptTemplateService;
import com.yihen.service.SceneService;
import com.yihen.service.StyleTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PropertyGenerateImgModelServiceImpl extends ImgModelServiceImpl implements PropertyGenerateImgModelService {

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired
    private StyleTemplateService styleTemplateService;

    @Autowired
    private CharacterService characterService;

    @Autowired
    private SceneService sceneService;

    @Autowired
    private ProjectMapper projectMapper;

    @Override
    public Characters generateCharacter(CharactersRequestVO charactersRequestVO) throws Exception {
        Long projectStyleId = projectMapper.getProjectStyleById(charactersRequestVO.getProjectId());
        String stylePrompt = buildCharacterStylePrompt(projectStyleId);

        PromptTemplate promptTemplate = promptTemplateService.getDefaultTemplateBySceneCode(SceneCode.CHARACTER_GEN);
        String message = promptTemplate.getPromptContent()
                .replace("{{input}}", charactersRequestVO.getDescription())
                .replace("{{style_template}}", stylePrompt);

        String imgUrl = generate(charactersRequestVO.getModelInstanceId(), message);

        Characters characters = characterService.getById(charactersRequestVO.getCharacterId());
        characters.setAvatar(imgUrl);
        return characters;
    }

    @Override
    public Scene generateScene(SceneRequestVO sceneRequestVO) throws Exception {
        Long projectStyleId = projectMapper.getProjectStyleById(sceneRequestVO.getProjectId());
        String stylePrompt = buildSceneStylePrompt(projectStyleId);

        PromptTemplate promptTemplate = promptTemplateService.getDefaultTemplateBySceneCode(SceneCode.SCENE_GEN);
        String message = promptTemplate.getPromptContent()
                .replace("{{input}}", sceneRequestVO.getDescription())
                .replace("{{style_template}}", stylePrompt);

        String imgUrl = generate(sceneRequestVO.getModelInstanceId(), message);

        Scene scene = sceneService.getById(sceneRequestVO.getSceneId());
        scene.setThumbnail(imgUrl);
        return scene;
    }

    private String buildCharacterStylePrompt(Long projectStyleId) {
        String baseStyle = getStyleDescription(projectStyleId);
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(baseStyle)) {
            parts.add(baseStyle.trim());
        }
        parts.add("真人写实风格，影视人物设定照质感，真实中国人五官比例与皮肤质感");
        parts.add("必须严格忠实输入中的年龄、体型、脸型、眼睛特征、发型、服装与气质，不得擅自改成更年轻、更瘦、更性感或更时尚的版本");
        parts.add("不得年轻化、瘦身、美型化、网红化、二次元化，不得改变为露脐装、短裤、紧身时装或与描述不符的服饰");
        parts.add("严格禁止卡通风、动漫风、插画风、Q版、3D渲染、玩偶感、游戏角色建模感");
        parts.add("角色必须为成年真人体态与真实面部结构，不得幼态化，不得夸张大眼，不得出现塑料皮肤");
        parts.add("服装、发型、身材与年龄气质保持现实世界可成立的真实效果");
        return String.join("；", parts);
    }

    private String buildSceneStylePrompt(Long projectStyleId) {
        String baseStyle = getStyleDescription(projectStyleId);
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(baseStyle)) {
            parts.add(baseStyle.trim());
        }
        parts.add("电影级真实场景质感，真实光影，真实材质");
        parts.add("禁止卡通风、动漫风、插画风、3D游戏场景感、过度概念设计稿风格");
        return String.join("；", parts);
    }

    private String getStyleDescription(Long projectStyleId) {
        if (projectStyleId == null) {
            return "";
        }
        StyleTemplate styleTemplate = styleTemplateService.getById(projectStyleId);
        return styleTemplate == null ? "" : styleTemplate.getDescription();
    }
}
