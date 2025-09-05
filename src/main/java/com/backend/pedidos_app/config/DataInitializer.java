package com.backend.pedidos_app.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.backend.pedidos_app.model.ERol;
import com.backend.pedidos_app.model.Rol;
import com.backend.pedidos_app.repository.RolRepository;


@Component
public class DataInitializer implements CommandLineRunner {
    @Autowired
    private RolRepository rolRepository;

    @Override
    public void run(String... args) throws Exception {
        if (rolRepository.findByNombre(ERol.ROLE_ADMIN).isEmpty()) {
            rolRepository.save(new Rol(ERol.ROLE_ADMIN));
        }
        if (rolRepository.findByNombre(ERol.ROLE_CLIENTE).isEmpty()) {
            rolRepository.save(new Rol(ERol.ROLE_CLIENTE));
        }
    }
}