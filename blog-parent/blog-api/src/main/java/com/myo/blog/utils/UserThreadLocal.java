package com.myo.blog.utils;

import com.myo.blog.dao.pojo.SysUser;
// 线程变量隔离，用于存储当前登录用户的信息
public class UserThreadLocal {
    //防止外部实例化
    private UserThreadLocal(){

    }
    //线程变量隔离
    private static final ThreadLocal<SysUser> LOCAL = new ThreadLocal<>();

    public static void put(SysUser sysUser){
        LOCAL.set(sysUser);
    }

    public static SysUser get(){
        return LOCAL.get();
    }

    public static void remove(){
        LOCAL.remove();
    }
}
