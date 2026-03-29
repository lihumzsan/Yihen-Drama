package com.yihen.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yihen.common.Result;
import com.yihen.controller.vo.ModelDefinitionRequestVO;
import com.yihen.controller.vo.ModelInstanceDefaultRequestVO;
import com.yihen.controller.vo.ModelInstanceRequestVO;
import com.yihen.entity.ModelDefinition;
import com.yihen.entity.ModelInstance;
import com.yihen.entity.ModelInstanceDefault;
import com.yihen.enums.ModelType;
import com.yihen.enums.SceneCode;
import com.yihen.service.ModelInstanceDefaultService;
import com.yihen.service.ModelManageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/models")
@Tag(name = "Model Manage", description = "Manage model definitions, instances, and defaults")
public class ModelManageController {

    @Resource
    private ModelManageService modelManageService;

    @Autowired
    private ModelInstanceDefaultService modelInstanceDefaultService;

    @GetMapping("/model-definition/list")
    @Operation(summary = "List model definitions")
    public Result<Page<ModelDefinition>> listModelDefinitions(
            @Parameter(description = "Page number", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "6") Integer size
    ) {
        Page<ModelDefinition> modelDefinitionPage = new Page<>(page, size);
        modelManageService.getModelDefinition(modelDefinitionPage);
        return Result.success(modelDefinitionPage);
    }

    @GetMapping("/model-definition/{id}")
    @Operation(summary = "Get model definition")
    public Result<ModelDefinition> getModelDefinition(@PathVariable Long id) {
        return Result.success(modelManageService.getById(id));
    }

    @GetMapping("/model-instance-by-type/{modelType}")
    @Operation(summary = "List model instances by type")
    public Result<Page<ModelInstance>> getModelInstanceByType(
            @Parameter(description = "Page number", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "6") Integer size,
            @Parameter(description = "Model type", example = "TEXT") @PathVariable ModelType modelType
    ) {
        Page<ModelInstance> modelInstancePage = new Page<>(page, size);
        modelManageService.getModelInstanceByType(modelInstancePage, modelType);
        return Result.success(modelInstancePage);
    }

    @PostMapping("/add-model-definition")
    @Operation(summary = "Add model definition")
    public Result<Void> addModelDefinition(@RequestBody ModelDefinitionRequestVO modelDefinitionRequestVO) {
        ModelDefinition modelDefinition = new ModelDefinition();
        BeanUtils.copyProperties(modelDefinitionRequestVO, modelDefinition);
        modelManageService.addModelDefinition(modelDefinition);
        return Result.success("添加成功");
    }

    @PutMapping("/model-definition")
    @Operation(summary = "Update model definition")
    public Result<Void> updateModelDefinition(@RequestBody ModelDefinitionRequestVO modelDefinitionRequestVO) {
        ModelDefinition modelDefinition = new ModelDefinition();
        BeanUtils.copyProperties(modelDefinitionRequestVO, modelDefinition);
        modelManageService.updateModelDefinition(modelDefinition);
        return Result.success("更新成功");
    }

    @DeleteMapping("/model-definition/{id}")
    @Operation(summary = "Delete model definition")
    public Result<Void> deleteModelDefinition(@PathVariable Long id) {
        modelManageService.deleteModelDefinition(id);
        return Result.success("删除成功");
    }

    @DeleteMapping("/model-instance/{id}")
    @Operation(summary = "Delete model instance")
    public Result<Void> deleteModelInstance(@PathVariable Long id) {
        modelManageService.deleteModelInstance(id);
        return Result.success("删除成功");
    }

    @PostMapping("/update-model-instance")
    @Operation(summary = "Update model instance")
    public Result<Void> updateModelInstance(@RequestBody ModelInstanceRequestVO modelInstanceRequestVO) {
        ModelInstance modelInstance = new ModelInstance();
        BeanUtils.copyProperties(modelInstanceRequestVO, modelInstance);
        modelManageService.updateModelInstance(modelInstance);
        return Result.success("更新成功");
    }

    @PostMapping("/add-model-instance")
    @Operation(summary = "Add model instance")
    public Result<Void> addModelInstance(@RequestBody ModelInstanceRequestVO modelInstanceRequestVO) {
        ModelInstance modelInstance = new ModelInstance();
        BeanUtils.copyProperties(modelInstanceRequestVO, modelInstance);
        modelManageService.addModelInstance(modelInstance);
        return Result.success("添加成功");
    }

    @GetMapping("/default-model-instance")
    @Operation(summary = "Get all default model instances")
    public Result<Map<String, ModelInstanceDefault>> getDefaultModelInstance() {
        return Result.success(modelInstanceDefaultService.getAllDefaultUnderType());
    }

    @GetMapping("/default-model-instance-by-type/{modelType}")
    @Operation(summary = "Get default model instance by type")
    public Result<ModelInstance> getDefaultModelInstanceByType(
            @PathVariable ModelType modelType,
            @RequestParam(required = false) SceneCode sceneCode
    ) {
        return Result.success(modelManageService.getDefaultModelInstanceByType(modelType, sceneCode));
    }

    @PostMapping("/add-default-model-instance")
    @Operation(summary = "Add default model instance")
    public Result<Void> addDefaultModelInstance(@RequestBody ModelInstanceDefaultRequestVO modelInstanceDefaultRequestVO) {
        ModelInstanceDefault modelInstanceDefault = new ModelInstanceDefault();
        BeanUtils.copyProperties(modelInstanceDefaultRequestVO, modelInstanceDefault);
        modelInstanceDefaultService.addDefault(modelInstanceDefault);
        return Result.success("添加成功");
    }

    @PutMapping("/update-default-model-instance")
    @Operation(summary = "Update default model instance")
    public Result<Void> updateDefaultModelInstance(@RequestBody ModelInstanceDefaultRequestVO modelInstanceDefaultRequestVO) {
        ModelInstanceDefault modelInstanceDefault = new ModelInstanceDefault();
        BeanUtils.copyProperties(modelInstanceDefaultRequestVO, modelInstanceDefault);
        modelInstanceDefaultService.updateDefault(modelInstanceDefault);
        return Result.success("更新成功");
    }
}
