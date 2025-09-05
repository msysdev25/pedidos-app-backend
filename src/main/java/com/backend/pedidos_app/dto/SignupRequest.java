package com.backend.pedidos_app.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter @Setter
public class SignupRequest {
    private String username;
    private String email;
    private String password;
    private String nombreCompleto;
    private String telefono;
    private Set<String> roles;
}