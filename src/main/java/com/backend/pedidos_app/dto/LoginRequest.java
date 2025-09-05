package com.backend.pedidos_app.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LoginRequest {
    private String username;
    private String password;
}