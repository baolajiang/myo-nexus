package com.myo.blog.entity.params;

import lombok.Data;

@Data
public class PageParams {

    private int page = 1;

    private int pageSize = 10;

    private String categoryId;

    private String tagId;

    private String year;

    private String month;

    private String keyword;// 搜索关键词
    // --- 新增独立搜索栏位 ---
    private String module;     // 模块名称
    private String nickname;   // 操作人
    private Integer status;    // 状态 (0成功, 1失败)
    private String traceId;    // 追踪ID
    // --- 新增独立搜索栏位结束 ---


    public String getMonth(){
        if (this.month != null && this.month.length() == 1){
            return "0"+this.month;
        }
        return this.month;
    }
}
