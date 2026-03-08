package com.myo.blog.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myo.blog.dao.mapper.MessageMapper;
import com.myo.blog.dao.pojo.Comment;
import com.myo.blog.dao.pojo.Message;
import com.myo.blog.dao.pojo.SysUser;
import com.myo.blog.entity.CommentVo;
import com.myo.blog.entity.MessageVo;
import com.myo.blog.entity.Result;
import com.myo.blog.entity.UserVo;
import com.myo.blog.entity.params.CommentParam;
import com.myo.blog.entity.params.MessageParam;
import com.myo.blog.service.MessageService;
import com.myo.blog.service.SysUserService;
import com.myo.blog.utils.UserThreadLocal;
import lombok.RequiredArgsConstructor;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author : myo
 * @create 2023/8/8 16:00
 */
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;

    private final SysUserService sysUserService;


    public Result message(MessageParam messageParam) {
        SysUser sysUser = UserThreadLocal.get();



/*        Message message = new Message();

        message.setAuthorId(sysUser.getId());
        message.setContent(messageParam.getContent());
        message.setCreateDate(System.currentTimeMillis());
        Long parent = messageParam.getParent();
        if (parent == null || parent == 0) {
            message.setLevel(1);
        }else{
            message.setLevel(2);
        }
        message.setParentId(parent == null ? 0 : parent);
        Long toUserId = messageParam.getToUserId();
        message.setToUid(toUserId == null ? 0 : toUserId);
        int t=this.messageMapper.insert(message);*/

        return Result.success(null);
    }

    /*private List<MessageVo> copyList(List<Message> message) {
        List<MessageVo> messageVoList = new ArrayList<>();
        for (Message Message : message) {
            messageVoList.add(copy(Message));
        }
        return messageVoList;
    }


    private MessageVo copy(Message message) {
        MessageVo messageVo = new MessageVo();
        BeanUtils.copyProperties(message,messageVo);
        messageVo.setId(String.valueOf(messageVo.getId()));
        //留言者信息
        Long authorId = message.getAuthorId();
        UserVo userVo = this.sysUserService.findUserVoById(authorId);
        messageVo.setAuthor(userVo);
        //子评论
        Integer level = message.getLevel();
        if (1 == level){
            Long id = message.getId();
            List<MessageVo> commentVoList = findMessageByParentId(id);
            messageVo.setChildrens(commentVoList);
        }
        //to User 给谁评论
        if (level > 1){
            Long toUid = message.getToUid();

            UserVo toUserVo = this.sysUserService.findUserVoById(toUid);
            messageVo.setToUser(toUserVo);
        }
        //评论or回复 时间
        messageVo.setCreateDate(new DateTime(message.getCreateDate()).toString("yyyy-MM-dd HH:mm"));
        return messageVo;
    }

    private List<MessageVo> findMessageByParentId(Long id) {
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Message::getParentId,id);
        queryWrapper.eq(Message::getLevel,2);
        return copyList(messageMapper.selectList(queryWrapper));
    }*/
}
