package com.knowai.knowaibackend.service.imp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.knowai.knowaibackend.dto.LoginDTO;
import com.knowai.knowaibackend.dto.RegisterDTO;
import com.knowai.knowaibackend.entity.User;
import com.knowai.knowaibackend.exception.BusinessException;
import com.knowai.knowaibackend.mapper.UserMapper;
import com.knowai.knowaibackend.service.AuthService;
import com.knowai.knowaibackend.utils.JwtUtil;
import com.knowai.knowaibackend.vo.LoginVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.autoconfigure.WebMvcProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl extends ServiceImpl<UserMapper, User> implements AuthService {
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

        //2.校验密码
        boolean matches = passwordEncoder.matches(dto.getPassword(), user.getPassword());
        if (!matches){
            throw new BusinessException("密码错误");
        }

        //3.生成token
        String token = JwtUtil.createToken(user.getId());
        LoginVO vo = new LoginVO();
        vo.setToken(token);

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
        return this.save(user);
    }
}
