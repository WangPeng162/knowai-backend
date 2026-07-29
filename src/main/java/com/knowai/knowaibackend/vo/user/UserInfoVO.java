package com.knowai.knowaibackend.vo.user;

import lombok.Data;

@Data
public class UserInfoVO {

    private Long id;

    private String username;

    private String nickname;

    private Integer status;

    private String email;

    private String phone;

    private String avatar;
}
