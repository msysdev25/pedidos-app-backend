package com.backend.pedidos_app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "productos")
@Getter @Setter
@NoArgsConstructor
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String descripcion;

    @Column(nullable = false)
    private Double precio;

    @ManyToOne
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    // Nueva columna para la URL de la imagen en Supabase
    @Column(name = "imagen_url")
    private String imagenUrl;

    // Mantener temporalmente para migración (eliminar después)
    @Lob
    @Column(name = "imagen", columnDefinition = "LONGBLOB")
    private byte[] imagen;

    @Column(name = "tipo_imagen")
    private String tipoImagen;

    @Column(nullable = false)
    private Boolean activo = true;
}