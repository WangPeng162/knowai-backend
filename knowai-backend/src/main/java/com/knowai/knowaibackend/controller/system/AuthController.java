package com.knowai.knowaibackend.controller.system;

import com.knowai.knowaibackend.common.Result;
import com.knowai.knowaibackend.dto.user.LoginDTO;
import com.knowai.knowaibackend.dto.user.PasswordDTO;
import com.knowai.knowaibackend.dto.user.RegisterDTO;
import com.knowai.knowaibackend.dto.user.UpdateUserInfoDTO;
import com.knowai.knowaibackend.service.AuthService;
import com.knowai.knowaibackend.vo.user.LoginVO;
import com.knowai.knowaibackend.vo.user.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证接口", description = "登录、注册相关接口")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        LoginVO vo = authService.login(dto);
        return Result.success(vo);
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody RegisterDTO dto) {
        boolean register = authService.register(dto);
        return register ? Result.success("注册成功") : Result.fail("注册失败");
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<String> modifyPassword(@Valid @RequestBody PasswordDTO dto){
        boolean success = authService.modifyPassword(dto);
        return success ? Result.success("修改成功") : Result.fail("修改失败");
    }


    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<UserInfoVO> userInfo(){
        UserInfoVO userInfo = authService.getUserInfo();
        return Result.success(userInfo);
    }

    @Operation(summary = "修改当前用户信息")
    @PutMapping("/me")
    public Result<String> updateUserInfo(@Valid @RequestBody UpdateUserInfoDTO dto){
        boolean success = authService.updateUserInfo(dto);
        return success ? Result.success("修改成功") : Result.fail("修改失败");
    }

}
