package com.yihen.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yihen.entity.ModelDefinition;
import com.yihen.entity.ModelInstance;
import com.yihen.enums.ModelType;
import com.yihen.enums.SceneCode;

import java.util.List;

public interface ModelManageService extends IService<ModelDefinition> {

    void addModelDefinition(ModelDefinition modelDefinition);

    void addModelInstance(ModelInstance modelInstance);

    List<ModelInstance> getModelInstanceByType(Page<ModelInstance> modelInstancePage, ModelType modelType);

    void testModelInstanceConnectivity(Long id);

    void deleteModelInstance(Long id);

    void deleteModelDefinition(Long id);

    void updateModelInstance(ModelInstance modelInstance);

    ModelInstance getDefaultModelInstanceByType(ModelType modelType, SceneCode sceneCode);

    void updateModelDefinition(ModelDefinition modelDefinition);

    ModelInstance getModelInstanceById(Long id);

    String getBaseUrlById(Long id);

    List<ModelDefinition> getModelDefinition(Page<ModelDefinition> modelDefinitionPage);
}
