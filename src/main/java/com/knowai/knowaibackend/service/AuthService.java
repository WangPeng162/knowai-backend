package com.knowai.knowaibackend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.knowai.knowaibackend.dto.LoginDTO;
import com.knowai.knowaibackend.dto.RegisterDTO;
import com.knowai.knowaibackend.entity.User;
import com.knowai.knowaibackend.vo.LoginVO;

public interface AuthService extends IService<User> {
    /**
     * 登录
     * @param dto
     * @return
     */
    LoginVO login(LoginDTO dto);

    /**
     * 注册
     * @param dto
     * @return
     */
    Boolean register(RegisterDTO dto);
}
