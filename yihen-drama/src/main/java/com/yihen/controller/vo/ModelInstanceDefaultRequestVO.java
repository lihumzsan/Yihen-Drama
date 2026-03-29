package com.yihen.controller.vo;

import com.yihen.enums.ModelType;
import com.yihen.enums.SceneCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "榛樿妯″瀷瀹炰緥璇锋眰鍙傛暟")
public class ModelInstanceDefaultRequestVO {
    @Schema(description = "妯″瀷绫诲瀷 TEXT/IMAGE")
    private ModelType modelType;

    @Schema(description = "妯″瀷瀹炰緥ID")
    private Long modelInstanceId;

    @Schema(description = "妯″瀷鍦烘櫙")
    private SceneCode sceneCode;
}
