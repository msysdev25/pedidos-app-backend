package com.backend.pedidos_app.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ProductoDto {
    private Long id;
    private String nombre;
    private String descripcion;
    private Double precio;
    private CategoriaDto categoria;
    private String imagenUrl; // Cambiado de byte[] a String
    private Boolean activo;
}