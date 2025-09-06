package com.backend.pedidos_app.config;

import com.backend.pedidos_app.model.Usuario;
import com.backend.pedidos_app.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.backend.pedidos_app.model.ERol;
import com.backend.pedidos_app.model.Rol;
import com.backend.pedidos_app.repository.RolRepository;

import java.util.HashSet;
import java.util.Set;


@Component
public class DataInitializer implements CommandLineRunner {
    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Crear roles si no existen
        Rol adminRol = null;
        if (rolRepository.findByNombre(ERol.ROLE_ADMIN).isEmpty()) {
            adminRol = rolRepository.save(new Rol(ERol.ROLE_ADMIN));
            System.out.println("Rol ADMIN creado");
        } else {
            adminRol = rolRepository.findByNombre(ERol.ROLE_ADMIN).get();
            System.out.println("Rol ADMIN ya existe");
        }

        if (rolRepository.findByNombre(ERol.ROLE_CLIENTE).isEmpty()) {
            rolRepository.save(new Rol(ERol.ROLE_CLIENTE));
            System.out.println("Rol CLIENTE creado");
        } else {
            System.out.println("Rol CLIENTE ya existe");
        }

        // Crear usuario administrador si no existe
        if (usuarioRepository.findByUsername("admin").isEmpty()) {
            Usuario admin = new Usuario();
            admin.setUsername("admin");
            admin.setEmail("admin@empresa.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setNombreCompleto("Administrador del Sistema");
            admin.setTelefono("50212345678");

            Set<Rol> roles = new HashSet<>();
            roles.add(adminRol);
            admin.setRoles(roles);

            usuarioRepository.save(admin);
            System.out.println("Usuario administrador creado:");
            System.out.println("Username: admin");
            System.out.println("Password: admin123");
            System.out.println("Email: admin@empresa.com");
        } else {
            System.out.println("Usuario administrador ya existe");
        }
    }
}