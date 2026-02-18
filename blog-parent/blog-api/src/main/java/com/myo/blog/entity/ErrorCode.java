package com.myo.blog.entity;

public enum  ErrorCode {

    PARAMS_ERROR(10001,"参数有误"),

    ACCOUNT_PWD_NOT_EXIST(10002,"用户名或密码不存在"),
    TOKEN_ERROR(10003,"token不合法"),
    ACCOUNT_EXIST(10004,"账号已存在"),
    NO_PERMISSION(70001,"无访问权限"),
    SESSION_TIME_OUT(90001,"会话超时"),
    NO_LOGIN(90002,"未登录"),
    ACCOUNT_DISABLED(90003, "账号已被封禁"),


    // 错误码
    SYSTEM_ERROR(500, "系统异常"),
    DATA_NOT_FOUND(10005, "数据不存在"),
    OPERATION_FAILED(10006, "操作失败"),
    FILE_UPLOAD_FAILED(10007, "文件上传失败"),
    NETWORK_ERROR(10008, "网络异常"),
    // 安全相关错误码
    IP_BANNED(90010, "系统检测到异常流量，您的IP已被暂时封禁，请稍后再试"),
    RISK_CONTROL(90011, "操作频率过高，请休息一下");

    private int code;//错误代码
    private String msg;//错误提示

    ErrorCode(int code, String msg){
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

}
