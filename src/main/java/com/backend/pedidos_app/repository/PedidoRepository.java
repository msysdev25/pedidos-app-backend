package com.backend.pedidos_app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.backend.pedidos_app.model.Pedido;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    
    // Métodos de consulta con JOIN FETCH (se mantienen igual)
    @Query("SELECT DISTINCT p FROM Pedido p LEFT JOIN FETCH p.productos pp ORDER BY p.fechaPedido DESC, pp.orden ASC")
    List<Pedido> findAllWithOrderedProducts();

    @Query("SELECT DISTINCT p FROM Pedido p LEFT JOIN FETCH p.productos pp WHERE p.estado = :estado ORDER BY p.fechaPedido DESC, pp.orden ASC")
    List<Pedido> findByEstadoWithOrderedProducts(@Param("estado") String estado);

    @Query("SELECT DISTINCT p FROM Pedido p LEFT JOIN FETCH p.productos pp WHERE p.usuario.id = :usuarioId ORDER BY p.fechaPedido DESC, pp.orden ASC")
    List<Pedido> findByUsuarioIdWithOrderedProducts(@Param("usuarioId") Long usuarioId);
    
    // Métodos existentes (se mantienen igual)
    List<Pedido> findByEstado(String estado);
    List<Pedido> findByFechaPedidoBetween(LocalDateTime inicio, LocalDateTime fin);
    List<Pedido> findByUsuarioId(Long usuarioId);
    
    long countByFechaPedidoBetween(LocalDateTime inicio, LocalDateTime fin);
    
    long countByEstado(String estado);
    
    List<Pedido> findTop5ByOrderByFechaPedidoDesc();

    // MÉTODOS MODIFICADOS PARA MANEJAR VALORES NULOS:

    // Método para sumar total con COALESCE
    @Query("SELECT COALESCE(SUM(p.total), 0.0) FROM Pedido p WHERE p.fechaPedido BETWEEN :inicio AND :fin")
    Double sumTotalByFechaPedidoBetween(@Param("inicio") LocalDateTime inicio, 
                                      @Param("fin") LocalDateTime fin);
    
    // Método para sumar total excluyendo cancelados con COALESCE
    @Query("SELECT COALESCE(SUM(p.total), 0.0) FROM Pedido p WHERE p.fechaPedido BETWEEN :inicio AND :fin AND p.estado <> :estado")
    Double sumTotalByFechaPedidoBetweenAndEstadoNot(
        @Param("inicio") LocalDateTime inicio, 
        @Param("fin") LocalDateTime fin,
        @Param("estado") String estado);
    
    // Método agrupado por mes con COALESCE para el total
    @Query("SELECT FUNCTION('DATE_FORMAT', p.fechaPedido, '%b') as mes, COALESCE(COUNT(p), 0) as total " +
           "FROM Pedido p " +
           "WHERE p.fechaPedido BETWEEN :inicio AND :fin " +
           "GROUP BY FUNCTION('DATE_FORMAT', p.fechaPedido, '%m'), FUNCTION('DATE_FORMAT', p.fechaPedido, '%b') " +
           "ORDER BY FUNCTION('DATE_FORMAT', p.fechaPedido, '%m')")
    List<Object[]> countPedidosGroupByMonth(@Param("inicio") LocalDateTime inicio, 
                                          @Param("fin") LocalDateTime fin);
    
    // Método agrupado por mes excluyendo cancelados con COALESCE
    @Query("SELECT FUNCTION('DATE_FORMAT', p.fechaPedido, '%b') as mes, COALESCE(COUNT(p), 0) as total " +
           "FROM Pedido p " +
           "WHERE p.fechaPedido BETWEEN :inicio AND :fin AND p.estado <> :estado " +
           "GROUP BY FUNCTION('DATE_FORMAT', p.fechaPedido, '%m'), FUNCTION('DATE_FORMAT', p.fechaPedido, '%b') " +
           "ORDER BY FUNCTION('DATE_FORMAT', p.fechaPedido, '%m')")
    List<Object[]> countPedidosGroupByMonthAndEstadoNot(
        @Param("inicio") LocalDateTime inicio, 
        @Param("fin") LocalDateTime fin,
        @Param("estado") String estado);
    
    // Método para contar pedidos excluyendo un estado con COALESCE
    @Query("SELECT COALESCE(COUNT(p), 0) FROM Pedido p WHERE p.fechaPedido BETWEEN :inicio AND :fin AND p.estado <> :estado")
    long countByFechaPedidoBetweenAndEstadoNot(
        @Param("inicio") LocalDateTime inicio, 
        @Param("fin") LocalDateTime fin,
        @Param("estado") String estado);
    
    // MÉTODOS PARA REPORTES - MODIFICADOS CON COALESCE:

    @Query("SELECT FUNCTION('DATE_FORMAT', p.fechaPedido, '%b') as mes, COALESCE(SUM(p.total), 0.0) as total " +
           "FROM Pedido p " +
           "WHERE p.fechaPedido BETWEEN :inicio AND :fin AND p.estado <> 'cancelado' " +
           "GROUP BY FUNCTION('DATE_FORMAT', p.fechaPedido, '%m'), FUNCTION('DATE_FORMAT', p.fechaPedido, '%b') " +
           "ORDER BY FUNCTION('DATE_FORMAT', p.fechaPedido, '%m')")
    List<Object[]> sumVentasGroupByMonth(@Param("inicio") LocalDateTime inicio, 
                                       @Param("fin") LocalDateTime fin);
    
    @Query("SELECT FUNCTION('DATE_FORMAT', p.fechaPedido, '%b') as mes, COALESCE(SUM(p.total), 0.0) as total " +
           "FROM Pedido p " +
           "WHERE p.fechaPedido BETWEEN :inicio AND :fin AND p.estado = :estado " +
           "GROUP BY FUNCTION('DATE_FORMAT', p.fechaPedido, '%m'), FUNCTION('DATE_FORMAT', p.fechaPedido, '%b') " +
           "ORDER BY FUNCTION('DATE_FORMAT', p.fechaPedido, '%m')")
    List<Object[]> sumVentasGroupByMonthAndEstado(@Param("inicio") LocalDateTime inicio, 
                                                @Param("fin") LocalDateTime fin,
                                                @Param("estado") String estado);
    
    @Query("SELECT p.estado, COALESCE(COUNT(p), 0) " +
           "FROM Pedido p " +
           "WHERE p.fechaPedido BETWEEN :inicio AND :fin " +
           "GROUP BY p.estado")
    List<Object[]> countPedidosGroupByEstado(@Param("inicio") LocalDateTime inicio, 
                                           @Param("fin") LocalDateTime fin);
    
    @Query("SELECT p.nombreCliente, p.telefonoCliente, COALESCE(COUNT(p), 0) as totalPedidos, COALESCE(SUM(p.total), 0.0) as totalGastado " +
           "FROM Pedido p " +
           "WHERE p.fechaPedido BETWEEN :inicio AND :fin " +
           "GROUP BY p.nombreCliente, p.telefonoCliente " +
           "ORDER BY totalGastado DESC " +
           "LIMIT 1")
    List<Object[]> findClienteFrecuente(@Param("inicio") LocalDateTime inicio, 
                                      @Param("fin") LocalDateTime fin);
    
    @Query("SELECT COALESCE(COUNT(p), 0) FROM Pedido p WHERE p.fechaPedido BETWEEN :inicio AND :fin AND p.estado = :estado")
    long countByFechaPedidoBetweenAndEstado(@Param("inicio") LocalDateTime inicio, 
                                          @Param("fin") LocalDateTime fin,
                                          @Param("estado") String estado);
    
    @Query("SELECT COALESCE(SUM(p.total), 0.0) FROM Pedido p WHERE p.fechaPedido BETWEEN :inicio AND :fin AND p.estado = :estado")
    Double sumTotalByFechaPedidoBetweenAndEstado(@Param("inicio") LocalDateTime inicio, 
                                               @Param("fin") LocalDateTime fin,
                                               @Param("estado") String estado);
}