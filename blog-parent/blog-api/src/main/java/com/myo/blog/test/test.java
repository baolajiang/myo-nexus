package com.myo.blog.test;

import java.util.Random;

public class test {
    public static void main(String[] args) {
        System.out.println(generateVerificationCode());
    }
    public static String generateVerificationCode() {
        // 创建一个新的Random类实例
        Random random = new Random();

        // 这个字符串将保存的验证码
        StringBuilder verificationCode = new StringBuilder();

        // 循环6次，每次添加一个0-9的随机数字
        for (int i = 0; i <= 9; i++) {
            verificationCode.append(random.nextInt(10));
        }

        // 将StringBuilder转换为字符串并返回
        return verificationCode.toString();
    }
}
