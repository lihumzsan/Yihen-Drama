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
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service("modelManageServiceImpl")
public class ModelManageServiceImpl extends ServiceImpl<ModelDefinitionMapper, ModelDefinition> implements ModelManageService {

    private static final Set<String> WORKFLOW_ROOT_SEGMENTS = Set.of("baseimage", "basevideo", "tool");

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
        normalizeWorkflowParams(modelInstance);
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
        normalizeWorkflowParams(modelInstance);
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

    private void normalizeWorkflowParams(ModelInstance modelInstance) {
        if (modelInstance == null || modelInstance.getParams() == null) {
            return;
        }
        if (modelInstance.getModelType() != ModelType.IMAGE && modelInstance.getModelType() != ModelType.VIDEO) {
            return;
        }

        Map<String, Object> normalizedParams = new LinkedHashMap<>(modelInstance.getParams());
        normalizeWorkflowReference(normalizedParams, "workflowPath", "workflowGroup", "workflowName");
        if (modelInstance.getModelType() == ModelType.IMAGE) {
            normalizeWorkflowReference(
                    normalizedParams,
                    "regenerateWorkflowPath",
                    "regenerateWorkflowGroup",
                    "regenerateWorkflowName"
            );
        }
        modelInstance.setParams(normalizedParams);
    }

    private void normalizeWorkflowReference(
            Map<String, Object> params,
            String pathKey,
            String groupKey,
            String nameKey
    ) {
        String workflowPath = asText(params.get(pathKey));
        if (!StringUtils.hasText(workflowPath)) {
            return;
        }

        WorkflowReference reference = parseWorkflowReference(workflowPath);
        if (StringUtils.hasText(reference.group())) {
            params.put(groupKey, reference.group());
        }
        if (StringUtils.hasText(reference.name())) {
            params.put(nameKey, reference.name());
        }
    }

    private WorkflowReference parseWorkflowReference(String workflowPath) {
        List<String> segments = extractWorkflowSegments(workflowPath);
        if (segments.isEmpty()) {
            return new WorkflowReference("", "");
        }

        List<String> directorySegments = segments.subList(0, Math.max(segments.size() - 1, 0));
        int rootIndex = findWorkflowRootIndex(directorySegments);
        List<String> groupSegments = rootIndex >= 0
                ? directorySegments.subList(rootIndex + 1, directorySegments.size())
                : directorySegments;

        return new WorkflowReference(
                String.join(" / ", groupSegments),
                stripJsonExtension(segments.get(segments.size() - 1))
        );
    }

    private List<String> extractWorkflowSegments(String workflowPath) {
        if (!StringUtils.hasText(workflowPath)) {
            return List.of();
        }
        return Arrays.stream(workflowPath.replace('\\', '/').split("/"))
                .filter(StringUtils::hasText)
                .toList();
    }

    private int findWorkflowRootIndex(List<String> segments) {
        for (int index = segments.size() - 1; index >= 0; index--) {
            if (WORKFLOW_ROOT_SEGMENTS.contains(segments.get(index).toLowerCase(Locale.ROOT))) {
                return index;
            }
        }
        return -1;
    }

    private String stripJsonExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        return filename.replaceFirst("(?i)\\.json$", "");
    }

    private String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private record WorkflowReference(String group, String name) {
    }
}
