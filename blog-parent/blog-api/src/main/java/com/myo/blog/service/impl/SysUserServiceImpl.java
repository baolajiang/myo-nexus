package com.myo.blog.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myo.blog.dao.mapper.SysUserMapper;
import com.myo.blog.dao.pojo.SysUser;
import com.myo.blog.entity.params.PageParams;
import com.myo.blog.entity.params.UserParam;
import com.myo.blog.service.LoginService;
import com.myo.blog.service.SysUserService;
import com.myo.blog.entity.ErrorCode;
import com.myo.blog.entity.LoginUserVo;
import com.myo.blog.entity.Result;
import com.myo.blog.entity.UserVo;
import com.myo.blog.utils.UserThreadLocal;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collection;
import java.util.List;
import com.myo.blog.dao.mapper.UserTokenMapper;
import com.myo.blog.dao.pojo.UserToken;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper sysUserMapper;

    private final StringRedisTemplate stringRedisTemplate;

    @Lazy
    @Autowired
    private LoginService loginService;

    private final UserTokenMapper userTokenMapper;
    /**
     * 根据用户ID查询用户的所有权限代码列表
     * @param userId 用户ID
     * @return 权限代码列表
     */
    @Override
    public List<String> findPermissionsByUserId(String userId) {
        if (userId == null) return null;
        return sysUserMapper.findPermissionsByUserId(userId);
    }
    @Override
    public UserVo findUserVoById(String id) { 

        if(id == null || "0".equals(id)){
            return new UserVo();
        }

        SysUser sysUser = sysUserMapper.selectById(id);

        if (sysUser == null){
            sysUser = new SysUser();
            sysUser.setId("1"); // 默认ID也改成 String
            sysUser.setAvatar("/static/img/tx.gif");
            sysUser.setNickname("baola");
        }
        UserVo userVo  = new UserVo();
        BeanUtils.copyProperties(sysUser,userVo);

        return userVo;
    }

    @Override
    public SysUser findUserById(String id) { 
        SysUser sysUser = sysUserMapper.selectById(id);
        if (sysUser == null){
            sysUser = new SysUser();
            sysUser.setNickname("myo");
        }
        return sysUser;
    }

    @Override
    public SysUser findUser(String account, String password) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getAccount,account);
        queryWrapper.eq(SysUser::getPassword,password);
        queryWrapper.select(SysUser::getAccount,
                SysUser::getStatus,
                SysUser::getSex,
                SysUser::getId,
                SysUser::getAvatar,
                SysUser::getNickname,
                SysUser::getMobilePhoneNumber,
                SysUser::getEmail);
        queryWrapper.last("limit 1");
        return sysUserMapper.selectOne(queryWrapper);
    }

    @Override
    public SysUser findIpaddr(String account, String password) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getAccount,account);
        queryWrapper.eq(SysUser::getPassword,password);
        queryWrapper.select(SysUser::getIpaddr);
        queryWrapper.last("limit 1");
        return sysUserMapper.selectOne(queryWrapper);
    }
    // 根据token查询用户信息
    @Override
    public Result findUserByToken(String token) {
        SysUser sysUser = loginService.checkToken(token);
        if (sysUser == null){
            return Result.fail(ErrorCode.TOKEN_ERROR.getCode(),ErrorCode.TOKEN_ERROR.getMsg());
        }
        LoginUserVo loginUserVo = new LoginUserVo();
        loginUserVo.setId(sysUser.getId()); // 直接赋值
        loginUserVo.setNickname(sysUser.getNickname());
        loginUserVo.setAvatar(sysUser.getAvatar());
        loginUserVo.setAccount(sysUser.getAccount());
        loginUserVo.setEmail(sysUser.getEmail());
        loginUserVo.setSex(sysUser.getSex());

        loginUserVo.setMobilePhoneNumber(sysUser.getMobilePhoneNumber());
        return Result.success(loginUserVo);
    }

    public SysUser queryUserByToken(String token) {
        return loginService.checkToken(token);
    }

    @Override
    public SysUser findUserByAccount(String account) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getAccount,account);
        queryWrapper.last("limit 1");
        return this.sysUserMapper.selectOne(queryWrapper);
    }

    @Override
    public UserVo findUserByAccount(UserParam user) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getAccount,user.getAccount())
                .eq(SysUser::getId,user.getId());
        queryWrapper.last("limit 1");
        SysUser sysUser=this.sysUserMapper.selectOne(queryWrapper);

        UserVo userVo  = new UserVo();
        userVo.setId(sysUser.getId());
        userVo.setAccount(sysUser.getAccount());
        userVo.setNickname(sysUser.getNickname());
        userVo.setAvatar(sysUser.getAvatar());
        userVo.setEmail(sysUser.getEmail());
        userVo.setMobilePhoneNumber(sysUser.getMobilePhoneNumber());

        return userVo;
    }

    @Override
    public int updateUser(UserParam userParam) {
        String id = userParam.getId();

        if (StringUtils.isBlank(id)) {
            return 0;
        }

        LambdaUpdateWrapper<SysUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysUser::getId, id);

        boolean hasUpdate = false;
        boolean needKickout = false;

        if (StringUtils.isNotBlank(userParam.getStatus())) {
            updateWrapper.set(SysUser::getStatus, userParam.getStatus());
            hasUpdate = true;
            if ("99".equals(userParam.getStatus())) {
                needKickout = true;
            }
        }
        if (StringUtils.isNotBlank(userParam.getNickname())) {
            updateWrapper.set(SysUser::getNickname, userParam.getNickname());
            hasUpdate = true;
        }
        if (StringUtils.isNotBlank(userParam.getAvatar())) {

            updateWrapper.set(SysUser::getAvatar, userParam.getAvatar());
            hasUpdate = true;
        }
        if (StringUtils.isNotBlank(userParam.getEmail())) {
            updateWrapper.set(SysUser::getEmail, userParam.getEmail());
            hasUpdate = true;
        }
        if (StringUtils.isNotBlank(userParam.getMobilePhoneNumber())) {
            updateWrapper.set(SysUser::getMobilePhoneNumber, userParam.getMobilePhoneNumber());
            hasUpdate = true;
        }
        if (userParam.getSex() != null) {
            updateWrapper.set(SysUser::getSex, userParam.getSex());
            hasUpdate = true;
        }
        if (StringUtils.isNotBlank(userParam.getRemark())) {
            updateWrapper.set(SysUser::getRemark, userParam.getRemark());
            hasUpdate = true;
        }


        if (hasUpdate) {
            int rows = this.sysUserMapper.update(null, updateWrapper);
            if (rows > 0) {
                if (needKickout) {
                    kickUserOffline(id); // 直接传 String
                } else {
                    updateRedisCache(id); // 直接传 String
                }
            }
            return rows;
        }

        return 0;
    }

    private void updateRedisCache(String userId) { 
        String token = stringRedisTemplate.opsForValue().get("USER_TOKEN:" + userId);
        if (StringUtils.isNotBlank(token)) {
            SysUser newestUser = sysUserMapper.selectById(userId);
            if (newestUser != null) {
                stringRedisTemplate.opsForValue().set("TOKEN:" + token, JSON.toJSONString(newestUser), 3, TimeUnit.DAYS);
            }
        }
    }

    @Override
    public int updateUserAvatar(UserParam userParam){
        return updateUser(userParam);
    }

    @Override
    public SysUser findUserByAccount(SysUser user) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getAccount,user.getAccount());
        queryWrapper.eq(SysUser::getId,user.getId());
        queryWrapper.last("limit 1");
        return this.sysUserMapper.selectOne(queryWrapper);
    }

    @Override
    public void save(SysUser sysUser) {
        this.sysUserMapper.insert(sysUser);
    }

    @Override
    public boolean updateById(SysUser sysUser) {
        return this.sysUserMapper.updateById(sysUser) > 0;
    }

    public List<SysUser> findUserByIds(Collection<String> ids) { // Collection<Long> -> Collection<String>
        return sysUserMapper.selectBatchIds(ids);
    }
    /**
     * 查询用户列表
     * @param pageParams 分页参数
     * @return 分页后的用户列表
     */
    @Override
    public Result UserList(PageParams pageParams) {
        // 获取当前登录用户
        SysUser currentUser = UserThreadLocal.get();
        if (currentUser == null) {
            return Result.fail(ErrorCode.TOKEN_ERROR.getCode(), "未获取到当前登录状态");
        }

        String currentUserId = currentUser.getId();
        // 获取当前用户的最高官阶（level值越小官阶越大，默认99）
        Integer currentUserLevel = getHighestRoleLevel(currentUserId);

        Page<SysUser> page = new Page<>(pageParams.getPage(), pageParams.getPageSize());

        // 直接调用 xml 中自定义的分页 SQL
        Page<SysUser> sysUserPage = sysUserMapper.selectUserListWithLevelCheck(page, currentUserId, currentUserLevel);
        List<SysUser> records = sysUserPage.getRecords();

        if (records.isEmpty()) {
            return Result.success(sysUserPage);
        }

        for (SysUser u : records) {
            // 因为在 XML 查询时已经排除了 password 和 salt，这里无需过滤
            String key = "USER_TOKEN:" + u.getId();
            Boolean isOnline = stringRedisTemplate.hasKey(key);
            u.setOnline(isOnline);
        }

        return Result.success(sysUserPage);
    }

    @Override
    public Result updateUserStatus(UserParam userParam) {
        String id = userParam.getId();
        if (StringUtils.isBlank(id)) {
            return Result.fail(ErrorCode.PARAMS_ERROR.getCode(), "用户ID不能为空");
        }
        if (StringUtils.isBlank(userParam.getStatus())) {
            return Result.fail(ErrorCode.PARAMS_ERROR.getCode(), "状态不能为空");
        }

        try {
            // 注意：这里不需要 Long.parseLong 了
            SysUser existingUser = sysUserMapper.selectById(id);
            if (existingUser == null) {
                return Result.fail(ErrorCode.PARAMS_ERROR.getCode(), "用户不存在，ID: " + id);
            }

            LambdaUpdateWrapper<SysUser> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(SysUser::getId, id)
                    .set(SysUser::getStatus, userParam.getStatus());

            int rows = this.sysUserMapper.update(null, updateWrapper);

            if (rows > 0) {
                if ("99".equals(userParam.getStatus())) {
                    kickUserOffline(id);
                } else {
                    updateRedisCache(id);
                }
                return Result.success("操作成功");
            } else {
                return Result.fail(ErrorCode.OPERATION_FAILED.getCode(), "操作失败，可能用户不存在或数据未变化");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(ErrorCode.SYSTEM_ERROR.getCode(), "系统异常");
        }
    }
    // 强制将用户踢出系统，删除所有相关缓存
    private void kickUserOffline(String userId) {
        String tokenKey = "USER_TOKEN:" + userId;
        String token = stringRedisTemplate.opsForValue().get(tokenKey);

        if (StringUtils.isBlank(token)) {
            LambdaQueryWrapper<UserToken> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(UserToken::getUserId, userId);
            UserToken userToken = userTokenMapper.selectOne(queryWrapper);
            if (userToken != null) {
                token = userToken.getToken();
            }
        }

        if (StringUtils.isNotBlank(token)) {
            stringRedisTemplate.delete("TOKEN:" + token);
        }
        stringRedisTemplate.delete(tokenKey);

        LambdaQueryWrapper<UserToken> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(UserToken::getUserId, userId);
        userTokenMapper.delete(deleteWrapper);

        stringRedisTemplate.delete("USER_STATUS:" + userId);
        stringRedisTemplate.delete("USER_INFO:" + userId);
        stringRedisTemplate.delete("ONLINE_USER:" + userId);
        Boolean isDeleted = stringRedisTemplate.delete("USER_PERMISSIONS:" + userId);
        System.out.println("删除用户权限缓存结果: " + isDeleted);
        System.out.println("用户 " + userId + " 被强制踢下线，删除所有相关缓存");
    }
    // 判断用户是否拥有某个特定权限
    @Override
    public boolean hasPermission(String userId, String permissionCode) {
        if (userId == null || permissionCode == null) return false;
        List<String> codes = sysUserMapper.findPermissionCodesByUserId(userId);
        return codes != null && codes.contains(permissionCode);
    }
    // 获取用户所有的权限名称列表
    @Override
    public List<String> getUserPermissionNames(String userId) {
        if (userId == null) return null;
        return sysUserMapper.findPermissionNamesByUserId(userId);
    }

    @Override
    public List<String> getUserRoleNames(String userId) {
        if (userId == null) return null;
        return sysUserMapper.findRoleNamesByUserId(userId);
    }
    @Override
    public Integer getHighestRoleLevel(String userId) {
        if (userId == null) {
            return 99;
        }
        Integer level = sysUserMapper.getHighestRoleLevel(userId);
        // 防止数据库里没有配level或者查出来是null的情况
        return level == null ? 99 : level;
    }

}