package com.knowai.knowaibackend.controller.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowai.knowaibackend.common.Result;
import com.knowai.knowaibackend.dto.user.UserDTO;
import com.knowai.knowaibackend.service.UserService;
import com.knowai.knowaibackend.vo.user.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户管理", description = "用户的增删改查接口")
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "查询所有用户")
    @GetMapping("/list")
    public Result<List<UserVO>> getAllUser() {
        List<UserVO> list = userService.getList();
        return Result.success(list);
    }

    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    public Result<IPage<UserVO>> page(
            @Parameter(description = "当前页", example = "1")
            @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数", example = "10")
            @RequestParam(defaultValue = "10") Long size
    ) {
        return Result.success(userService.getUserPage(current, size));
    }

    @Operation(summary = "添加用户")
    @PostMapping("/add")
    public Result<String> addUser(@Valid @RequestBody UserDTO dto) {
        boolean success = userService.addUser(dto);
        return success ? Result.success("新增成功") : Result.fail("新增失败");
    }

    @Operation(summary = "修改用户")
    @PutMapping("/{id}")
    public Result<String> update(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Valid @RequestBody UserDTO dto) {
        boolean success = userService.updateUser(id, dto);
        return success ? Result.success("修改成功") : Result.fail("修改失败");
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<String> delete(@Parameter(description = "用户ID") @PathVariable Long id) {
        boolean success = userService.deleteById(id);
        return success ? Result.success("删除成功") : Result.fail("删除失败");
    }
}
