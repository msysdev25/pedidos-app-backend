package com.backend.pedidos_app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.backend.pedidos_app.dto.JwtResponse;
import com.backend.pedidos_app.dto.LoginRequest;
import com.backend.pedidos_app.dto.SignupRequest;
import com.backend.pedidos_app.model.ERol;
import com.backend.pedidos_app.model.Rol;
import com.backend.pedidos_app.model.Usuario;
import com.backend.pedidos_app.repository.RolRepository;
import com.backend.pedidos_app.repository.UsuarioRepository;
import com.backend.pedidos_app.security.JwtUtils;
import com.backend.pedidos_app.security.UserDetailsImpl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    RolRepository rolRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles);
    }

    public void registerUser(SignupRequest signUpRequest) {
        if (usuarioRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new RuntimeException("Error: El nombre de usuario ya está en uso!");
        }

        if (usuarioRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new RuntimeException("Error: El email ya está en uso!");
        }

        Usuario usuario = new Usuario(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()),
                signUpRequest.getNombreCompleto(),
                signUpRequest.getTelefono());

        Set<String> strRoles = signUpRequest.getRoles();
        Set<Rol> roles = new HashSet<>();

        if (strRoles == null) {
            Rol clienteRole = rolRepository.findByNombre(ERol.ROLE_CLIENTE)
                    .orElseThrow(() -> new RuntimeException("Error: Rol no encontrado."));
            roles.add(clienteRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Rol adminRole = rolRepository.findByNombre(ERol.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Rol no encontrado."));
                        roles.add(adminRole);
                        break;
                    default:
                        Rol clienteRole = rolRepository.findByNombre(ERol.ROLE_CLIENTE)
                                .orElseThrow(() -> new RuntimeException("Error: Rol no encontrado."));
                        roles.add(clienteRole);
                }
            });
        }

        usuario.setRoles(roles);
        usuarioRepository.save(usuario);
    }
}