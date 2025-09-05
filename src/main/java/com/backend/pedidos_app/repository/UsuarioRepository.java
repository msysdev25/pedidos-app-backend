package com.backend.pedidos_app.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.backend.pedidos_app.model.Usuario;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    Optional<Usuario> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    @Query("SELECT DISTINCT p.categoria FROM Producto p WHERE p.activo = true")
    List<String> findDistinctCategoriaByActivoTrue();
}
