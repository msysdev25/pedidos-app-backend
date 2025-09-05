package com.backend.pedidos_app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.backend.pedidos_app.model.Categoria;
import com.backend.pedidos_app.model.Producto;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findAll(); //Cambiar el método findByActivoTrue a findAll para incluir inactivos
    // Mantener este para cuando necesitemos solo activos (como en el catálogo público)
    List<Producto> findByActivoTrue();
    
    List<Producto> findByCategoriaIdAndActivoTrue(Long categoriaId);
    
    @Query("SELECT DISTINCT p.categoria FROM Producto p")
    List<Categoria> findDistinctCategorias();
    
    @Query("SELECT DISTINCT p.categoria FROM Producto p WHERE p.activo = true")
    List<Categoria> findDistinctCategoriasByActivoTrue();
    
    @Query("SELECT pp.producto.id, SUM(pp.cantidad) as cantidad " +
           "FROM PedidoProducto pp " +
           "JOIN pp.pedido p " +
           "WHERE p.fechaPedido BETWEEN :inicio AND :fin AND p.estado <> 'cancelado' " +
           "GROUP BY pp.producto.id " +
           "ORDER BY cantidad DESC")
    List<Object[]> findProductosMasVendidos(@Param("inicio") LocalDateTime inicio, 
                                          @Param("fin") LocalDateTime fin);
    
    List<Producto> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);

    @Query("SELECT COUNT(pp) > 0 FROM PedidoProducto pp WHERE pp.producto.id = :productoId")
    boolean existsPedidosByProductoId(@Param("productoId") Long productoId);
    
    @Query("SELECT COUNT(p) > 0 FROM Producto p WHERE p.categoria.id = :categoriaId AND p.activo = true")
    boolean existsByCategoriaIdAndActivoTrue(@Param("categoriaId") Long categoriaId);
    
    @Query("SELECT COUNT(p) > 0 FROM Producto p WHERE p.categoria.id = :categoriaId")
    boolean existsByCategoriaId(@Param("categoriaId") Long categoriaId);
}