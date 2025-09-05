package com.backend.pedidos_app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.backend.pedidos_app.dto.CategoriaDto;
import com.backend.pedidos_app.dto.ProductoDto;
import com.backend.pedidos_app.exception.ResourceNotFoundException;
import com.backend.pedidos_app.model.Categoria;
import com.backend.pedidos_app.model.Producto;
import com.backend.pedidos_app.repository.CategoriaRepository;
import com.backend.pedidos_app.repository.ProductoRepository;


import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;

@Service
public class ProductoService {
    
    @Autowired
    private ProductoRepository productoRepository;
    
    @Autowired
    private CategoriaRepository categoriaRepository;
    
    @Autowired
    private SupabaseStorageService storageService;
    
    public List<ProductoDto> obtenerTodosLosProductos() {
        return productoRepository.findAll().stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }

    public List<ProductoDto> obtenerTodosLosProductosActivos() {
        return productoRepository.findByActivoTrue().stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }

    public List<ProductoDto> obtenerProductosPorCategoria(Long categoriaId) {
        return productoRepository.findByCategoriaIdAndActivoTrue(categoriaId).stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }
    
    // Cambiar este para incluir inactivos
    public List<CategoriaDto> obtenerTodasLasCategorias() {
        return categoriaRepository.findAll().stream()
                .map(this::convertirCategoriaADto)
                .collect(Collectors.toList());
    }
    
    public List<CategoriaDto> obtenerTodasLasCategoriasActivas() {
        return categoriaRepository.findByActivoTrue().stream()
                .map(this::convertirCategoriaADto)
                .collect(Collectors.toList());
    }

    public ProductoDto crearProducto(ProductoDto productoDto, MultipartFile imagen) {
        // Validar que la categoría existe
        Categoria categoria = categoriaRepository.findById(productoDto.getCategoria().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id: " + productoDto.getCategoria().getId()));
        
        // Validar que la categoría está activa
        if (!categoria.getActivo()) {
            throw new IllegalStateException("No se puede asignar una categoría inactiva a un producto");
        }

        Producto producto = new Producto();
        producto.setNombre(productoDto.getNombre());
        producto.setDescripcion(productoDto.getDescripcion());
        producto.setPrecio(productoDto.getPrecio());
        producto.setCategoria(categoria);
        producto.setActivo(true);

        if (imagen != null && !imagen.isEmpty()) {
            try {
                String imageUrl = storageService.uploadFile(imagen, "productos");
                producto.setImagenUrl(imageUrl);
            } catch (IOException e) {
                throw new RuntimeException("Error al subir la imagen del producto", e);
            }
        }

        Producto productoGuardado = productoRepository.save(producto);
        return convertirADto(productoGuardado);
    }

    public ProductoDto actualizarProducto(Long id, ProductoDto productoDto, MultipartFile imagen) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));

        // Validar y actualizar categoría si cambió
        if (!producto.getCategoria().getId().equals(productoDto.getCategoria().getId())) {
            Categoria nuevaCategoria = categoriaRepository.findById(productoDto.getCategoria().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id: " + productoDto.getCategoria().getId()));
            
            if (!nuevaCategoria.getActivo()) {
                throw new IllegalStateException("No se puede asignar una categoría inactiva a un producto");
            }
            
            producto.setCategoria(nuevaCategoria);
        }

        producto.setNombre(productoDto.getNombre());
        producto.setDescripcion(productoDto.getDescripcion());
        producto.setPrecio(productoDto.getPrecio());
        producto.setActivo(productoDto.getActivo());

        if (imagen != null && !imagen.isEmpty()) {
            try {
                // Eliminar imagen anterior si existe
                if (producto.getImagenUrl() != null) {
                    try {
                        storageService.deleteFile(producto.getImagenUrl());
                    } catch (Exception e) {
                        // Loggear el error pero continuar con la actualización
                        System.err.println("Error al eliminar la imagen anterior: " + e.getMessage());
                    }
                }
                
                // Subir nueva imagen
                String imageUrl = storageService.uploadFile(imagen, "productos");
                producto.setImagenUrl(imageUrl);
            } catch (IOException e) {
                throw new RuntimeException("Error al actualizar la imagen del producto", e);
            }
        }

        Producto productoActualizado = productoRepository.save(producto);
        return convertirADto(productoActualizado);
    }

    @Transactional
    public void desactivarProducto(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));

        producto.setActivo(false);
        productoRepository.save(producto);
    }

    public ProductoDto actualizarEstadoProducto(Long id, Boolean estado) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
        
        producto.setActivo(estado);
        Producto productoActualizado = productoRepository.save(producto);
        return convertirADto(productoActualizado);
    }
    
    public ProductoDto obtenerProductoPorId(Long id) {
        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
        return convertirADto(producto);
    }

    private ProductoDto convertirADto(Producto producto) {
        ProductoDto dto = new ProductoDto();
        dto.setId(producto.getId());
        dto.setNombre(producto.getNombre());
        dto.setDescripcion(producto.getDescripcion());
        dto.setPrecio(producto.getPrecio());
        dto.setCategoria(convertirCategoriaADto(producto.getCategoria()));
        dto.setImagenUrl(producto.getImagenUrl());
        dto.setActivo(producto.getActivo());
        return dto;
    }

    private CategoriaDto convertirCategoriaADto(Categoria categoria) {
        CategoriaDto dto = new CategoriaDto();
        dto.setId(categoria.getId());
        dto.setNombre(categoria.getNombre());
        dto.setActivo(categoria.getActivo());
        return dto;
    }

    public List<ProductoDto> buscarProductosPorNombre(String nombre) {
        return productoRepository.findByNombreContainingIgnoreCaseAndActivoTrue(nombre).stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }
    
    public List<ProductoDto> obtenerProductosMasVendidos(LocalDateTime inicio, LocalDateTime fin) {
        List<Object[]> resultados = productoRepository.findProductosMasVendidos(inicio, fin);
        
        return resultados.stream()
                .map(result -> {
                    Long productoId = (Long) result[0];
                    Producto producto = productoRepository.findById(productoId)
                            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + productoId));
                    return convertirADto(producto);
                })
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void eliminarProducto(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
        
        // Verificar si el producto está asociado a algún pedido
        boolean tienePedidos = productoRepository.existsPedidosByProductoId(id);
        
        if (tienePedidos) {
            // Si tiene pedidos, solo desactivamos
            producto.setActivo(false);
            productoRepository.save(producto);
        } else {
            // Si no tiene pedidos, eliminamos la imagen y luego el producto
            if (producto.getImagenUrl() != null && !producto.getImagenUrl().isEmpty()) {
                try {
                    storageService.deleteFile(producto.getImagenUrl());
                } catch (Exception e) {
                    // Loggear el error pero continuar con la eliminación
                    System.err.println("Error al eliminar la imagen del producto: " + e.getMessage());
                }
            }
            productoRepository.delete(producto);
        }
    }

    public boolean existsPedidosByProductoId(Long productoId) {
        return productoRepository.existsPedidosByProductoId(productoId);
    }
}