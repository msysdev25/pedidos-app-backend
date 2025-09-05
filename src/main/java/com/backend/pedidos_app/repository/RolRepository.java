package com.backend.pedidos_app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.backend.pedidos_app.model.ERol;
import com.backend.pedidos_app.model.Rol;

import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<Rol, Integer> {
    Optional<Rol> findByNombre(ERol nombre);
}