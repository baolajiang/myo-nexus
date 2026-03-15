package com.myo.blog.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myo.blog.dao.pojo.Attachment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 附件 Mapper
 * 继承 BaseMapper，提供基础的 CRUD 操作
 * 复杂查询通过 Service 层用 LambdaQueryWrapper 构建，无需自定义 XML
 */
@Mapper
public interface AttachmentMapper extends BaseMapper<Attachment> {
}