package com.yihen.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yihen.enums.ModelType;
import com.yihen.enums.SceneCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("model_instance_default ")
@Schema(description = "鍚勭被鍨嬩笅榛樿鐨勬ā鍨嬪疄渚嬭〃")
public class ModelInstanceDefault extends BaseEntity {

    @Schema(description = "妯″瀷绫诲瀷 TEXT/IMAGE")
    private ModelType modelType;

    @Schema(description = "妯″瀷瀹炰緥ID")
    private Long modelInstanceId;

    @Schema(description = "鐘舵€?, example = \"1-鍚敤锛?-绂佺敤\"")
    private Byte status;

    @Schema(description = "澶囨敞璇存槑")
    private String remark;

    @TableField(exist = false)
    @Schema(description = "榛樿妯″瀷瀵瑰簲鍦烘櫙")
    private SceneCode sceneCode;
}
