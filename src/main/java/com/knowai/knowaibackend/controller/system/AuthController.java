package com.knowai.knowaibackend.controller.system;



import com.knowai.knowaibackend.common.Result;
import com.knowai.knowaibackend.dto.LoginDTO;
import com.knowai.knowaibackend.dto.RegisterDTO;
import com.knowai.knowaibackend.service.AuthService;
import com.knowai.knowaibackend.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 登录
     * @param dto
     * @return
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO dto){
        LoginVO vo = authService.login(dto);
        return Result.success(vo);
    }

    /**
     * 注册
     * @param dto
     * @return
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterDTO dto){
        boolean register = authService.register(dto);
        return register ? Result.success("注册成功") : Result.fail("注册失败");
    }

}
