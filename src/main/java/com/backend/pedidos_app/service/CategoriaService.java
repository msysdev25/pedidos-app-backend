package com.backend.pedidos_app.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.backend.pedidos_app.dto.CategoriaDto;
import com.backend.pedidos_app.exception.ResourceNotFoundException;
import com.backend.pedidos_app.model.Categoria;
import com.backend.pedidos_app.repository.CategoriaRepository;
import com.backend.pedidos_app.repository.ProductoRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class CategoriaService {
    
    @Autowired
    private CategoriaRepository categoriaRepository;
    
    @Autowired
    private ProductoRepository productoRepository;

    public List<CategoriaDto> obtenerTodasLasCategoriasActivas() {
        return categoriaRepository.findByActivoTrue().stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }

    public CategoriaDto crearCategoria(CategoriaDto categoriaDto) {
        if (categoriaRepository.existsByNombre(categoriaDto.getNombre())) {
            throw new IllegalArgumentException("Ya existe una categoría con ese nombre");
        }
        
        Categoria categoria = new Categoria();
        categoria.setNombre(categoriaDto.getNombre());
        categoria.setActivo(true);
        
        Categoria categoriaGuardada = categoriaRepository.save(categoria);
        return convertirADto(categoriaGuardada);
    }

    public CategoriaDto actualizarCategoria(Long id, CategoriaDto categoriaDto) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id: " + id));
        
        if (!categoria.getNombre().equals(categoriaDto.getNombre()) && 
            categoriaRepository.existsByNombre(categoriaDto.getNombre())) {
            throw new IllegalArgumentException("Ya existe una categoría con ese nombre");
        }
        
        categoria.setNombre(categoriaDto.getNombre());
        Categoria categoriaActualizada = categoriaRepository.save(categoria);
        return convertirADto(categoriaActualizada);
    }
    
    @Transactional
    public Map<String, Object> toggleEstadoCategoria(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id: " + id));
        
        if (categoria.getActivo()) {
            // Si está activa, verificamos si podemos desactivarla
            boolean tieneProductosActivos = productoRepository.existsByCategoriaIdAndActivoTrue(id);
            
            if (tieneProductosActivos) {
                throw new IllegalStateException("No se puede desactivar una categoría con productos activos asociados");
            }
            
            categoria.setActivo(false);
        } else {
            // Si está inactiva, la reactivamos
            categoria.setActivo(true);
        }
        
        categoriaRepository.save(categoria);
        return Map.of(
            "action", categoria.getActivo() ? "activated" : "deactivated",
            "message", categoria.getActivo() ? "Categoría reactivada correctamente" : "Categoría desactivada correctamente"
        );
    }

    @Transactional
    public Map<String, Object> eliminarCategoria(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id: " + id));
        
        boolean tieneProductos = productoRepository.existsByCategoriaId(id);
        
        if (tieneProductos) {
            categoria.setActivo(false);
            categoriaRepository.save(categoria);
            return Map.of(
                "action", "deactivated",
                "message", "La categoría tiene productos asociados y ha sido desactivada"
            );
        } else {
            categoriaRepository.delete(categoria);
            return Map.of(
                "action", "deleted",
                "message", "Categoría eliminada correctamente"
            );
        }
    }

    private CategoriaDto convertirADto(Categoria categoria) {
        CategoriaDto dto = new CategoriaDto();
        dto.setId(categoria.getId());
        dto.setNombre(categoria.getNombre());
        dto.setActivo(categoria.getActivo());
        return dto;
    }
}