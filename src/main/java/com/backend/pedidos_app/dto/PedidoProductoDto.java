package com.backend.pedidos_app.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PedidoProductoDto {
    private Long productoId;
    private Integer cantidad;
    private String personalizaciones;
}