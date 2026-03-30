ALTER TABLE project
    ADD COLUMN IF NOT EXISTS global_style_prompt TEXT NULL COMMENT '项目级全局风格定义' AFTER style_id;

ALTER TABLE episode
    ADD COLUMN IF NOT EXISTS visual_setting TEXT NULL COMMENT '章节视觉设定' AFTER abstraction;
