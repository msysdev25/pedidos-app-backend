package com.backend.pedidos_app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "pedidos")
@Getter @Setter
@NoArgsConstructor
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombreCliente;

    @Column(nullable = false)
    private String telefonoCliente;

    @Column(nullable = true)
    private String direccion;

    @Column(nullable = false)
    private String tipoEntrega; // "recoger" o "domicilio"

    @Column(nullable = false)
    private Double recargoDomicilio = 0.0;

    @Column(nullable = false)
    private String tipoPago; // "efectivo" o "transferencia"

    @Column(nullable = true)
    private String comprobanteUrl;

    @Column(nullable = false)
    private Double total;

    @Column(nullable = false)
    private String estado = "pendiente"; // pendiente, en_preparacion, listo, entregado, cancelado

    @Column(nullable = false)
    private LocalDateTime fechaPedido = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = true)
    private Usuario usuario;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC") // Ordenar por ID (o puedes añadir un campo 'orden' explícito)
    private List<PedidoProducto> productos = new ArrayList<>();
}