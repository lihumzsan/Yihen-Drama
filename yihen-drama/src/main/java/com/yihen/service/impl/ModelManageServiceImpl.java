package com.yihen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yihen.asyn.ModelPersistFacade;
import com.yihen.entity.ModelDefinition;
import com.yihen.entity.ModelInstance;
import com.yihen.entity.ModelInstanceDefault;
import com.yihen.enums.ModelType;
import com.yihen.enums.SceneCode;
import com.yihen.mapper.ModelDefinitionMapper;
import com.yihen.mapper.ModelInstanceMapper;
import com.yihen.service.ModelInstanceDefaultService;
import com.yihen.service.ModelManageService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service("modelManageServiceImpl")
public class ModelManageServiceImpl extends ServiceImpl<ModelDefinitionMapper, ModelDefinition> implements ModelManageService {

    @Autowired
    private ModelInstanceMapper modelInstanceMapper;

    @Autowired
    private ModelInstanceDefaultService modelInstanceDefaultService;

    @Autowired
    private ModelPersistFacade modelPersistFacade;

    @Override
    public void addModelDefinition(ModelDefinition modelDefinition) {
        save(modelDefinition);
    }

    @Override
    public void addModelInstance(ModelInstance modelInstance) {
        checkModelInstance(modelInstance);
        modelInstanceMapper.insert(modelInstance);
        modelPersistFacade.addDefaultModel(modelInstance);
    }

    @Override
    public List<ModelInstance> getModelInstanceByType(Page<ModelInstance> modelInstancePage, ModelType modelType) {
        LambdaQueryWrapper<ModelInstance> queryWrapper = new LambdaQueryWrapper<ModelInstance>()
                .eq(ModelInstance::getModelType, modelType);
        modelInstanceMapper.selectPage(modelInstancePage, queryWrapper);
        List<ModelInstance> modelInstances = modelInstancePage.getRecords();

        if (ObjectUtils.isEmpty(modelInstances)) {
            return new ArrayList<>();
        }

        List<Long> providerIds = modelInstances.stream()
                .map(ModelInstance::getModelDefId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<ModelDefinition> modelDefinitions = listByIds(providerIds);
        Map<Long, ModelDefinition> modelDefinitionMap = modelDefinitions.stream()
                .collect(Collectors.toMap(ModelDefinition::getId, item -> item));
        modelInstances.forEach(modelInstance -> {
            ModelDefinition modelDefinition = modelDefinitionMap.get(modelInstance.getModelDefId());
            if (modelDefinition == null && modelInstance.getModelDefId() != null) {
                modelDefinition = getById(modelInstance.getModelDefId());
            }
            if (modelDefinition != null) {
                modelInstance.setProviderCode(modelDefinition.getProviderCode() != null
                        ? modelDefinition.getProviderCode().toLowerCase()
                        : null);
                modelInstance.setBaseUrl(modelDefinition.getBaseUrl());
            } else {
                modelInstance.setProviderCode(null);
                modelInstance.setBaseUrl(null);
            }
        });

        return modelInstances;
    }

    @Override
    public ModelInstance getDefaultModelInstanceByType(ModelType modelType, SceneCode sceneCode) {
        ModelInstanceDefault defaultInstance = modelInstanceDefaultService.getDefault(modelType, sceneCode);
        ModelInstance modelInstance = attachProviderInfo(
                defaultInstance == null ? null : modelInstanceMapper.selectById(defaultInstance.getModelInstanceId())
        );
        if (modelInstance != null) {
            return modelInstance;
        }

        if (sceneCode != null) {
            ModelInstance sceneMatched = attachProviderInfo(getFirstEnabledModelInstance(modelType, sceneCode));
            if (sceneMatched != null) {
                return sceneMatched;
            }
        }

        ModelInstanceDefault fallbackDefault = sceneCode == null ? null : modelInstanceDefaultService.getDefault(modelType, null);
        modelInstance = attachProviderInfo(
                fallbackDefault == null ? null : modelInstanceMapper.selectById(fallbackDefault.getModelInstanceId())
        );
        if (modelInstance != null) {
            return modelInstance;
        }

        return attachProviderInfo(getFirstEnabledModelInstance(modelType, null));
    }

    @Override
    public void updateModelDefinition(ModelDefinition modelDefinition) {
        if (modelDefinition.getId() == null) {
            throw new RuntimeException("鍘傚晢ID涓嶈兘涓虹┖");
        }
        updateById(modelDefinition);
    }

    @Override
    public ModelInstance getModelInstanceById(Long id) {
        return modelInstanceMapper.selectById(id);
    }

    @Override
    public String getBaseUrlById(Long id) {
        return getBaseMapper().getBaseUrlById(id);
    }

    @Override
    public List<ModelDefinition> getModelDefinition(Page<ModelDefinition> modelDefinitionPage) {
        page(modelDefinitionPage);
        return modelDefinitionPage.getRecords();
    }

    @Override
    public void testModelInstanceConnectivity(Long id) {
    }

    @Override
    public void deleteModelInstance(Long id) {
        boolean isDefault = modelInstanceDefaultService.checkIsDefault(id);
        if (isDefault) {
            throw new RuntimeException("榛樿妯″瀷涓嶅彲鍒犻櫎");
        }
        modelInstanceMapper.deleteById(id);
    }

    @Override
    public void deleteModelDefinition(Long id) {
        Long count = modelInstanceMapper.selectCount(
                new LambdaQueryWrapper<ModelInstance>().eq(ModelInstance::getModelDefId, id)
        );

        if (count > 0) {
            throw new RuntimeException("璇ュ巶鍟嗕笅鏈夋ā鍨嬪疄渚嬶紝璇峰厛鍒犻櫎妯″瀷瀹炰緥");
        }
        removeById(id);
    }

    @Override
    public void updateModelInstance(ModelInstance modelInstance) {
        modelInstanceMapper.updateById(modelInstance);
    }

    private void checkModelInstance(ModelInstance modelInstance) {
        if (ObjectUtils.isEmpty(modelInstance.getModelType())) {
            throw new RuntimeException("妯″瀷绫诲瀷涓嶈兘涓虹┖");
        }
        if (ObjectUtils.isEmpty(modelInstance.getModelCode())) {
            throw new RuntimeException("妯″瀷缂栫爜涓嶈兘涓虹┖");
        }
        if (ObjectUtils.isEmpty(modelInstance.getModelDefId())) {
            throw new RuntimeException("鍘傚晢瀹氫箟ID涓嶈兘涓虹┖");
        }
        ModelDefinition modelDefinition = getById(modelInstance.getModelDefId());
        boolean requiresApiKey = modelDefinition == null
                || !"comfyui".equalsIgnoreCase(modelDefinition.getProviderCode());
        if (requiresApiKey && ObjectUtils.isEmpty(modelInstance.getApiKey())) {
            throw new RuntimeException("apiKey涓嶈兘涓虹┖");
        }

        if (ObjectUtils.isEmpty(modelInstance.getInstanceName())) {
            modelInstance.setInstanceName(modelInstance.getModelCode());
        }
    }

    private ModelInstance getFirstEnabledModelInstance(ModelType modelType, SceneCode sceneCode) {
        LambdaQueryWrapper<ModelInstance> queryWrapper = new LambdaQueryWrapper<ModelInstance>()
                .eq(ModelInstance::getModelType, modelType)
                .eq(ModelInstance::getStatus, (byte) 1)
                .orderByDesc(ModelInstance::getUpdateTime)
                .last("LIMIT 1");
        if (sceneCode != null) {
            queryWrapper.eq(ModelInstance::getSceneCode, sceneCode);
        }
        return modelInstanceMapper.selectOne(queryWrapper);
    }

    private ModelInstance attachProviderInfo(ModelInstance modelInstance) {
        if (modelInstance == null) {
            return null;
        }
        ModelDefinition modelDefinition = getById(modelInstance.getModelDefId());
        if (modelDefinition != null) {
            modelInstance.setProviderCode(modelDefinition.getProviderCode() != null
                    ? modelDefinition.getProviderCode().toLowerCase()
                    : null);
            modelInstance.setBaseUrl(modelDefinition.getBaseUrl());
        }
        return modelInstance;
    }
}
