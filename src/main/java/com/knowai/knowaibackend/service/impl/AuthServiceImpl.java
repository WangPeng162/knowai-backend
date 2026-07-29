package com.knowai.knowaibackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.knowai.knowaibackend.common.UserContext;
import com.knowai.knowaibackend.dto.user.LoginDTO;
import com.knowai.knowaibackend.dto.user.PasswordDTO;
import com.knowai.knowaibackend.dto.user.RegisterDTO;
import com.knowai.knowaibackend.dto.user.UpdateUserInfoDTO;
import com.knowai.knowaibackend.entity.User;
import com.knowai.knowaibackend.exception.BusinessException;
import com.knowai.knowaibackend.mapper.UserMapper;
import com.knowai.knowaibackend.service.AuthService;
import com.knowai.knowaibackend.utils.JwtUtil;
import com.knowai.knowaibackend.vo.user.LoginVO;
import com.knowai.knowaibackend.vo.user.UserInfoVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl extends ServiceImpl<UserMapper, User> implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;


    @Override
    public LoginVO login(LoginDTO dto) {
        //1.查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, dto.getUsername())
        );

        if (user == null){
            throw new BusinessException("用户不存在");
        }

        //2.状态判断
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        //3.校验密码
        boolean matches = passwordEncoder.matches(dto.getPassword(), user.getPassword());
        if (!matches){
            throw new BusinessException("密码错误");
        }


        //4.生成token
        String token = JwtUtil.createToken(user.getId());
        LoginVO vo = new LoginVO();
        vo.setToken(token);

        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());
        return vo;
    }

    @Override
    public Boolean register(RegisterDTO dto) {
        //1.查询用户名是否存在
        User exist =
                userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (exist != null){
            throw new BusinessException("用户已存在");
        }

        //2.用户注册到数据库
        //2.1创建用户
        User user = new User();
        BeanUtils.copyProperties(dto,user);
        //2.2密码加密
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        //2.3设置默认这段
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());

        //3.保存
        boolean success = this.save(user);
        if (success) {
            log.info("用户注册成功: username={}", dto.getUsername());
        }
        return success;
    }

    @Override
    public boolean modifyPassword(PasswordDTO dto) {
        //1.获取用户
        //1.1获取用户id
        Long userId = UserContext.getUserId();
        //1.2根据id获取用户
        User user = getById(userId);

        //2.判断用户是否存在
        if (user == null){
            throw new BusinessException("用户不存在");
        }

        //3.判断旧密码
        boolean matches = passwordEncoder.matches(dto.getOldPassword(), user.getPassword());
        if (!matches){
            throw new BusinessException("请输入正确密码");
        }

        //4.判断两次新密码
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())){
            log.info("输入的两次密码不一致");
            throw new BusinessException("两次输入的新密码不一致");
        }

        //5.密码加密
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));

        boolean success = this.updateById(user);
        if (success) {
            log.info("用户修改密码成功,userId:{}", userId);
        }
        return success;
    }

    @Override
    public UserInfoVO getUserInfo() {
        //1.获取当前用户id
        Long userId = UserContext.getUserId();
        //判断id是否为空
        if(userId == null){
            throw new BusinessException("用户未登录");
        }
        //2.获取用户
        User user = getById(userId);
        //判断用户是否为空
        if(user == null){
            throw new BusinessException("用户不存在");
        }

        //3.记录日志
        log.info("获取当前用户信息成功 userId={}, username={}", user.getId(), user.getUsername());

        //4.拷贝到vo
        UserInfoVO vo = new UserInfoVO();
        BeanUtils.copyProperties(user,vo);

        return vo;
    }

    @Override
    public boolean updateUserInfo(UpdateUserInfoDTO dto) {
        //1.获取用户id
        Long userId = UserContext.getUserId();

        //2.获取用户
        User user = getById(userId);
        //判断用户是否为空
        if(user == null){
            throw new BusinessException("用户不存在");
        }

        //3.dto转换user实体
        BeanUtils.copyProperties(dto,user);

        //4.更新
        boolean success = this.updateById(user);
        if (success) {
            log.info(
                    "修改当前用户资料成功 userId={}, username={}",
                    user.getId(),
                    user.getUsername()
            );
        }
        return success;
    }
}
