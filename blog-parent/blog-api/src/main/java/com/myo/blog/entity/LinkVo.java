package com.myo.blog.entity;

import lombok.Data;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author : myo
 * @create 2023/7/29 14:32
 */
@Data
public class LinkVo {
    private String id;

    private String name;

    private String content;

    private String url;

    private String imgicon;

    private Integer status;

    private Integer sort;

}
