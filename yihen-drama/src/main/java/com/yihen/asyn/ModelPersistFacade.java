package com.yihen.asyn;

import com.yihen.entity.ModelInstance;
import com.yihen.entity.ModelInstanceDefault;
import com.yihen.service.ModelInstanceDefaultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModelPersistFacade {

    @Autowired
    private ModelInstanceDefaultService modelInstanceDefaultService;

    @Async("modelExecutor")
    @Transactional(rollbackFor = Exception.class)
    public void addDefaultModel(ModelInstance modelInstance) {
        boolean checkedExistUnderType = modelInstanceDefaultService.checkExistUnderType(
                modelInstance.getModelType(),
                modelInstance.getSceneCode()
        );

        if (!checkedExistUnderType) {
            ModelInstanceDefault modelInstanceDefault = new ModelInstanceDefault();
            modelInstanceDefault.setModelInstanceId(modelInstance.getId());
            modelInstanceDefault.setModelType(modelInstance.getModelType());
            modelInstanceDefault.setSceneCode(modelInstance.getSceneCode());
            modelInstanceDefaultService.addDefault(modelInstanceDefault);
        }
    }
}
