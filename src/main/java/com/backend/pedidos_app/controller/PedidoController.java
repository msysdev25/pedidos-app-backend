package com.backend.pedidos_app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.backend.pedidos_app.dto.PedidoRequest;
import com.backend.pedidos_app.dto.PedidoResponse;
import com.backend.pedidos_app.dto.ReporteRequest;
import com.backend.pedidos_app.service.PedidoService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

//@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    @Autowired
    private PedidoService pedidoService;

    @PostMapping("/crear")
    public ResponseEntity<PedidoResponse> crearPedido(@RequestBody PedidoRequest pedidoRequest) {
        return ResponseEntity.ok(pedidoService.crearPedido(pedidoRequest));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PedidoResponse>> obtenerTodosLosPedidos() {
        return ResponseEntity.ok(pedidoService.obtenerTodosLosPedidos());
    }

    @GetMapping("/estado/{estado}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PedidoResponse>> obtenerPedidosPorEstado(@PathVariable String estado) {
        return ResponseEntity.ok(pedidoService.obtenerPedidosPorEstado(estado));
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PedidoResponse> actualizarEstadoPedido(
            @PathVariable Long id, @RequestParam String estado) {
        return ResponseEntity.ok(pedidoService.actualizarEstadoPedido(id, estado));
    }

    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasRole('CLIENTE') or hasRole('ADMIN')")
    public ResponseEntity<List<PedidoResponse>> obtenerPedidosPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(pedidoService.obtenerPedidosPorUsuario(usuarioId));
    }

    @GetMapping("/reporte")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PedidoResponse>> generarReporte(
            @ModelAttribute ReporteRequest reporteRequest) {
        return ResponseEntity.ok(pedidoService.obtenerReportePedidos(
            reporteRequest.getInicio(), 
            reporteRequest.getFin()
        ));
    }
    
 // Agregar este nuevo método para el dashboard:
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> obtenerDatosDashboard(
            @ModelAttribute ReporteRequest reporteRequest) {
        return ResponseEntity.ok(pedidoService.obtenerEstadisticasDashboard(
            reporteRequest.getInicio(),
            reporteRequest.getFin()
        ));
    }
    
    @PostMapping("/upload-comprobante/{pedidoId}")
    @PreAuthorize("permitAll()") // Permitir acceso sin autenticación
    public ResponseEntity<PedidoResponse> uploadComprobante(
            @PathVariable Long pedidoId,
            @RequestParam("file") MultipartFile file) {
        
        PedidoResponse response = pedidoService.uploadComprobante(pedidoId, file);
        return ResponseEntity.ok(response);
    }
}