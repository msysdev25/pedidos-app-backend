package com.backend.pedidos_app.dto;



import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CategoriaDto {
    private Long id;
    private String nombre;
    private Boolean activo;
}