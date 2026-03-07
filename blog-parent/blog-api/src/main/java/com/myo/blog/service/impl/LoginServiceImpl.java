package com.myo.blog.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myo.blog.dao.mapper.SysUserMapper;
import com.myo.blog.dao.mapper.UserTokenMapper;
import com.myo.blog.dao.pojo.SysUser;
import com.myo.blog.dao.pojo.UserToken;
import com.myo.blog.service.LoginService;
import com.myo.blog.service.MailService;
import com.myo.blog.service.SysUserService;
import com.myo.blog.utils.HttpContextUtils;
import com.myo.blog.utils.IpUtils;
import com.myo.blog.utils.JWTUtils;
import com.myo.blog.entity.ErrorCode;
import com.myo.blog.entity.Result;
import com.myo.blog.entity.params.LoginParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Transactional
public class LoginServiceImpl implements LoginService {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;
    @Autowired
    private SysUserMapper sysUserMapper;

    // 注入邮件发送器
    @Autowired
    private JavaMailSender mailSender;
    // 注入邮件服务类
    @Autowired
    private MailService mailService;

    // === 注入 UserTokenMapper ===
    @Autowired
    private UserTokenMapper userTokenMapper;

    // 从配置文件读取发件人，防止硬编码
    @Value("${spring.mail.username}")
    private String fromEmail;
    // 密码盐值，用于密码加密
    private static final String slat = "myo!@#";
    /**
     * 登录功能
     * @param loginParam
     * @return
     */
    @Override
    public Result login(LoginParam loginParam) {
        String account = loginParam.getAccount();
        String password = loginParam.getPassword();

        log.debug("开始验证登录信息 - 账号: {}", account);

        if (StringUtils.isBlank(account) || StringUtils.isBlank(password)){
            log.warn("登录参数为空 - 账号: {}, 密码长度: {}", account, password != null ? password.length() : 0);
            return Result.fail(ErrorCode.PARAMS_ERROR.getCode(),ErrorCode.PARAMS_ERROR.getMsg());
        }


        password = DigestUtils.md5Hex(password + slat);
        log.debug("密码加密完成 - 账号: {}", account);

        SysUser sysUser = sysUserService.findUser(account,password);
        // ================== 账号校验 ==================
        if (sysUser == null){
            log.warn("用户不存在或密码错误 - 账号: {}", account);
            return Result.fail(ErrorCode.ACCOUNT_PWD_NOT_EXIST.getCode(),ErrorCode.ACCOUNT_PWD_NOT_EXIST.getMsg());
        }
        // ================== 封禁校验 ==================
        // 假设约定：status 为 "99" 时表示封禁，0 正常 1警告
        if (sysUser.getStatus() != null && sysUser.getStatus() == 99) {
            log.warn("该账号已被封禁 - 账号: {}", account);
            return Result.fail(ErrorCode.ACCOUNT_DISABLED.getCode(), "账号已被封禁，请联系管理员");
        }

        log.info("用户验证成功 - 用户ID: {}, 账号: {}, 昵称: {}", sysUser.getId(), account, sysUser.getNickname());

        log.info("用户验证成功 - 用户ID: {}, 账号: {}, 昵称: {}", sysUser.getId(), account, sysUser.getNickname());

        String token = JWTUtils.createToken(sysUser.getId());
        //更新最后登录IP,并设置登录时间
        updateLoginInfo(sysUser.getId());

        // === 同时保存到 Redis 和 MySQL ===
        saveToken(token, sysUser);

        log.debug("登录信息更新完成 - 用户ID: {}, Token生成成功", sysUser.getId());
        return Result.success(token);
    }

    /**
     * 更新用户登录信息
     * @param userId 用户ID
     */
    public void updateLoginInfo(String userId){
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        String ip= IpUtils.getIpAddr(request);
        log.debug("更新用户登录信息 - 用户ID: {}, 登录IP: {}", userId, ip);
        SysUser user = new SysUser();
        user.setId(userId);
        user.setLastIpaddr(ip);
        user.setLastLogin(System.currentTimeMillis());
        this.sysUserService.updateById(user);
        log.debug("用户登录信息更新完成 - 用户ID: {}", userId);
    }

    /**
     * 校验 Token (包含灾难恢复逻辑)
     */
    @Override
    public SysUser checkToken(String token) {
        if (StringUtils.isBlank(token)){
            return null;
        }
        Map<String, Object> stringObjectMap = JWTUtils.checkToken(token);
        if (stringObjectMap == null){
            return null;
        }

        // 1. 先查 Redis
        String userJson = redisTemplate.opsForValue().get("TOKEN:" + token);
        if (StringUtils.isNotBlank(userJson)){
            // Redis 命中，正常返回并续期
            SysUser sysUser = JSON.parseObject(userJson, SysUser.class);
            redisTemplate.expire("TOKEN:" + token, 3, TimeUnit.DAYS);
            redisTemplate.expire("USER_TOKEN:" + sysUser.getId(), 3, TimeUnit.DAYS);

            return sysUser;
        }

        // 2. Redis 未命中，尝试从数据库恢复 (灾难恢复)
        return checkTokenFromDb(token);
    }

    /**
     * 辅助方法：从数据库恢复 Token
     */
    private SysUser checkTokenFromDb(String token) {
        LambdaQueryWrapper<UserToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserToken::getToken, token);
        UserToken userToken = userTokenMapper.selectOne(wrapper);

        // 如果数据库也没有，或者已过期
        if (userToken == null || userToken.getExpireTime() < System.currentTimeMillis()) {
            return null;
        }

        // 数据库有记录 -> 复活 Session
        String userId = userToken.getUserId();
        SysUser sysUser = sysUserService.findUserById(userId);

        // 重新写回 Redis
        saveTokenToRedis(token, sysUser);

        log.info("触发灾难恢复机制：从数据库恢复了用户会话 - 用户ID: {}", userId);
        return sysUser;
    }

    @Override
    public Result logout(String token) {
        // 先获取用户，方便后续删数据库
        SysUser sysUser = checkToken(token);

        // 1. 删 Redis
        redisTemplate.delete("TOKEN:" + token);
        if (sysUser != null) {
            redisTemplate.delete("USER_TOKEN:" + sysUser.getId());

            // 2. 删 MySQL
            userTokenMapper.deleteById(sysUser.getId());
        }
        return Result.success(null);
    }

    // ================== 【发送验证码】 ==================
    /**
     * 发送验证码接口（优化版）
     */
    public Result sendEmailCode(String email) {
        log.debug("开始发送邮箱验证码 - 邮箱: {}", email);

        if (StringUtils.isBlank(email)) {
            log.warn("邮箱验证码发送失败 - 邮箱为空");
            return Result.fail(ErrorCode.PARAMS_ERROR.getCode(), "邮箱不能为空");
        }

        String code = String.valueOf(new Random().nextInt(899999) + 100000);
        log.debug("验证码生成完成 - 邮箱: {}, 验证码: {}", email, code);

        redisTemplate.opsForValue().set("REGISTER_CODE_" + email, code, 5, TimeUnit.MINUTES);
        log.debug("验证码存入Redis完成 - 邮箱: {}, 有效期: 5分钟", email);

        mailService.sendMailAsync(
                email,
                "【月之别邸】注册验证码",
                "欢迎来到月之别邸，您的注册验证码是：" + code + "。有效期 5 分钟，请勿泄露。"
        );

        log.info("邮箱验证码发送成功 - 邮箱: {}", email);
        return Result.success("验证码已发送");
    }


    // ================== 【注册逻辑】 ==================
    @Override
    public Result register(LoginParam loginParam) {

        String account = loginParam.getAccount();
        String password = loginParam.getPassword();
        String nickname = loginParam.getNickname();
        Integer sex = loginParam.getSex();
        String email = loginParam.getEmail();
        String code = loginParam.getCode();
        String avatar = loginParam.getAvatar();

        log.info("注册请求开始 - 账号: {}, 邮箱: {}, 昵称: {}", account, email, nickname);

        if (StringUtils.isBlank(account)
                || StringUtils.isBlank(password)
                || StringUtils.isBlank(nickname)
                || StringUtils.isBlank(email)
                || StringUtils.isBlank(code)
                || (sex != 0 && sex != 1 && sex != 2)
        ){
            log.warn("注册参数校验失败 - 账号: {}, 邮箱: {}, 昵称: {}", account, email, nickname);
            return Result.fail(ErrorCode.PARAMS_ERROR.getCode(),ErrorCode.PARAMS_ERROR.getMsg());
        }

        String redisCode = redisTemplate.opsForValue().get("REGISTER_CODE_" + email);
        if (StringUtils.isBlank(redisCode)) {
            log.warn("验证码校验失败 - 邮箱: {}, 原因: 验证码已过期或未获取", email);
            return Result.fail(ErrorCode.PARAMS_ERROR.getCode(), "验证码已过期或未获取");
        }
        if (!redisCode.equals(code)) {
            log.warn("验证码校验失败 - 邮箱: {}, 输入验证码: {}, 正确验证码: {}", email, code, redisCode);
            return Result.fail(ErrorCode.PARAMS_ERROR.getCode(), "验证码错误");
        }
        log.debug("验证码校验成功 - 邮箱: {}", email);

        SysUser sysUser = sysUserService.findUserByAccount(account);
        if (sysUser != null){
            log.warn("账号已存在 - 账号: {}", account);
            return Result.fail(ErrorCode.ACCOUNT_EXIST.getCode(),"账户已经被注册了");
        }
        log.debug("账号唯一性校验通过 - 账号: {}", account);

        sysUser = new SysUser();
        sysUser.setNickname(nickname);
        sysUser.setAccount(account);
        sysUser.setSex(sex);
        sysUser.setPassword(DigestUtils.md5Hex(password+slat));
        sysUser.setCreateDate(System.currentTimeMillis());
        sysUser.setLastLogin(System.currentTimeMillis());
        sysUser.setAvatar(avatar);

        sysUser.setDeleted(0);
        sysUser.setSalt("");
        sysUser.setStatus(0);
        sysUser.setEmail(email);

        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        String ip= IpUtils.getIpAddr(request);
        sysUser.setIpaddr(ip);
        sysUser.setLastIpaddr("");

        log.debug("准备保存用户信息 - 账号: {}, 邮箱: {}, IP: {}", account, email, ip);
        // 保存用户
        this.sysUserService.save(sysUser);
        // 假设数据库里 ID=4 是普通用户角色
        sysUserMapper.insertUserRole(sysUser.getId(), 4L);
        log.info("用户注册成功 - 用户ID: {}, 账号: {}, 邮箱: {}", sysUser.getId(), account, email);


        redisTemplate.delete("REGISTER_CODE_" + email);
        log.debug("验证码删除完成 - 邮箱: {}", email);

        // 6. 自动登录
        String token = JWTUtils.createToken(sysUser.getId());

        // === 同时保存到 Redis 和 MySQL ===
        saveToken(token, sysUser);

        log.info("用户自动登录完成 - 用户ID: {}, Token生成成功", sysUser.getId());

        return Result.success(token);
    }

    @Override
    public Result kick(Long userId) {
        // 1. 尝试从 Redis 获取 Token
        String token = redisTemplate.opsForValue().get("USER_TOKEN:" + userId);

        // 2. 如果 Redis 挂了或空的，尝试从 DB 捞一下 Token (为了能删掉它)
        if (StringUtils.isBlank(token)) {
            UserToken userToken = userTokenMapper.selectById(userId);
            if (userToken != null) {
                token = userToken.getToken();
            }
        }

        if (StringUtils.isBlank(token)) {
            return Result.fail(ErrorCode.NO_LOGIN.getCode(), "该用户未登录或已下线");
        }

        // 3. 删 Redis
        redisTemplate.delete("TOKEN:" + token);
        redisTemplate.delete("USER_TOKEN:" + userId);

        // 4. 删 MySQL
        userTokenMapper.deleteById(userId);

        return Result.success("用户已强制下线");
    }

    /**
     * 统一保存 Token 到 Redis 和 MySQL
     */
    private void saveToken(String token, SysUser sysUser) {
        // 1. 存 Redis (保持原有的 3 天过期)
        saveTokenToRedis(token, sysUser);

        // 2. 存 MySQL (作为持久化备份)
        UserToken userToken = new UserToken();
        userToken.setUserId(sysUser.getId());
        userToken.setToken(token);
        // 过期时间设为 3 天 (毫秒)
        userToken.setExpireTime(System.currentTimeMillis() + 3L * 24 * 60 * 60 * 1000);

        // 如果存在则更新，不存在则插入
        UserToken exist = userTokenMapper.selectById(sysUser.getId());
        if (exist != null) {
            userTokenMapper.updateById(userToken);
        } else {
            userTokenMapper.insert(userToken);
        }
    }

    private void saveTokenToRedis(String token, SysUser sysUser) {
        redisTemplate.opsForValue().set("TOKEN:" + token, JSON.toJSONString(sysUser), 3, TimeUnit.DAYS);
        redisTemplate.opsForValue().set("USER_TOKEN:" + sysUser.getId(), token, 3, TimeUnit.DAYS);

    }
}