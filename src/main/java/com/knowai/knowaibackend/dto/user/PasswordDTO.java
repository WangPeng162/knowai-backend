package com.knowai.knowaibackend.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordDTO {
    @NotBlank(message = "请输入旧密码")
    private String oldPassword;

    @NotBlank(message = "请输入新密码")
    private String newPassword;

    @NotBlank(message = "请再次确认密码")
    private String confirmPassword;
}
