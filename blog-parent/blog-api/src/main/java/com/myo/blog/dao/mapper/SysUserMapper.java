package com.myo.blog.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myo.blog.dao.pojo.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SysUserMapper extends BaseMapper<SysUser> {
     /**
     * 根据用户ID查询权限标识列表
     * @param userId 用户ID
     * @return 权限标识列表
     */
     //这个方法，对应 XML 里的 <select id="findPermissionsByUserId">
    List<String> findPermissionsByUserId(String userId);

    /**
     * 插入用户角色关联关系
     * @param userId 用户ID
     * @param roleId 角色ID
     */
    void insertUserRole(@Param("userId") String userId, @Param("roleId") Long roleId);

    // 查出该用户拥有的所有权限标识 (例如: user:status, article:delete)
    @Select("SELECT p.code FROM myo_sys_permission p " +
            "LEFT JOIN myo_sys_role_permission rp ON p.id = rp.permission_id " +
            "LEFT JOIN myo_sys_user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<String> findPermissionCodesByUserId(String userId);

    // 顺便查出权限名称，等会儿发给 AI 让他“察言观色”
    @Select("SELECT p.name FROM myo_sys_permission p " +
            "LEFT JOIN myo_sys_role_permission rp ON p.id = rp.permission_id " +
            "LEFT JOIN myo_sys_user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<String> findPermissionNamesByUserId(String userId);

    // 获取用户所有的角色名称列表
    List<String> findRoleNamesByUserId(String userId);
}
