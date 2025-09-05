package com.backend.pedidos_app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.backend.pedidos_app.exception.ResourceNotFoundException;
import com.backend.pedidos_app.model.Producto;
import com.backend.pedidos_app.repository.PedidoRepository;
import com.backend.pedidos_app.repository.ProductoRepository;

@Service
public class ReporteService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ProductoRepository productoRepository;

    public Map<String, Object> generarReporte(LocalDateTime inicio, LocalDateTime fin, String estadoFiltro) {
        Map<String, Object> reporte = new HashMap<>();
        
        // 1. Ventas por mes
        List<Object[]> ventasPorMes = estadoFiltro.equals("todos") ? 
            pedidoRepository.sumVentasGroupByMonth(inicio, fin) :
            pedidoRepository.sumVentasGroupByMonthAndEstado(inicio, fin, estadoFiltro);
        
        List<String> meses = new ArrayList<>();
        List<Double> montos = new ArrayList<>();
        
        for (Object[] resultado : ventasPorMes) {
            meses.add((String) resultado[0]);
            // Manejar valores nulos
            Double monto = resultado[1] != null ? (Double) resultado[1] : 0.0;
            montos.add(monto);
        }
        
        reporte.put("meses", meses);
        reporte.put("ventas", montos);
        
        // 2. Totales - Manejar valores nulos en las consultas
        Double totalVentasTemp = estadoFiltro.equals("todos") ? 
            pedidoRepository.sumTotalByFechaPedidoBetweenAndEstadoNot(inicio, fin, "cancelado") :
            pedidoRepository.sumTotalByFechaPedidoBetweenAndEstado(inicio, fin, estadoFiltro);
        
        double totalVentas = totalVentasTemp != null ? totalVentasTemp : 0.0;
        
        Long totalPedidosTemp = estadoFiltro.equals("todos") ? 
            pedidoRepository.countByFechaPedidoBetween(inicio, fin) :
            pedidoRepository.countByFechaPedidoBetweenAndEstado(inicio, fin, estadoFiltro);
            
        long totalPedidos = totalPedidosTemp != null ? totalPedidosTemp : 0L;
        
        Long pedidosCanceladosTemp = pedidoRepository.countByFechaPedidoBetweenAndEstado(inicio, fin, "cancelado");
        long pedidosCancelados = pedidosCanceladosTemp != null ? pedidosCanceladosTemp : 0L;
        
        reporte.put("totalVentas", totalVentas);
        reporte.put("totalPedidos", totalPedidos);
        reporte.put("pedidosCancelados", pedidosCancelados);
        
        // 3. Distribución de estados de pedidos
        List<Object[]> estadosPedidos = pedidoRepository.countPedidosGroupByEstado(inicio, fin);
        List<Map<String, Object>> estados = new ArrayList<>();
        
        for (Object[] resultado : estadosPedidos) {
            Map<String, Object> estado = new HashMap<>();
            estado.put("estado", resultado[0]);
            estado.put("cantidad", resultado[1] != null ? resultado[1] : 0L);
            estados.add(estado);
        }
        
        reporte.put("estadosPedidos", estados);
        
        // 4. Producto más vendido - Manejar caso cuando no hay productos
        List<Object[]> productosMasVendidos = productoRepository.findProductosMasVendidos(inicio, fin);
        if (!productosMasVendidos.isEmpty() && productosMasVendidos.get(0)[0] != null) {
            Object[] productoMasVendido = productosMasVendidos.get(0);
            Long productoId = (Long) productoMasVendido[0];
            Long cantidadVendida = productoMasVendido[1] != null ? (Long) productoMasVendido[1] : 0L;
            
            Producto producto = productoRepository.findById(productoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productoId));
            
            Double total = cantidadVendida * producto.getPrecio();
            
            Map<String, Object> productoMap = new HashMap<>();
            productoMap.put("nombre", producto.getNombre());
            productoMap.put("cantidad", cantidadVendida);
            productoMap.put("total", total);
            reporte.put("productoMasVendido", productoMap);
        } else {
            // Si no hay productos vendidos, poner valores por defecto
            Map<String, Object> productoMap = new HashMap<>();
            productoMap.put("nombre", "No hay datos");
            productoMap.put("cantidad", 0L);
            productoMap.put("total", 0.0);
            reporte.put("productoMasVendido", productoMap);
        }
        
        // 5. Cliente frecuente - Manejar caso cuando no hay clientes
        List<Object[]> clienteFrecuente = pedidoRepository.findClienteFrecuente(inicio, fin);
        if (!clienteFrecuente.isEmpty() && clienteFrecuente.get(0)[0] != null) {
            Map<String, Object> cliente = new HashMap<>();
            cliente.put("nombre", clienteFrecuente.get(0)[0] != null ? clienteFrecuente.get(0)[0] : "N/A");
            cliente.put("telefono", clienteFrecuente.get(0)[1] != null ? clienteFrecuente.get(0)[1] : "N/A");
            cliente.put("pedidos", clienteFrecuente.get(0)[2] != null ? clienteFrecuente.get(0)[2] : 0L);
            cliente.put("total", clienteFrecuente.get(0)[3] != null ? clienteFrecuente.get(0)[3] : 0.0);
            reporte.put("clienteFrecuente", cliente);
        } else {
            // Si no hay clientes frecuentes, poner valores por defecto
            Map<String, Object> cliente = new HashMap<>();
            cliente.put("nombre", "No hay datos");
            cliente.put("telefono", "N/A");
            cliente.put("pedidos", 0L);
            cliente.put("total", 0.0);
            reporte.put("clienteFrecuente", cliente);
        }
        
        return reporte;
    }
}