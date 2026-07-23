package com.knowai.knowaibackend.controller.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowai.knowaibackend.common.Result;
import com.knowai.knowaibackend.dto.UserDTO;
import com.knowai.knowaibackend.entity.User;
import com.knowai.knowaibackend.service.UserService;
import com.knowai.knowaibackend.vo.UserVO;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SuppressWarnings("all")
@PermitAll
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 查询所有用户
     * @return
     */
    @GetMapping("/list")
    public Result<List<UserVO>> getAllUser(){
        List<UserVO> list = userService.getList();
        return Result.success(list);
    }

    /**
     * 分页查询
     * @param current
     * @param size
     * @return
     */
    @GetMapping("/page")
    public Result<IPage<UserVO>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size
    ){
        return Result.success(userService.getUserPage(current,size));
    }

    /**
     * 添加用户
     * @param dto
     * @return
     */
    @PostMapping("/add")
    public Result<String> addUser(@RequestBody UserDTO dto){
        boolean success = userService.addUser(dto);
        return success ? Result.success("新增成功") : Result.fail("新增失败");
    }

    /**
     * 用户更新
     * @param dto
     * @return
     */
    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Long id,@RequestBody UserDTO dto){
        boolean success = userService.updateUser(id,dto);
        return success ? Result.success("修改成功") : Result.fail("修改失败");
    }

    /**
     * 根据id删除用户
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id){
        boolean success = userService.deleteById(id);
        return success ? Result.success("删除成功") : Result.fail("删除失败");
    }
}
