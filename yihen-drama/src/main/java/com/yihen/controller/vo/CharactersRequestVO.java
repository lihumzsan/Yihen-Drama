package com.yihen.controller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "角色请求参数")
public class CharactersRequestVO {

    @Schema(description = "模型实例id")
    private Long modelInstanceId;

    @Schema(description = "角色Id")
    private Long characterId;

    @Schema(description = "项目Id", example = "1")
    private Long ProjectId;

    @Schema(description = "角色描述", example = "女，25岁，职场新人")
    private String description;

    @Schema(description = "生成动作 generate/regenerate", example = "generate")
    private String actionType;
}
