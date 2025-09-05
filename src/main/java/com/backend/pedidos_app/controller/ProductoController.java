package com.backend.pedidos_app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.backend.pedidos_app.dto.CategoriaDto;
import com.backend.pedidos_app.dto.ProductoDto;
import com.backend.pedidos_app.exception.ResourceNotFoundException;
import com.backend.pedidos_app.model.Producto;
import com.backend.pedidos_app.repository.ProductoRepository;
import com.backend.pedidos_app.service.ProductoService;
import com.backend.pedidos_app.service.SupabaseStorageService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;


//@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/productos")
public class ProductoController {
    
    @Autowired
    private ProductoService productoService;
    
    @Autowired
    private SupabaseStorageService storageService;
    
    @Autowired
    private ProductoRepository productoRepository;

    @GetMapping("/todos")
    public ResponseEntity<List<ProductoDto>> obtenerTodosLosProductos() {
        return ResponseEntity.ok(productoService.obtenerTodosLosProductos());
    }
    
    @GetMapping
    public ResponseEntity<List<ProductoDto>> obtenerTodosLosProductosActivos() {
        return ResponseEntity.ok(productoService.obtenerTodosLosProductosActivos());
    }

    @GetMapping("/categorias/todas")
    public ResponseEntity<List<CategoriaDto>> obtenerTodasLasCategorias() {
        return ResponseEntity.ok(productoService.obtenerTodasLasCategorias());
    }
    
    @GetMapping("/categorias")
    public ResponseEntity<List<CategoriaDto>> obtenerTodasLasCategoriasActivas() {
        return ResponseEntity.ok(productoService.obtenerTodasLasCategoriasActivas());
    }

    @GetMapping("/categoria/{categoriaId}")
    public ResponseEntity<List<ProductoDto>> obtenerProductosPorCategoria(
            @PathVariable Long categoriaId) {
        return ResponseEntity.ok(productoService.obtenerProductosPorCategoria(categoriaId));
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<ProductoDto>> buscarProductosPorNombre(
            @RequestParam String nombre) {
        return ResponseEntity.ok(productoService.buscarProductosPorNombre(nombre));
    }

    @GetMapping("/mas-vendidos")
    public ResponseEntity<List<ProductoDto>> obtenerProductosMasVendidos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return ResponseEntity.ok(productoService.obtenerProductosMasVendidos(inicio, fin));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoDto> obtenerProductoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.obtenerProductoPorId(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductoDto> crearProducto(
            @RequestPart("producto") ProductoDto productoDto,
            @RequestPart(value = "imagen", required = false) MultipartFile imagen) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productoService.crearProducto(productoDto, imagen));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductoDto> actualizarProducto(
            @PathVariable Long id,
            @RequestPart("producto") ProductoDto productoDto,
            @RequestPart(value = "imagen", required = false) MultipartFile imagen) {
        return ResponseEntity.ok(productoService.actualizarProducto(id, productoDto, imagen));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<ProductoDto> actualizarEstadoProducto(
            @PathVariable Long id,
            @RequestParam Boolean estado) {
        return ResponseEntity.ok(productoService.actualizarEstadoProducto(id, estado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarProducto(@PathVariable Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
        
        boolean tienePedidos = productoRepository.existsPedidosByProductoId(id);
        
        if (tienePedidos) {
            producto.setActivo(false);
            productoRepository.save(producto);
            return ResponseEntity.ok().body(Map.of(
                "message", "El producto está asociado a pedidos y ha sido desactivado",
                "action", "deactivated"
            ));
        } else {
            if (producto.getImagenUrl() != null && !producto.getImagenUrl().isEmpty()) {
                try {
                    storageService.deleteFile(producto.getImagenUrl());
                } catch (Exception e) {
                    System.err.println("Error al eliminar la imagen del producto: " + e.getMessage());
                }
            }
            productoRepository.delete(producto);
            return ResponseEntity.ok().body(Map.of(
                "message", "Producto eliminado correctamente",
                "action", "deleted"
            ));
        }
    }
}