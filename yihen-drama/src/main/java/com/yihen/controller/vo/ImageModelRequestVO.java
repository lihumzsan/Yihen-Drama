package com.yihen.controller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "图像请求参数")
public class ImageModelRequestVO {

    @Schema(description = "模型实例id")
    private Long modelInstanceId;

    @Schema(description = "项目Id", example = "1")
    private Long projectId;

    @Schema(description = "描述", example = "女，25岁，职场新人")
    private String description;

    @Schema(description = "对象")
    private Object object;

    @Schema(description = "覆盖模型参数")
    private Map<String, Object> paramsOverride;
}
