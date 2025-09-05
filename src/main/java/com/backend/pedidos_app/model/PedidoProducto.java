package com.backend.pedidos_app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pedido_productos")
@Getter @Setter
@NoArgsConstructor
public class PedidoProducto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad = 1;

    @Column(nullable = true)
    private String personalizaciones;

    @Column(nullable = false)
    private Double precioUnitario;
    
    @Column(name = "producto_order") // Columna para mantener el orden
    private Integer orden;
}