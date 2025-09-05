package com.backend.pedidos_app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.backend.pedidos_app.model.Categoria;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findByActivoTrue();
    Optional<Categoria> findByNombre(String nombre);
    boolean existsByNombre(String nombre);
    
    @Query("SELECT COUNT(p) > 0 FROM Producto p WHERE p.categoria.id = :categoriaId AND p.activo = true")
    boolean existsProductosActivosByCategoriaId(@Param("categoriaId") Long categoriaId);
    
    
}