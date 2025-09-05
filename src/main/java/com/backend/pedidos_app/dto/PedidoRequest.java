package com.backend.pedidos_app.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class PedidoRequest {
    private String nombreCliente;
    private String telefonoCliente;
    private String direccion;
    private String tipoEntrega;
    private String tipoPago;
    private String comprobanteUrl; // Nuevo campo
    private List<PedidoProductoDto> productos;
    private Long usuarioId; // Opcional, para usuarios registrados
}