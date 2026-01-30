package com.myo.blog.entity.params;

import com.myo.blog.entity.CategoryVo;
import com.myo.blog.entity.TagVo;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
/**
 * Created by IntelliJ IDEA.
 *
 * @Author : myo
 * @create 2023/8/10 15:40
 */
/**
 * 文章参数
 */
@Data
public class ArticleParam {

    private String id;

    private ArticleBodyParam body;

    private CategoryVo category;

    private String summary;

    private List<TagVo> tags;

    private String title;

    private String cover;

    private Integer viewKeys;// 文章可视度 (1:公开, 2:私密, 3:粉丝可见 - 具体定义看业务)
}
