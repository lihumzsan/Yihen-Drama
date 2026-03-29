package com.yihen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yihen.entity.ModelInstanceDefault;
import com.yihen.enums.ModelType;
import com.yihen.enums.SceneCode;

import java.util.Map;

public interface ModelInstanceDefaultService extends IService<ModelInstanceDefault> {

    Map<String, ModelInstanceDefault> getAllDefaultUnderType();

    boolean checkExistUnderType(ModelType modelType, SceneCode sceneCode);

    boolean checkIsDefault(Long modelInstanceId);

    ModelInstanceDefault getDefault(ModelType modelType, SceneCode sceneCode);

    void addDefault(ModelInstanceDefault modelInstanceDefault);

    void deleteDefault(Long modelInstanceId, ModelType modelType);

    void updateDefault(ModelInstanceDefault modelInstanceDefault);
}
