package com.yihen.core.model.support;

import com.yihen.entity.Episode;
import com.yihen.entity.Project;
import com.yihen.entity.StyleTemplate;
import com.yihen.mapper.EpisodeMapper;
import com.yihen.mapper.ProjectMapper;
import com.yihen.service.StyleTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PromptContextSupport {

    private final ProjectMapper projectMapper;
    private final EpisodeMapper episodeMapper;
    private final StyleTemplateService styleTemplateService;

    public String applyContext(String template, Long projectId, Long episodeId) {
        String message = safeText(template);
        PromptContext context = buildContext(projectId, episodeId);
        boolean hasPlaceholder = hasContextPlaceholder(message);

        message = message
                .replace("{{style_template}}", context.stylePrompt())
                .replace("{{project_constraints}}", context.instructionPrompt())
                .replace("{{project_description}}", context.projectDescription())
                .replace("{{project_style_name}}", context.styleName())
                .replace("{{project_consistency}}", context.consistencyPrompt())
                .replace("{{project_global_style}}", context.globalStylePrompt())
                .replace("{{episode_visual_setting}}", context.episodeVisualSetting());

        message = clearResidualPlaceholders(message);

        if (!hasPlaceholder && StringUtils.hasText(context.instructionPrompt())) {
            message = injectInstructionBlock(message, context.instructionPrompt());
        }
        return message;
    }

    public String buildStylePrompt(Long projectId, Long episodeId) {
        return buildContext(projectId, episodeId).stylePrompt();
    }

    public PromptContext buildContext(Long projectId, Long episodeId) {
        if (projectId == null) {
            return PromptContext.empty();
        }

        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            return PromptContext.empty();
        }

        Episode episode = episodeId == null ? null : episodeMapper.selectById(episodeId);
        StyleTemplate styleTemplate = project.getStyleId() == null
                ? null
                : styleTemplateService.getById(project.getStyleId());

        String styleName = safeText(styleTemplate == null ? null : styleTemplate.getName()).trim();
        String styleDescription = safeText(styleTemplate == null ? null : styleTemplate.getDescription()).trim();
        String projectDescription = safeText(project.getDescription()).trim();
        String globalStylePrompt = safeText(project.getGlobalStylePrompt()).trim();
        String episodeVisualSetting = safeText(episode == null ? null : episode.getVisualSetting()).trim();
        String consistencyPrompt = buildConsistencyPrompt(project.getConsistency());

        List<String> stylePromptParts = new ArrayList<>();
        if (StringUtils.hasText(styleName)) {
            stylePromptParts.add("Overall visual style: " + styleName);
        }
        if (StringUtils.hasText(styleDescription)) {
            stylePromptParts.add(styleDescription);
        }
        if (StringUtils.hasText(globalStylePrompt)) {
            stylePromptParts.add("Project-wide visual bible: " + globalStylePrompt);
        }
        if (StringUtils.hasText(episodeVisualSetting)) {
            stylePromptParts.add("Episode-specific visual setting: " + episodeVisualSetting);
        }
        if (StringUtils.hasText(projectDescription)) {
            stylePromptParts.add("Project description constraints: " + projectDescription);
        }
        if (StringUtils.hasText(consistencyPrompt)) {
            stylePromptParts.add(consistencyPrompt);
        }

        List<String> instructionLines = new ArrayList<>();
        if (StringUtils.hasText(styleName)) {
            instructionLines.add("- Keep the overall visual style aligned with " + styleName);
        }
        if (StringUtils.hasText(styleDescription)) {
            instructionLines.add("- Style details: " + styleDescription);
        }
        if (StringUtils.hasText(globalStylePrompt)) {
            instructionLines.add("- Project-wide visual bible: " + globalStylePrompt);
        }
        if (StringUtils.hasText(episodeVisualSetting)) {
            instructionLines.add("- Episode-specific visual setting: " + episodeVisualSetting);
        }
        if (StringUtils.hasText(projectDescription)) {
            instructionLines.add("- Project description constraints: " + projectDescription);
        }
        if (StringUtils.hasText(consistencyPrompt)) {
            instructionLines.add("- Consistency target: " + consistencyPrompt);
        }

        return new PromptContext(
                styleName,
                projectDescription,
                globalStylePrompt,
                episodeVisualSetting,
                consistencyPrompt,
                String.join("; ", stylePromptParts),
                String.join("\n", instructionLines)
        );
    }

    private String buildConsistencyPrompt(Integer consistency) {
        if (consistency == null || consistency <= 0) {
            return "";
        }
        return "Maintain cross-shot consistency for character appearance, environments, and overall visual language. Target strength: "
                + consistency + "%";
    }

    private boolean hasContextPlaceholder(String template) {
        return template.contains("{{style_template}}")
                || template.contains("{{project_constraints}}")
                || template.contains("{{project_description}}")
                || template.contains("{{project_style_name}}")
                || template.contains("{{project_consistency}}")
                || template.contains("{{project_global_style}}")
                || template.contains("{{episode_visual_setting}}");
    }

    private String clearResidualPlaceholders(String template) {
        return template
                .replace("{{style_template}}", "")
                .replace("{{project_constraints}}", "")
                .replace("{{project_description}}", "")
                .replace("{{project_style_name}}", "")
                .replace("{{project_consistency}}", "")
                .replace("{{project_global_style}}", "")
                .replace("{{episode_visual_setting}}", "");
    }

    private String injectInstructionBlock(String template, String instructionPrompt) {
        String block = "[Project and episode visual constraints that must be preserved]\n"
                + "- The following constraints must be integrated into the final prompt body and must not be omitted:\n"
                + instructionPrompt;
        String[] markers = {
                "【现在开始生成】",
                "【现在开始】",
                "【现在开始输出】",
                "【现在开始创作】"
        };
        for (String marker : markers) {
            int index = template.indexOf(marker);
            if (index >= 0) {
                return template.substring(0, index).trim() + "\n\n" + block + "\n\n" + template.substring(index);
            }
        }
        return template + "\n\n" + block;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    public record PromptContext(
            String styleName,
            String projectDescription,
            String globalStylePrompt,
            String episodeVisualSetting,
            String consistencyPrompt,
            String stylePrompt,
            String instructionPrompt
    ) {
        public static PromptContext empty() {
            return new PromptContext("", "", "", "", "", "", "");
        }
    }
}
