-- 用户头像：客户端压缩后的图片，以 data URL 文本（data:image/...;base64,...）存储
-- 服务端不做图像处理/外链，仅校验类型与体积，避免引入文件存储依赖
ALTER TABLE app_user ADD COLUMN avatar_image CLOB;
