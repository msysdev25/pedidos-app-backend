package com.backend.pedidos_app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.backend.pedidos_app.dto.PedidoProductoDto;
import com.backend.pedidos_app.dto.PedidoRequest;
import com.backend.pedidos_app.dto.PedidoResponse;
import com.backend.pedidos_app.dto.PedidoResponse.PedidoProductoResponse;
import com.backend.pedidos_app.exception.ResourceNotFoundException;
import com.backend.pedidos_app.model.Pedido;
import com.backend.pedidos_app.model.PedidoProducto;
import com.backend.pedidos_app.model.Producto;
import com.backend.pedidos_app.model.Usuario;
import com.backend.pedidos_app.repository.PedidoRepository;
import com.backend.pedidos_app.repository.ProductoRepository;
import com.backend.pedidos_app.repository.UsuarioRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private SupabaseStorageService storageService;

    @Value("${app.recargo.domicilio}")
    private Double recargoDomicilio;


    @Transactional
    public PedidoResponse crearPedido(PedidoRequest pedidoRequest) {
        Pedido pedido = new Pedido();
        pedido.setNombreCliente(pedidoRequest.getNombreCliente());
        pedido.setTelefonoCliente(pedidoRequest.getTelefonoCliente());
        pedido.setDireccion(pedidoRequest.getDireccion());
        pedido.setTipoEntrega(pedidoRequest.getTipoEntrega());
        pedido.setTipoPago(pedidoRequest.getTipoPago());
        
        // Aplicar recargo si es domicilio
        if ("domicilio".equals(pedidoRequest.getTipoEntrega())) {
            pedido.setRecargoDomicilio(recargoDomicilio);
        } else {
            pedido.setRecargoDomicilio(0.0);
        }

        // Asociar usuario si está registrado
        if (pedidoRequest.getUsuarioId() != null) {
            Usuario usuario = usuarioRepository.findById(pedidoRequest.getUsuarioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + pedidoRequest.getUsuarioId()));
            pedido.setUsuario(usuario);
        }

        // Procesar productos del pedido
        List<PedidoProducto> productos = new ArrayList<>();
        Double subtotal = 0.0;

        int orden = 0;
        for (PedidoProductoDto productoDto : pedidoRequest.getProductos()) {
            Producto producto = productoRepository.findById(productoDto.getProductoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

            PedidoProducto pedidoProducto = new PedidoProducto();
            pedidoProducto.setPedido(pedido);
            pedidoProducto.setProducto(producto);
            pedidoProducto.setCantidad(productoDto.getCantidad());
            pedidoProducto.setPersonalizaciones(productoDto.getPersonalizaciones());
            pedidoProducto.setPrecioUnitario(producto.getPrecio());
            pedidoProducto.setOrden(orden++); // Asignar orden secuencial

            productos.add(pedidoProducto);
            subtotal += producto.getPrecio() * productoDto.getCantidad();
        }

        pedido.setProductos(productos);
        pedido.setTotal(subtotal + pedido.getRecargoDomicilio());
        pedido.setEstado("pendiente");

        Pedido pedidoGuardado = pedidoRepository.save(pedido);
        return convertirARespuesta(pedidoGuardado);
    }

    public List<PedidoResponse> obtenerTodosLosPedidos() {
        return pedidoRepository.findAllWithOrderedProducts().stream()
                .map(this::convertirARespuesta)
                .collect(Collectors.toList());
    }

    public List<PedidoResponse> obtenerPedidosPorEstado(String estado) {
        return pedidoRepository.findByEstadoWithOrderedProducts(estado).stream()
                .map(this::convertirARespuesta)
                .collect(Collectors.toList());
    }

    public PedidoResponse actualizarEstadoPedido(Long id, String estado) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con id: " + id));

        pedido.setEstado(estado);
        Pedido pedidoActualizado = pedidoRepository.save(pedido);
        return convertirARespuesta(pedidoActualizado);
    }

    public List<PedidoResponse> obtenerPedidosPorUsuario(Long usuarioId) {
        return pedidoRepository.findByUsuarioIdWithOrderedProducts(usuarioId).stream()
                .map(this::convertirARespuesta)
                .collect(Collectors.toList());
    }

    public List<PedidoResponse> obtenerReportePedidos(LocalDateTime inicio, LocalDateTime fin) {
        return pedidoRepository.findByFechaPedidoBetween(inicio, fin).stream()
                .map(this::convertirARespuesta)
                .collect(Collectors.toList());
    }

    private PedidoResponse convertirARespuesta(Pedido pedido) {
        PedidoResponse respuesta = new PedidoResponse();
        respuesta.setId(pedido.getId());
        respuesta.setNombreCliente(pedido.getNombreCliente());
        respuesta.setTelefonoCliente(pedido.getTelefonoCliente());
        respuesta.setDireccion(pedido.getDireccion());
        respuesta.setTipoEntrega(pedido.getTipoEntrega());
        respuesta.setRecargoDomicilio(pedido.getRecargoDomicilio());
        respuesta.setTipoPago(pedido.getTipoPago());
        respuesta.setComprobanteUrl(pedido.getComprobanteUrl());
        respuesta.setTotal(pedido.getTotal());
        respuesta.setEstado(pedido.getEstado());
        respuesta.setFechaPedido(pedido.getFechaPedido());

        List<PedidoResponse.PedidoProductoResponse> productosRespuesta = new ArrayList<>();
        for (PedidoProducto pp : pedido.getProductos()) {
            PedidoResponse.PedidoProductoResponse ppr = new PedidoResponse.PedidoProductoResponse();
            ppr.setNombreProducto(pp.getProducto().getNombre());
            ppr.setCantidad(pp.getCantidad());
            ppr.setPersonalizaciones(pp.getPersonalizaciones());
            ppr.setPrecioUnitario(pp.getPrecioUnitario());
            productosRespuesta.add(ppr);
        }
        
        respuesta.setProductos(productosRespuesta);
        return respuesta;
    }
    


    public PedidoResponse uploadComprobante(Long id, MultipartFile file) {
        if (id == 0) {
            throw new IllegalArgumentException("El pedido debe crearse primero antes de subir el comprobante");
        }
        
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con id: " + id));

        try {
            String comprobanteUrl = storageService.uploadFile(file, "comprobantes");
            pedido.setComprobanteUrl(comprobanteUrl);
            
            // Cambiar estado si es pago por transferencia
            if ("transferencia".equals(pedido.getTipoPago())) {
                pedido.setEstado("pendiente_verificacion");
            }

            Pedido pedidoActualizado = pedidoRepository.save(pedido);
            return convertirARespuesta(pedidoActualizado);
        } catch (IOException e) {
            throw new RuntimeException("Error al subir el comprobante de pago", e);
        }
    }
    
    public Map<String, Object> obtenerEstadisticasDashboard(LocalDateTime inicio, LocalDateTime fin) {
        Map<String, Object> estadisticas = new HashMap<>();
        
        // Total pedidos en el rango (incluyendo cancelados)
        long totalPedidos = pedidoRepository.countByFechaPedidoBetween(inicio, fin);
        estadisticas.put("totalPedidos", totalPedidos);
        
        // Ganancias totales en el rango (EXCLUYENDO cancelados)
        Double gananciasTotales = pedidoRepository.sumTotalByFechaPedidoBetweenAndEstadoNot(inicio, fin, "cancelado");
        estadisticas.put("gananciasTotales", gananciasTotales != null ? gananciasTotales : 0.0);
        
        // Pedidos hoy (incluyendo cancelados)
        LocalDateTime hoyInicio = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime hoyFin = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        long pedidosHoy = pedidoRepository.countByFechaPedidoBetween(hoyInicio, hoyFin);
        estadisticas.put("pedidosHoy", pedidosHoy);
        
        // Pedidos pendientes (solo pendientes)
        long pedidosPendientes = pedidoRepository.countByEstado("pendiente");
        estadisticas.put("pedidosPendientes", pedidosPendientes);
        
        // Últimos pedidos (5 más recientes, incluyendo cancelados)
        List<Pedido> ultimosPedidos = pedidoRepository.findTop5ByOrderByFechaPedidoDesc();
        estadisticas.put("ultimosPedidos", ultimosPedidos.stream()
            .map(this::convertirARespuesta)
            .collect(Collectors.toList()));
        
        // Pedidos por mes (EXCLUYENDO cancelados)
        List<Object[]> pedidosPorMes = pedidoRepository.countPedidosGroupByMonthAndEstadoNot(inicio, fin, "cancelado");
        List<String> meses = new ArrayList<>();
        List<Long> conteos = new ArrayList<>();
        
        for (Object[] resultado : pedidosPorMes) {
            meses.add((String) resultado[0]);
            conteos.add((Long) resultado[1]);
        }
        
        estadisticas.put("meses", meses);
        estadisticas.put("pedidosPorMes", conteos);
        
        return estadisticas;
    }
}