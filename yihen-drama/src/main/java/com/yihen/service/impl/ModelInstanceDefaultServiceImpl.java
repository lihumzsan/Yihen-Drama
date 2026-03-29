package com.yihen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yihen.entity.ModelInstanceDefault;
import com.yihen.enums.ModelType;
import com.yihen.enums.SceneCode;
import com.yihen.mapper.ModelInstanceDefaultMapper;
import com.yihen.service.ModelInstanceDefaultService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.stream.Collectors;

@Service("modelInstanceDefaultServiceImpl")
public class ModelInstanceDefaultServiceImpl extends ServiceImpl<ModelInstanceDefaultMapper, ModelInstanceDefault>
        implements ModelInstanceDefaultService {

    @Override
    public Map<String, ModelInstanceDefault> getAllDefaultUnderType() {
        return list().stream()
                .peek(this::hydrateSceneCode)
                .collect(Collectors.toMap(this::buildKey, item -> item, (left, right) -> right));
    }

    @Override
    public boolean checkExistUnderType(ModelType modelType, SceneCode sceneCode) {
        return getDefault(modelType, sceneCode) != null;
    }

    @Override
    public boolean checkIsDefault(Long modelInstanceId) {
        LambdaQueryWrapper<ModelInstanceDefault> queryWrapper = new LambdaQueryWrapper<ModelInstanceDefault>()
                .eq(ModelInstanceDefault::getModelInstanceId, modelInstanceId);
        return count(queryWrapper) > 0;
    }

    @Override
    public ModelInstanceDefault getDefault(ModelType modelType, SceneCode sceneCode) {
        LambdaQueryWrapper<ModelInstanceDefault> queryWrapper = new LambdaQueryWrapper<ModelInstanceDefault>()
                .eq(ModelInstanceDefault::getModelType, modelType)
                .eq(ModelInstanceDefault::getStatus, (byte) 1)
                .last("LIMIT 1");
        if (sceneCode == null) {
            queryWrapper.and(wrapper -> wrapper.isNull(ModelInstanceDefault::getRemark)
                    .or()
                    .eq(ModelInstanceDefault::getRemark, ""));
        } else {
            queryWrapper.eq(ModelInstanceDefault::getRemark, sceneCode.getKey());
        }

        ModelInstanceDefault modelInstanceDefault = getOne(queryWrapper, false);
        if (modelInstanceDefault != null) {
            hydrateSceneCode(modelInstanceDefault);
        }
        return modelInstanceDefault;
    }

    @Override
    public void addDefault(ModelInstanceDefault modelInstanceDefault) {
        if (checkExistUnderType(modelInstanceDefault.getModelType(), modelInstanceDefault.getSceneCode())) {
            throw new RuntimeException("璇ユā鍨嬬被鍨嬪凡瀛樺湪榛樿瀹炰緥");
        }

        syncRemark(modelInstanceDefault);
        save(modelInstanceDefault);
    }

    @Override
    public void deleteDefault(Long modelInstanceId, ModelType modelType) {
        LambdaQueryWrapper<ModelInstanceDefault> queryWrapper = new LambdaQueryWrapper<ModelInstanceDefault>()
                .eq(ModelInstanceDefault::getModelInstanceId, modelInstanceId);
        remove(queryWrapper);
    }

    @Override
    public void updateDefault(ModelInstanceDefault modelInstanceDefault) {
        if (!checkExistUnderType(modelInstanceDefault.getModelType(), modelInstanceDefault.getSceneCode())) {
            addDefault(modelInstanceDefault);
            return;
        }

        syncRemark(modelInstanceDefault);
        LambdaUpdateWrapper<ModelInstanceDefault> updateWrapper = new LambdaUpdateWrapper<ModelInstanceDefault>()
                .eq(ModelInstanceDefault::getModelType, modelInstanceDefault.getModelType());
        if (modelInstanceDefault.getSceneCode() == null) {
            updateWrapper.and(wrapper -> wrapper.isNull(ModelInstanceDefault::getRemark)
                    .or()
                    .eq(ModelInstanceDefault::getRemark, ""));
        } else {
            updateWrapper.eq(ModelInstanceDefault::getRemark, modelInstanceDefault.getSceneCode().getKey());
        }
        updateWrapper
                .set(ModelInstanceDefault::getModelInstanceId, modelInstanceDefault.getModelInstanceId())
                .set(ModelInstanceDefault::getRemark, modelInstanceDefault.getRemark());

        update(updateWrapper);
    }

    private void syncRemark(ModelInstanceDefault modelInstanceDefault) {
        modelInstanceDefault.setRemark(modelInstanceDefault.getSceneCode() == null
                ? null
                : modelInstanceDefault.getSceneCode().getKey());
    }

    private void hydrateSceneCode(ModelInstanceDefault modelInstanceDefault) {
        if (modelInstanceDefault == null || !StringUtils.hasText(modelInstanceDefault.getRemark())) {
            return;
        }
        modelInstanceDefault.setSceneCode(SceneCode.fromKey(modelInstanceDefault.getRemark()));
    }

    private String buildKey(ModelInstanceDefault modelInstanceDefault) {
        String typeKey = modelInstanceDefault.getModelType() == null ? "" : modelInstanceDefault.getModelType().getKey();
        String sceneKey = modelInstanceDefault.getSceneCode() == null ? "" : modelInstanceDefault.getSceneCode().getKey();
        return StringUtils.hasText(sceneKey) ? typeKey + ":" + sceneKey : typeKey;
    }
}
