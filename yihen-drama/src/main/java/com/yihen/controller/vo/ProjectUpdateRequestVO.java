package com.yihen.controller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "项目信息请求参数")
public class ProjectUpdateRequestVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "项目名称", example = "霸道总裁爱上我")
    private String name;

    @Schema(description = "项目描述")
    private String description;

    @Schema(description = "封面URL")
    private String cover;

    @Schema(description = "风格ID")
    private Long style;

    @Schema(description = "项目级全局风格定义")
    private String globalStylePrompt;
}
