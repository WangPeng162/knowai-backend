package com.knowai.knowaibackend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.knowai.knowaibackend.dto.user.LoginDTO;
import com.knowai.knowaibackend.dto.user.PasswordDTO;
import com.knowai.knowaibackend.dto.user.RegisterDTO;
import com.knowai.knowaibackend.dto.user.UpdateUserInfoDTO;
import com.knowai.knowaibackend.entity.User;
import com.knowai.knowaibackend.vo.user.LoginVO;
import com.knowai.knowaibackend.vo.user.UserInfoVO;

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

    /**
     * 修改密码
     * @param dto
     * @return
     */
    boolean modifyPassword(PasswordDTO dto);

    /**
     * 获取当前用户信息
     * @return
     */
    UserInfoVO getUserInfo();

    /**
     * 修改当前用户信息
     * @return
     */
    boolean updateUserInfo(UpdateUserInfoDTO dto);
}
