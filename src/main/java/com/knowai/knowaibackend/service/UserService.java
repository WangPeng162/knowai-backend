package com.knowai.knowaibackend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.knowai.knowaibackend.dto.UserDTO;
import com.knowai.knowaibackend.entity.User;
import com.knowai.knowaibackend.vo.UserVO;
import java.util.List;


public interface UserService extends IService<User> {
    // 查询全部用户
    List<UserVO> getList();
    //分页查询
    IPage<UserVO> getUserPage(long current, long size);
    //新增用户
    boolean addUser(UserDTO dto);
    //修改用户
    boolean updateUser(Long id, UserDTO dto);
    //根据id删除用户
    boolean deleteById(Long id);
}
