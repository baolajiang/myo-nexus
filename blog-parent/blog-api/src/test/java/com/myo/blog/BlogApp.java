package com.myo.blog;



import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import com.alibaba.fastjson.JSON;
import com.myo.blog.entity.Result;
import com.myo.blog.service.CommentsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.io.*;

import java.net.*;
import java.util.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by IntelliJ IDEA.
 *
 * @Author : myo
 * @create 2023/7/12 9:55
 */
@SpringBootTest
public class BlogApp {
    @Autowired
    private CommentsService commentsService;

    @Test
    public void test() throws Exception {

        System.out.println(generateVerificationCode());

    }
    public  String generateVerificationCode() {
        // 创建一个新的Random类实例
        Random random = new Random();

        // 这个字符串将保存验证码
        StringBuilder verificationCode = new StringBuilder();

        // 循环6次，每次添加一个0-9的随机数字
        for (int i = 0; i< 6; i++) {
            verificationCode.append(random.nextInt(10));
        }

        // 将StringBuilder转换为字符串并返回
        return verificationCode.toString();
    }





}
