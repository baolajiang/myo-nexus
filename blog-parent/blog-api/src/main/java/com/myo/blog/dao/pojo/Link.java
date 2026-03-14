package com.myo.blog.dao.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author : myo
 * @create 2023/7/29 14:23
 */
@Data
public class Link {
    /***
     * id
     * */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    /***
     * 连接名字
     * */
    private String name;
    /***
     * 连接描述
     * */
    private String content;
    /***
     * 连接路径
     * */
    private String url;
    /***
     * 连接图片
     * */
    private String imgicon;
     /***
      * 状态 0 禁用 1 正常
      * */
    private Integer status;
     /***
      * 排序
      * */
    private Integer sort;


}
