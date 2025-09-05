package com.backend.pedidos_app.controller;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.backend.pedidos_app.dto.JwtResponse;
import com.backend.pedidos_app.dto.LoginRequest;
import com.backend.pedidos_app.dto.ResetPasswordRequest;
import com.backend.pedidos_app.dto.SignupRequest;
import com.backend.pedidos_app.model.Usuario;
import com.backend.pedidos_app.repository.UsuarioRepository;
import com.backend.pedidos_app.security.JwtUtils;
import com.backend.pedidos_app.service.AuthService;

//@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthService authService;
    
    @Autowired
    JwtUtils jwtUtils;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
        
        // Obtener roles del token recién generado
        List<String> roles = jwtUtils.getRolesFromJwtToken(jwtResponse.getToken());
        jwtResponse.setRoles(roles);
       
        return ResponseEntity.ok(jwtResponse);
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signUpRequest) {
        authService.registerUser(signUpRequest);
        return ResponseEntity.ok("Usuario registrado exitosamente!");
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        Optional<Usuario> usuarioOptional = usuarioRepository.findByUsername(request.getUsername());
        
        if (!usuarioOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Collections.singletonMap("message", "No se encontró un usuario con ese nombre de usuario"));
        }
        
        Usuario usuario = usuarioOptional.get();
        usuario.setPassword(passwordEncoder.encode(request.getNewPassword()));
        usuarioRepository.save(usuario);
        
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(Collections.singletonMap("message", "Contraseña actualizada exitosamente"));
    }
    
    
}