package com.backend.pedidos_app.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backend.pedidos_app.service.ExcelExportService;
import com.backend.pedidos_app.service.ReporteService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reportes")
@PreAuthorize("hasRole('ADMIN')")
public class ReporteController {
    private static final Logger logger = LoggerFactory.getLogger(ReporteController.class);

    private final ReporteService reporteService;
    private final ExcelExportService excelExportService;

    @Autowired
    public ReporteController(ReporteService reporteService, ExcelExportService excelExportService) {
        this.reporteService = reporteService;
        this.excelExportService = excelExportService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> generarReporte(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false, defaultValue = "todos") String estado) {
        
        logger.info("Generando reporte JSON para inicio: {}, fin: {}, estado: {}", inicio, fin, estado);
        LocalDateTime inicioDateTime = inicio.atStartOfDay();
        LocalDateTime finDateTime = fin.atTime(23, 59, 59);
        
        Map<String, Object> reporte = reporteService.generarReporte(inicioDateTime, finDateTime, estado);
        logger.info("Reporte JSON generado: {}", reporte);
        return ResponseEntity.ok(reporte);
    }
    
    @GetMapping("/exportar")
    public ResponseEntity<Resource> exportarReporte(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam(required = false, defaultValue = "todos") String estado) throws IOException {
        
        logger.info("Generando reporte Excel para inicio: {}, fin: {}, estado: {}", inicio, fin, estado);
        Map<String, Object> reporte = reporteService.generarReporte(inicio, fin, estado);
        logger.info("Reporte generado: {}", reporte);
        byte[] excelBytes = excelExportService.exportarReporte(reporte);
        
        String nombreArchivo = String.format("reporte_%s_%s.xlsx", 
            inicio.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), 
            fin.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        ByteArrayResource resource = new ByteArrayResource(excelBytes);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .contentLength(excelBytes.length)
                .body(resource);
    }

    @GetMapping("/exportar-prueba")
    public ResponseEntity<Resource> exportarReportePrueba() throws IOException {
        logger.info("Generando archivo Excel de prueba");
        byte[] excelBytes = excelExportService.exportarReporteSimple();
        
        ByteArrayResource resource = new ByteArrayResource(excelBytes);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"prueba.xlsx\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .contentLength(excelBytes.length)
                .body(resource);
    }
    
    @GetMapping("/exportar-simulado")
    public ResponseEntity<Resource> exportarReporteSimulado() throws IOException {
        Map<String, Object> reporte = new HashMap<>();
        reporte.put("totalVentas", 10000.0);
        reporte.put("totalPedidos", 50L);
        reporte.put("pedidosCancelados", 5L);
        reporte.put("meses", Arrays.asList("Ene", "Feb", "Mar"));
        reporte.put("ventas", Arrays.asList(3000.0, 4000.0, 3000.0));
        reporte.put("estadosPedidos", Arrays.asList(
            Map.of("estado", "Entregado", "cantidad", 30L),
            Map.of("estado", "Pendiente", "cantidad", 15L)
        ));
        reporte.put("productoMasVendido", Map.of("nombre", "Producto A", "cantidad", 100L, "total", 5000.0));
        reporte.put("clienteFrecuente", Map.of("nombre", "Cliente X", "telefono", "12345678", "pedidos", 10L, "total", 2000.0));
        
        byte[] excelBytes = excelExportService.exportarReporte(reporte);
        ByteArrayResource resource = new ByteArrayResource(excelBytes);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte_simulado.xlsx\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .contentLength(excelBytes.length)
                .body(resource);
    }
}