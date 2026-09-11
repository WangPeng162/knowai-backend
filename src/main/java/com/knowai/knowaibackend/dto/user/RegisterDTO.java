package com.knowai.knowaibackend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @Size(min = 6, max = 20, message = "密码长度需在6-20位之间")
    private String password;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    /**
     * 邀请码：仅当后端配置了 knowai.invite-code 时才校验（本地开发不配则跳过）
     */
    private String inviteCode;
}
