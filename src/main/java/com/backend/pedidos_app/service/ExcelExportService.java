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
import java.util.List;
import java.util.Map;

@Service
public class ExcelExportService {
    private static final Logger logger = LoggerFactory.getLogger(ExcelExportService.class);

    public byte[] exportarReporte(Map<String, Object> reporte) throws IOException {
        if (reporte == null || reporte.isEmpty()) {
            logger.error("El reporte es nulo o vacío");
            throw new IllegalArgumentException("El reporte no puede ser nulo o vacío");
        }

        logger.info("Generando reporte Excel con datos: {}", reporte);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            // Crear estilos
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle defaultStyle = createDefaultStyle(workbook);
            CellStyle percentStyle = createPercentStyle(workbook);

            // Crear hojas
            createSummarySheet(workbook, reporte, headerStyle, currencyStyle, defaultStyle, percentStyle);
            createSalesByMonthSheet(workbook, reporte, headerStyle, currencyStyle, defaultStyle);
            createOrderStatusSheet(workbook, reporte, headerStyle, defaultStyle);
            createHighlightsSheet(workbook, reporte, headerStyle, defaultStyle);

            // Validar workbook
            validateWorkbook(workbook);

            // Escribir y cerrar el workbook
            workbook.write(outputStream);
            workbook.close();

            byte[] excelBytes = outputStream.toByteArray();
            validateExcelFile(excelBytes);

            logger.info("Archivo Excel generado exitosamente. Tamaño: {} bytes", excelBytes.length);
            return excelBytes;
        } catch (Exception e) {
            logger.error("Error al generar el reporte Excel", e);
            throw new IOException("Error al generar el archivo Excel: " + e.getMessage(), e);
        }
    }

    // Método de prueba para generar un archivo Excel simple
    public byte[] exportarReporteSimple() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Prueba");
            Row row = sheet.createRow(0);
            Cell cell = row.createCell(0);
            cell.setCellValue("¡Hola, mundo!");

            workbook.write(outputStream);
            workbook.close();
            byte[] excelBytes = outputStream.toByteArray();
            validateExcelFile(excelBytes);
            logger.info("Archivo Excel de prueba generado. Tamaño: {} bytes", excelBytes.length);
            return excelBytes;
        } catch (Exception e) {
            logger.error("Error al generar archivo de prueba", e);
            throw new IOException("Error al generar archivo de prueba: " + e.getMessage(), e);
        }
    }

    private void validateWorkbook(Workbook workbook) {
        if (workbook.getNumberOfSheets() == 0) {
            throw new IllegalStateException("El workbook no contiene hojas");
        }

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            if (sheet.getPhysicalNumberOfRows() == 0) {
                logger.warn("La hoja '{}' está vacía", sheet.getSheetName());
            }
        }
    }

    private void validateExcelFile(byte[] excelBytes) throws IOException {
        if (excelBytes.length < 100) {
            throw new IOException("El archivo generado es demasiado pequeño para ser un XLSX válido");
        }
        if (excelBytes[0] != 0x50 || excelBytes[1] != 0x4B) {
            throw new IOException("El archivo generado no es un ZIP válido");
        }

        // Validar que el archivo sea un XLSX válido
        try (ByteArrayInputStream bais = new ByteArrayInputStream(excelBytes);
             Workbook testWorkbook = new XSSFWorkbook(bais)) {
            logger.info("Validación del archivo XLSX exitosa");
        } catch (Exception e) {
            throw new IOException("El archivo generado no es un XLSX válido: " + e.getMessage(), e);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorderStyles(style);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("Q#,##0.00"));
        setBorderStyles(style);
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createPercentStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("0.00%"));
        setBorderStyles(style);
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createDefaultStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        setBorderStyles(style);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private void setBorderStyles(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
    }

    private void createSummarySheet(Workbook workbook, Map<String, Object> reporte, 
                                   CellStyle headerStyle, CellStyle currencyStyle, 
                                   CellStyle defaultStyle, CellStyle percentStyle) {
        Sheet sheet = workbook.createSheet("Resumen");
        
        sheet.setColumnWidth(0, 25 * 256);
        sheet.setColumnWidth(1, 20 * 256);

        Row headerRow = sheet.createRow(0);
        createCell(headerRow, 0, "Métrica", headerStyle);
        createCell(headerRow, 1, "Valor", headerStyle);

        int rowNum = 1;
        createSummaryRow(sheet, rowNum++, "Ventas Totales", 
                        safeGetDouble(reporte.get("totalVentas")), currencyStyle, defaultStyle);
        createSummaryRow(sheet, rowNum++, "Total Pedidos", 
                        safeGetLong(reporte.get("totalPedidos")), defaultStyle, defaultStyle);
        createSummaryRow(sheet, rowNum++, "Pedidos Cancelados", 
                        safeGetLong(reporte.get("pedidosCancelados")), defaultStyle, defaultStyle);

        long totalPedidos = safeGetLong(reporte.get("totalPedidos"));
        long cancelados = safeGetLong(reporte.get("pedidosCancelados"));
        double tasaCancelacion = totalPedidos > 0 ? (cancelados * 100.0 / totalPedidos) / 100 : 0;

        Row rateRow = sheet.createRow(rowNum++);
        createCell(rateRow, 0, "Tasa de Cancelación", defaultStyle);
        Cell rateCell = rateRow.createCell(1);
        rateCell.setCellValue(tasaCancelacion);
        rateCell.setCellStyle(percentStyle);

        autoSizeColumns(sheet, 2);
    }

    private void createSalesByMonthSheet(Workbook workbook, Map<String, Object> reporte,
                                        CellStyle headerStyle, CellStyle currencyStyle, 
                                        CellStyle defaultStyle) {
        Sheet sheet = workbook.createSheet("Ventas por Mes");

        sheet.setColumnWidth(0, 15 * 256);
        sheet.setColumnWidth(1, 20 * 256);

        Row headerRow = sheet.createRow(0);
        createCell(headerRow, 0, "Mes", headerStyle);
        createCell(headerRow, 1, "Ventas (Q)", headerStyle);

        List<String> meses = safeGetList(reporte.get("meses"), String.class);
        List<Double> ventas = safeGetList(reporte.get("ventas"), Double.class);

        if (meses == null || ventas == null || meses.size() != ventas.size()) {
            logger.warn("Datos inválidos para Ventas por Mes: meses={}, ventas={}", meses, ventas);
            createCell(sheet.createRow(1), 0, "No hay datos disponibles", defaultStyle);
            return;
        }

        for (int i = 0; i < meses.size(); i++) {
            Row row = sheet.createRow(i + 1);
            createCell(row, 0, meses.get(i) != null ? meses.get(i) : "Desconocido", defaultStyle);

            Cell cell = row.createCell(1);
            cell.setCellValue(ventas.get(i) != null ? ventas.get(i) : 0.0);
            cell.setCellStyle(currencyStyle);
        }

        autoSizeColumns(sheet, 2);
    }

    private void createOrderStatusSheet(Workbook workbook, Map<String, Object> reporte,
                                       CellStyle headerStyle, CellStyle defaultStyle) {
        Sheet sheet = workbook.createSheet("Estados de Pedidos");

        sheet.setColumnWidth(0, 25 * 256);
        sheet.setColumnWidth(1, 15 * 256);

        Row headerRow = sheet.createRow(0);
        createCell(headerRow, 0, "Estado", headerStyle);
        createCell(headerRow, 1, "Cantidad", headerStyle);

        List<Map<String, Object>> estados = safeGetListOfMaps(reporte.get("estadosPedidos"));

        if (estados.isEmpty()) {
            logger.warn("No hay datos de estados de pedidos");
            createCell(sheet.createRow(1), 0, "No hay datos disponibles", defaultStyle);
            return;
        }

        for (int i = 0; i < estados.size(); i++) {
            Map<String, Object> estado = estados.get(i);
            Row row = sheet.createRow(i + 1);
            createCell(row, 0, safeGetString(estado.get("estado")), defaultStyle);

            Cell cell = row.createCell(1);
            cell.setCellValue(safeGetLong(estado.get("cantidad")));
            cell.setCellStyle(defaultStyle);
        }

        autoSizeColumns(sheet, 2);
    }

    private void createHighlightsSheet(Workbook workbook, Map<String, Object> reporte,
                                      CellStyle headerStyle, CellStyle defaultStyle) {
        Sheet sheet = workbook.createSheet("Destacados");

        sheet.setColumnWidth(0, 25 * 256);
        sheet.setColumnWidth(1, 30 * 256);
        sheet.setColumnWidth(2, 30 * 256);
        sheet.setColumnWidth(3, 30 * 256);

        Row headerRow = sheet.createRow(0);
        createCell(headerRow, 0, "Tipo", headerStyle);
        createCell(headerRow, 1, "Nombre", headerStyle);
        createCell(headerRow, 2, "Detalle 1", headerStyle);
        createCell(headerRow, 3, "Detalle 2", headerStyle);

        int rowNum = 1;

        if (reporte.containsKey("productoMasVendido")) {
            Map<String, Object> producto = safeGetMap(reporte.get("productoMasVendido"));
            if (!producto.isEmpty()) {
                Row row = sheet.createRow(rowNum++);
                createCell(row, 0, "Producto Más Vendido", defaultStyle);
                createCell(row, 1, safeGetString(producto.get("nombre")), defaultStyle);
                createCell(row, 2, "Cantidad: " + safeGetLong(producto.get("cantidad")), defaultStyle);
                createCell(row, 3, "Total: Q" + safeGetDouble(producto.get("total")), defaultStyle);
            }
        }

        if (reporte.containsKey("clienteFrecuente")) {
            Map<String, Object> cliente = safeGetMap(reporte.get("clienteFrecuente"));
            if (!cliente.isEmpty()) {
                Row row = sheet.createRow(rowNum++);
                createCell(row, 0, "Cliente Frecuente", defaultStyle);
                createCell(row, 1, safeGetString(cliente.get("nombre")) + " (" + 
                                  safeGetString(cliente.get("telefono")) + ")", defaultStyle);
                createCell(row, 2, "Total pedidos: " + safeGetLong(cliente.get("pedidos")), defaultStyle);
                createCell(row, 3, "Total gastado: Q" + safeGetDouble(cliente.get("total")), defaultStyle);
            }
        }

        if (rowNum == 1) {
            createCell(sheet.createRow(rowNum), 0, "No hay datos disponibles", defaultStyle);
        }

        autoSizeColumns(sheet, 4);
    }

    private void createSummaryRow(Sheet sheet, int rowNum, String label, Number value, 
                                 CellStyle valueStyle, CellStyle labelStyle) {
        Row row = sheet.createRow(rowNum);
        createCell(row, 0, label, labelStyle);

        Cell cell = row.createCell(1);
        cell.setCellValue(value != null ? value.doubleValue() : 0.0);
        cell.setCellStyle(valueStyle);
    }

    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void autoSizeColumns(Sheet sheet, int numColumns) {
        for (int i = 0; i < numColumns; i++) {
            sheet.autoSizeColumn(i);
        }
    }

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
}