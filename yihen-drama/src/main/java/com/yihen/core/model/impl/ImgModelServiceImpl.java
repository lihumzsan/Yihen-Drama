package com.yihen.core.model.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yihen.controller.vo.ImageModelRequestVO;
import com.yihen.core.model.ImgModelService;
import com.yihen.core.model.strategy.image.ImageModelFactory;
import com.yihen.core.model.strategy.image.ImageModelStrategy;
import com.yihen.entity.ModelInstance;
import com.yihen.mapper.ModelInstanceMapper;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ImgModelServiceImpl extends ServiceImpl<ModelInstanceMapper, ModelInstance> implements ImgModelService {

    @Autowired
    private ImageModelFactory imageModelFactory;

    @Override
    public String generate(Long modelId, String text) throws Exception {
        if (ObjectUtils.isEmpty(modelId)) {
            throw new IllegalArgumentException("modelId cannot be empty");
        }
        ImageModelRequestVO requestVO = new ImageModelRequestVO();
        requestVO.setModelInstanceId(modelId);
        requestVO.setDescription(text);
        return generate(requestVO);
    }

    protected String generate(ImageModelRequestVO requestVO) throws Exception {
        if (requestVO == null || ObjectUtils.isEmpty(requestVO.getModelInstanceId())) {
            throw new IllegalArgumentException("modelId cannot be empty");
        }
        ImageModelStrategy strategy = imageModelFactory.getStrategy(requestVO.getModelInstanceId());
        return strategy.create(requestVO);
    }
}
