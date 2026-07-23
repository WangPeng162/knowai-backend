package com.knowai.knowaibackend.dto;

import lombok.Data;

@Data
public class UserDTO {
    private String username;
    private String password;
    private String nickname;
    private Integer status;
}
