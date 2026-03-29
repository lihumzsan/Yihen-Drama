package com.yihen.service.impl.decorator;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yihen.constant.model.ModelInstanceDefaultRedisConstant;
import com.yihen.entity.ModelInstanceDefault;
import com.yihen.enums.ModelType;
import com.yihen.enums.SceneCode;
import com.yihen.mapper.ModelInstanceDefaultMapper;
import com.yihen.service.ModelInstanceDefaultService;
import com.yihen.util.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Primary
@Slf4j
public class ModelInstanceDefaultServiceDecorator extends ServiceImpl<ModelInstanceDefaultMapper, ModelInstanceDefault>
        implements ModelInstanceDefaultService {

    private final ModelInstanceDefaultService modelInstanceDefaultService;
    private final RedisUtils redisUtils;

    public ModelInstanceDefaultServiceDecorator(
            @Qualifier("modelInstanceDefaultServiceImpl") ModelInstanceDefaultService modelInstanceDefaultService,
            RedisUtils redisUtils
    ) {
        this.modelInstanceDefaultService = modelInstanceDefaultService;
        this.redisUtils = redisUtils;
    }

    @Override
    public Map<String, ModelInstanceDefault> getAllDefaultUnderType() {
        Map<String, ModelInstanceDefault> map =
                (Map<String, ModelInstanceDefault>) redisUtils.get(ModelInstanceDefaultRedisConstant.MODEL_INSTANCE_DEFAULT_KEY);
        if (ObjectUtils.isEmpty(map)) {
            map = modelInstanceDefaultService.getAllDefaultUnderType();
            redisUtils.putHash(ModelInstanceDefaultRedisConstant.MODEL_INSTANCE_DEFAULT_KEY, map, 1, TimeUnit.DAYS);
        }
        return map;
    }

    @Override
    public boolean checkExistUnderType(ModelType modelType, SceneCode sceneCode) {
        return modelInstanceDefaultService.checkExistUnderType(modelType, sceneCode);
    }

    @Override
    public boolean checkIsDefault(Long modelInstanceId) {
        return modelInstanceDefaultService.checkIsDefault(modelInstanceId);
    }

    @Override
    public ModelInstanceDefault getDefault(ModelType modelType, SceneCode sceneCode) {
        return modelInstanceDefaultService.getDefault(modelType, sceneCode);
    }

    @Override
    public void addDefault(ModelInstanceDefault modelInstanceDefault) {
        modelInstanceDefaultService.addDefault(modelInstanceDefault);
        redisUtils.delete(ModelInstanceDefaultRedisConstant.MODEL_INSTANCE_DEFAULT_KEY);
    }

    @Override
    public void deleteDefault(Long modelInstanceId, ModelType modelType) {
        modelInstanceDefaultService.deleteDefault(modelInstanceId, modelType);
        redisUtils.delete(ModelInstanceDefaultRedisConstant.MODEL_INSTANCE_DEFAULT_KEY);
    }

    @Override
    public void updateDefault(ModelInstanceDefault modelInstanceDefault) {
        modelInstanceDefaultService.updateDefault(modelInstanceDefault);
        redisUtils.delete(ModelInstanceDefaultRedisConstant.MODEL_INSTANCE_DEFAULT_KEY);
    }
}
