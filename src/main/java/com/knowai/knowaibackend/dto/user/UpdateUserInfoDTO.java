package com.knowai.knowaibackend.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateUserInfoDTO {

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @Email(message = "邮箱格式错误")
    private String email;

    @Pattern(
            regexp = "^1[3-9]\\d{9}$",
            message = "手机号格式错误"
    )
    private String phone;

    @NotBlank(message = "头像不能为空")
    private String avatar;
}
