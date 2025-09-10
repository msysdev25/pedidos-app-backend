package com.backend.pedidos_app.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;


@Service
public class ExcelExportService {
    private static final Logger logger = LoggerFactory.getLogger(ExcelExportService.class);
    private static final int MAX_MEMORY_THRESHOLD = 50 * 1024 * 1024; // 50MB

    public byte[] exportarReporte(Map<String, Object> reporte) throws IOException {
        if (reporte == null || reporte.isEmpty()) {
            logger.error("El reporte es nulo o vacío");
            throw new IllegalArgumentException("El reporte no puede ser nulo o vacío");
        }

        logger.info("Generando reporte Excel con datos: {}", reporte);

        // Usar SXSSFWorkbook para mejor manejo de memoria
        SXSSFWorkbook workbook = null;
        try {
            // Crear workbook optimizado para memoria
            workbook = new SXSSFWorkbook(100); // mantener 100 filas en memoria
            workbook.setCompressTempFiles(true); // comprimir archivos temporales

            // Crear estilos una sola vez
            Map<String, CellStyle> styles = createStyles(workbook);

            // Crear hojas de manera optimizada
            createOptimizedSummarySheet(workbook, reporte, styles);
            createOptimizedSalesByMonthSheet(workbook, reporte, styles);
            createOptimizedOrderStatusSheet(workbook, reporte, styles);
            createOptimizedHighlightsSheet(workbook, reporte, styles);

            // Generar el archivo
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);

            byte[] excelBytes = outputStream.toByteArray();
            outputStream.close();

            logger.info("Archivo Excel generado exitosamente. Tamaño: {} bytes", excelBytes.length);

            // Validación básica
            if (excelBytes.length < 1000) {
                throw new IOException("Archivo generado demasiado pequeño");
            }

            return excelBytes;

        } catch (Exception e) {
            logger.error("Error al generar el reporte Excel: {}", e.getMessage(), e);
            throw new IOException("Error al generar el archivo Excel: " + e.getMessage(), e);
        } finally {
            // Limpiar recursos
            if (workbook != null) {
                try {
                    workbook.dispose(); // eliminar archivos temporales
                    workbook.close();
                } catch (Exception e) {
                    logger.warn("Error al cerrar workbook: {}", e.getMessage());
                }
            }
            // Forzar garbage collection
            System.gc();
        }
    }

    private Map<String, CellStyle> createStyles(Workbook workbook) {
        Map<String, CellStyle> styles = new HashMap<>();

        // Estilo header
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorderStyles(headerStyle);
        styles.put("header", headerStyle);

        // Estilo currency
        CellStyle currencyStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        currencyStyle.setDataFormat(format.getFormat("Q#,##0.00"));
        setBorderStyles(currencyStyle);
        currencyStyle.setAlignment(HorizontalAlignment.RIGHT);
        styles.put("currency", currencyStyle);

        // Estilo default
        CellStyle defaultStyle = workbook.createCellStyle();
        setBorderStyles(defaultStyle);
        defaultStyle.setAlignment(HorizontalAlignment.LEFT);
        styles.put("default", defaultStyle);

        // Estilo percent
        CellStyle percentStyle = workbook.createCellStyle();
        percentStyle.setDataFormat(format.getFormat("0.00%"));
        setBorderStyles(percentStyle);
        percentStyle.setAlignment(HorizontalAlignment.RIGHT);
        styles.put("percent", percentStyle);

        return styles;
    }

    private void setBorderStyles(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
    }

    private void createOptimizedSummarySheet(SXSSFWorkbook workbook, Map<String, Object> reporte,
                                             Map<String, CellStyle> styles) {
        Sheet sheet = workbook.createSheet("Resumen");

        Row headerRow = sheet.createRow(0);
        createCell(headerRow, 0, "Métrica", styles.get("header"));
        createCell(headerRow, 1, "Valor", styles.get("header"));

        int rowNum = 1;
        createSummaryRow(sheet, rowNum++, "Ventas Totales",
                safeGetDouble(reporte.get("totalVentas")), styles);
        createSummaryRow(sheet, rowNum++, "Total Pedidos",
                safeGetLong(reporte.get("totalPedidos")), styles);
        createSummaryRow(sheet, rowNum++, "Pedidos Cancelados",
                safeGetLong(reporte.get("pedidosCancelados")), styles);

        long totalPedidos = safeGetLong(reporte.get("totalPedidos"));
        long cancelados = safeGetLong(reporte.get("pedidosCancelados"));
        double tasaCancelacion = totalPedidos > 0 ? (cancelados * 100.0 / totalPedidos) / 100 : 0;

        Row rateRow = sheet.createRow(rowNum);
        createCell(rateRow, 0, "Tasa de Cancelación", styles.get("default"));
        Cell rateCell = rateRow.createCell(1);
        rateCell.setCellValue(tasaCancelacion);
        rateCell.setCellStyle(styles.get("percent"));
    }

    private void createOptimizedSalesByMonthSheet(SXSSFWorkbook workbook, Map<String, Object> reporte,
                                                  Map<String, CellStyle> styles) {
        Sheet sheet = workbook.createSheet("Ventas por Mes");

        Row headerRow = sheet.createRow(0);
        createCell(headerRow, 0, "Mes", styles.get("header"));
        createCell(headerRow, 1, "Ventas (Q)", styles.get("header"));

        List<String> meses = safeGetList(reporte.get("meses"), String.class);
        List<Double> ventas = safeGetList(reporte.get("ventas"), Double.class);

        if (meses == null || ventas == null || meses.isEmpty()) {
            createCell(sheet.createRow(1), 0, "No hay datos disponibles", styles.get("default"));
            return;
        }

        int maxSize = Math.min(meses.size(), ventas.size());
        for (int i = 0; i < maxSize; i++) {
            Row row = sheet.createRow(i + 1);
            createCell(row, 0, meses.get(i) != null ? meses.get(i) : "Desconocido", styles.get("default"));

            Cell cell = row.createCell(1);
            cell.setCellValue(ventas.get(i) != null ? ventas.get(i) : 0.0);
            cell.setCellStyle(styles.get("currency"));
        }
    }

    private void createOptimizedOrderStatusSheet(SXSSFWorkbook workbook, Map<String, Object> reporte,
                                                 Map<String, CellStyle> styles) {
        Sheet sheet = workbook.createSheet("Estados de Pedidos");

        Row headerRow = sheet.createRow(0);
        createCell(headerRow, 0, "Estado", styles.get("header"));
        createCell(headerRow, 1, "Cantidad", styles.get("header"));

        List<Map<String, Object>> estados = safeGetListOfMaps(reporte.get("estadosPedidos"));

        if (estados.isEmpty()) {
            createCell(sheet.createRow(1), 0, "No hay datos disponibles", styles.get("default"));
            return;
        }

        for (int i = 0; i < estados.size(); i++) {
            Map<String, Object> estado = estados.get(i);
            Row row = sheet.createRow(i + 1);
            createCell(row, 0, safeGetString(estado.get("estado")), styles.get("default"));

            Cell cell = row.createCell(1);
            cell.setCellValue(safeGetLong(estado.get("cantidad")));
            cell.setCellStyle(styles.get("default"));
        }
    }

    private void createOptimizedHighlightsSheet(SXSSFWorkbook workbook, Map<String, Object> reporte,
                                                Map<String, CellStyle> styles) {
        Sheet sheet = workbook.createSheet("Destacados");

        Row headerRow = sheet.createRow(0);
        createCell(headerRow, 0, "Tipo", styles.get("header"));
        createCell(headerRow, 1, "Nombre", styles.get("header"));
        createCell(headerRow, 2, "Detalle 1", styles.get("header"));
        createCell(headerRow, 3, "Detalle 2", styles.get("header"));

        int rowNum = 1;

        if (reporte.containsKey("productoMasVendido")) {
            Map<String, Object> producto = safeGetMap(reporte.get("productoMasVendido"));
            if (!producto.isEmpty() && !"No hay datos".equals(safeGetString(producto.get("nombre")))) {
                Row row = sheet.createRow(rowNum++);
                createCell(row, 0, "Producto Más Vendido", styles.get("default"));
                createCell(row, 1, safeGetString(producto.get("nombre")), styles.get("default"));
                createCell(row, 2, "Cantidad: " + safeGetLong(producto.get("cantidad")), styles.get("default"));
                createCell(row, 3, "Total: Q" + String.format("%.2f", safeGetDouble(producto.get("total"))), styles.get("default"));
            }
        }

        if (reporte.containsKey("clienteFrecuente")) {
            Map<String, Object> cliente = safeGetMap(reporte.get("clienteFrecuente"));
            if (!cliente.isEmpty() && !"No hay datos".equals(safeGetString(cliente.get("nombre")))) {
                Row row = sheet.createRow(rowNum++);
                createCell(row, 0, "Cliente Frecuente", styles.get("default"));
                createCell(row, 1, safeGetString(cliente.get("nombre")) + " (" +
                        safeGetString(cliente.get("telefono")) + ")", styles.get("default"));
                createCell(row, 2, "Total pedidos: " + safeGetLong(cliente.get("pedidos")), styles.get("default"));
                createCell(row, 3, "Total gastado: Q" + String.format("%.2f", safeGetDouble(cliente.get("total"))), styles.get("default"));
            }
        }

        if (rowNum == 1) {
            createCell(sheet.createRow(rowNum), 0, "No hay datos disponibles", styles.get("default"));
        }
    }

    private void createSummaryRow(Sheet sheet, int rowNum, String label, Number value,
                                  Map<String, CellStyle> styles) {
        Row row = sheet.createRow(rowNum);
        createCell(row, 0, label, styles.get("default"));

        Cell cell = row.createCell(1);
        if (label.contains("Total")) {
            cell.setCellValue(value != null ? value.doubleValue() : 0.0);
            cell.setCellStyle(styles.get("currency"));
        } else {
            cell.setCellValue(value != null ? value.doubleValue() : 0.0);
            cell.setCellStyle(styles.get("default"));
        }
    }

    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    // Métodos de utilidad simplificados
    private String safeGetString(Object obj) {
        return obj != null ? obj.toString() : "";
    }

    private long safeGetLong(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        return 0L;
    }

    private double safeGetDouble(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        return 0.0;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> safeGetList(Object obj, Class<T> type) {
        return obj instanceof List ? (List<T>) obj : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> safeGetListOfMaps(Object obj) {
        return obj instanceof List ? (List<Map<String, Object>>) obj : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> safeGetMap(Object obj) {
        return obj instanceof Map ? (Map<String, Object>) obj : Collections.emptyMap();
    }

    // Método de prueba simplificado
    public byte[] exportarReporteSimple() throws IOException {
        SXSSFWorkbook workbook = null;
        try {
            workbook = new SXSSFWorkbook(10);
            Sheet sheet = workbook.createSheet("Prueba");
            Row row = sheet.createRow(0);
            Cell cell = row.createCell(0);
            cell.setCellValue("¡Hola, mundo! - Prueba desde Render");

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            byte[] excelBytes = outputStream.toByteArray();
            outputStream.close();

            logger.info("Archivo Excel de prueba generado. Tamaño: {} bytes", excelBytes.length);
            return excelBytes;
        } finally {
            if (workbook != null) {
                workbook.dispose();
                workbook.close();
            }
            System.gc();
        }
    }
}