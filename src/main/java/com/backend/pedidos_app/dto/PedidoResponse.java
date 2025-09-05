package com.backend.pedidos_app.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
public class PedidoResponse {
    private Long id;
    private String nombreCliente;
    private String telefonoCliente;
    private String direccion;
    private String tipoEntrega;
    private Double recargoDomicilio;
    private String tipoPago;
    private String comprobanteUrl;
    private Double total;
    private String estado;
    private LocalDateTime fechaPedido;
    private List<PedidoProductoResponse> productos;

    @Getter @Setter
    public static class PedidoProductoResponse {
        private String nombreProducto;
        private Integer cantidad;
        private String personalizaciones;
        private Double precioUnitario;
    }
}