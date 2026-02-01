package com.myo.blog.utils;

import com.myo.blog.config.BlogProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
/**
 * 序列号生成器
 * @author tangmengxi
 * @date 2024/11/13 10:20
 */
@Component
public class SerialGenerator {

    private static Long MACHINE_ID= 1L;
    private static final int MAX_SEQUENCE = 9999;
    private static final int MIN_RANDOM = 1;
    private static final int MAX_RANDOM = 99;

    @Autowired
    private BlogProperties blogProperties;

    private static final AtomicLong atomicSeq = new AtomicLong(1);

    @PostConstruct
    public void init() {
        MACHINE_ID = blogProperties.getMachine().getId();
        if (MACHINE_ID == null || MACHINE_ID < 1 || MACHINE_ID > 99) {
            MACHINE_ID = 1L;
        }
        System.out.println("当前机器码已初始化为: " + MACHINE_ID);
    }
    /**
     * 生成序列号
     * @param bizPrefix 业务前缀
     * @return 序列号
     */
    public static String generate(String bizPrefix) {
        // A. 时间部分
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String dateStr = sdf.format(new Date());

        // B. 机器码部分
        String machineStr = String.format("%02d", MACHINE_ID);

        // C. 随机数部分 - 确保始终2位数
        int randomNum = ThreadLocalRandom.current().nextInt(MIN_RANDOM, MAX_RANDOM + 1);
        String randomStr = String.format("%02d", randomNum);  // 强制格式化为2位数

        // D. 序列号部分
        long seq = getNextSequence();
        String seqStr = String.format("%04d", seq);

        return bizPrefix + dateStr + machineStr + randomStr + seqStr;
    }

    private static long getNextSequence() {
        while (true) {
            long currentSeq = atomicSeq.get();
            long nextSeq = currentSeq + ThreadLocalRandom.current().nextInt(1, 3);

            if (nextSeq > MAX_SEQUENCE) {
                // 尝试重置序列号
                if (atomicSeq.compareAndSet(currentSeq, 1)) {
                    return 1;
                }
                // 如果重置失败，继续循环重试
            } else {
                // 尝试递增序列号
                if (atomicSeq.compareAndSet(currentSeq, nextSeq)) {
                    return nextSeq;
                }
                // 如果递增失败，继续循环重试
            }
        }
    }
}