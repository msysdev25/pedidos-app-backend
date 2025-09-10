package com.backend.pedidos_app.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

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

        try {
            LocalDateTime inicioDateTime = inicio.atStartOfDay();
            LocalDateTime finDateTime = fin.atTime(23, 59, 59);

            Map<String, Object> reporte = reporteService.generarReporte(inicioDateTime, finDateTime, estado);
            logger.info("Reporte JSON generado exitosamente");
            return ResponseEntity.ok(reporte);
        } catch (Exception e) {
            logger.error("Error al generar reporte JSON", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al generar el reporte: " + e.getMessage()));
        }
    }

    @GetMapping("/exportar")
    public ResponseEntity<?> exportarReporte(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam(required = false, defaultValue = "todos") String estado) {

        logger.info("Iniciando exportación de reporte Excel para inicio: {}, fin: {}, estado: {}", inicio, fin, estado);

        try {
            // Validar fechas
            if (inicio.isAfter(fin)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "La fecha de inicio no puede ser posterior a la fecha de fin"));
            }

            // Generar reporte
            long startTime = System.currentTimeMillis();
            Map<String, Object> reporte = reporteService.generarReporte(inicio, fin, estado);
            long reportTime = System.currentTimeMillis() - startTime;
            logger.info("Reporte generado en {} ms", reportTime);

            // Generar Excel con timeout
            startTime = System.currentTimeMillis();
            byte[] excelBytes = excelExportService.exportarReporte(reporte);
            long excelTime = System.currentTimeMillis() - startTime;
            logger.info("Excel generado en {} ms, tamaño: {} bytes", excelTime, excelBytes.length);

            // Validar tamaño
            if (excelBytes.length > MAX_FILE_SIZE) {
                logger.warn("Archivo demasiado grande: {} bytes", excelBytes.length);
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                        .body(Map.of("error", "El archivo generado es demasiado grande"));
            }

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
                    .header("X-File-Size", String.valueOf(excelBytes.length))
                    .contentLength(excelBytes.length)
                    .body(resource);

        } catch (IllegalArgumentException e) {
            logger.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Datos inválidos: " + e.getMessage()));
        } catch (IOException e) {
            logger.error("Error de E/O al generar Excel: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al generar archivo: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error inesperado al exportar reporte", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno del servidor: " + e.getMessage()));
        }
    }

    @GetMapping("/exportar-prueba")
    public ResponseEntity<?> exportarReportePrueba() {
        logger.info("Generando archivo Excel de prueba");

        try {
            byte[] excelBytes = excelExportService.exportarReporteSimple();

            if (excelBytes.length == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "No se pudo generar el archivo de prueba"));
            }

            ByteArrayResource resource = new ByteArrayResource(excelBytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"prueba.xlsx\"")
                    .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .header("X-File-Size", String.valueOf(excelBytes.length))
                    .contentLength(excelBytes.length)
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error al generar archivo de prueba", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al generar archivo de prueba: " + e.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "OK");
        status.put("timestamp", System.currentTimeMillis());
        status.put("memory", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        status.put("maxMemory", Runtime.getRuntime().maxMemory());
        return ResponseEntity.ok(status);
    }

    @GetMapping("/exportar-simulado")
    public ResponseEntity<?> exportarReporteSimulado() {
        logger.info("Generando reporte simulado");

        try {
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
                    .header("X-File-Size", String.valueOf(excelBytes.length))
                    .contentLength(excelBytes.length)
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error al generar reporte simulado", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al generar reporte simulado: " + e.getMessage()));
        }
    }
}