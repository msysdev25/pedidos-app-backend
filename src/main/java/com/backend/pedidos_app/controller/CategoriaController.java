package com.backend.pedidos_app.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.backend.pedidos_app.dto.CategoriaDto;
import com.backend.pedidos_app.service.CategoriaService;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;



@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {
    
    @Autowired
    private CategoriaService categoriaService;

    @GetMapping
    public ResponseEntity<List<CategoriaDto>> obtenerTodasLasCategoriasActivas() {
        return ResponseEntity.ok(categoriaService.obtenerTodasLasCategoriasActivas());
    }

    @PostMapping
    public ResponseEntity<CategoriaDto> crearCategoria(@RequestBody CategoriaDto categoriaDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoriaService.crearCategoria(categoriaDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaDto> actualizarCategoria(
            @PathVariable Long id, @RequestBody CategoriaDto categoriaDto) {
        return ResponseEntity.ok(categoriaService.actualizarCategoria(id, categoriaDto));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<?> toggleEstadoCategoria(@PathVariable Long id) {
        try {
            Map<String, Object> response = categoriaService.toggleEstadoCategoria(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarCategoria(@PathVariable Long id) {
        Map<String, Object> response = categoriaService.eliminarCategoria(id);
        return ResponseEntity.ok(response);
    }
}