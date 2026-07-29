package com.knowai.knowaibackend.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.knowai.knowaibackend.dto.user.UserDTO;
import com.knowai.knowaibackend.entity.User;
import com.knowai.knowaibackend.exception.BusinessException;
import com.knowai.knowaibackend.mapper.UserMapper;
import com.knowai.knowaibackend.service.UserService;

import com.knowai.knowaibackend.vo.user.UserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);


    /**
     * 查询全部用户
     * @return
     */
    @Override
    public List<UserVO> getList() {

        List<User> list = this.list();
        //循环转换为VO集合返回
        List<UserVO> voList = new ArrayList<>();
        for (User user : list) {
            UserVO vo = new UserVO();
            // 拷贝同名、同类型字段
            BeanUtils.copyProperties(user,vo);
            voList.add(vo);
        }
        return voList;
    }

    /**
     * 分页查询
     * @param current
     * @param size
     * @return
     */
    @Override
    public IPage<UserVO> getUserPage(long current, long size) {
        // 1. 创建分页对象
        Page<User> page = new Page<>(current, size);
        // 2. 执行分页查询
        Page<User> userPage = baseMapper.selectPage(page, null);
        // 3. 创建VO分页对象
        Page<UserVO> voPage = new Page<>();
        // 4. 复制分页信息
        voPage.setCurrent(userPage.getCurrent());
        voPage.setSize(userPage.getSize());
        voPage.setTotal(userPage.getTotal());
        // 5. Entity转VO
        List<UserVO> voList = userPage.getRecords().stream().map(
                user -> {
                    UserVO vo = new UserVO();
                    BeanUtils.copyProperties(user,vo);
                    return vo;
                }
        ).toList();
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 新增用户
     * @param dto
     * @return
     */
    @Override
    public boolean addUser(UserDTO dto) {
        //DTO转User
        User user = new User();
        BeanUtils.copyProperties(dto,user);
        //填充创建时间
        user.setCreateTime(LocalDateTime.now());
        boolean success = this.save(user);
        if (success) {
            log.info("新增用户成功: username={}", dto.getUsername());
        }
        return success;
    }

    /**
     * 修改用户
     * @param id
     * @param dto
     * @return
     */
    @Override
    public boolean updateUser(Long id, UserDTO dto) {
        //得到user实体
        User user = this.getById(id);
        //判断user是否为空
        if (user == null){
            throw new BusinessException("用户不存在");
        }
        BeanUtils.copyProperties(dto,user);
        boolean success = this.updateById(user);
        if (success) {
            log.info("修改用户成功: userId={}", id);
        }
        return success;
    }

    /**
     * 根据id删除用户
     * @param id
     * @return
     */
    @Override
    public boolean deleteById(Long id) {
        boolean success = this.removeById(id);
        if (success) {
            log.info("删除用户成功: userId={}", id);
        }
        return success;
    }


}
