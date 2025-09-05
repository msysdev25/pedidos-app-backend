package com.backend.pedidos_app.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.pedidos_app.dto.CambiarPasswordDTO;
import com.backend.pedidos_app.dto.UsuarioDTO;
import com.backend.pedidos_app.model.Usuario;
import com.backend.pedidos_app.repository.UsuarioRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getUsuarioById(@PathVariable Long id) {
        return usuarioRepository.findById(id)
            .map(usuario -> {
                UsuarioDTO dto = new UsuarioDTO();
                dto.setId(usuario.getId());
                dto.setUsername(usuario.getUsername());
                dto.setEmail(usuario.getEmail());
                dto.setNombreCompleto(usuario.getNombreCompleto());
                dto.setTelefono(usuario.getTelefono());
                return ResponseEntity.ok(dto);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUsuario(@PathVariable Long id, @RequestBody UsuarioDTO usuarioDetails) {
        Optional<Usuario> usuarioOptional = usuarioRepository.findById(id);
        if (!usuarioOptional.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        Usuario usuario = usuarioOptional.get();
        usuario.setEmail(usuarioDetails.getEmail());
        usuario.setNombreCompleto(usuarioDetails.getNombreCompleto());
        usuario.setTelefono(usuarioDetails.getTelefono());
        
        Usuario updatedUsuario = usuarioRepository.save(usuario);
        
        // Retornar el DTO actualizado
        UsuarioDTO responseDto = new UsuarioDTO();
        responseDto.setId(updatedUsuario.getId());
        responseDto.setUsername(updatedUsuario.getUsername());
        responseDto.setEmail(updatedUsuario.getEmail());
        responseDto.setNombreCompleto(updatedUsuario.getNombreCompleto());
        responseDto.setTelefono(updatedUsuario.getTelefono());
        
        return ResponseEntity.ok(responseDto);
    }
    
    @PostMapping("/cambiar-password")
    public ResponseEntity<Map<String, String>> cambiarPassword(@Valid @RequestBody CambiarPasswordDTO request) {
        Map<String, String> response = new HashMap<>();
        
        Optional<Usuario> usuarioOptional = usuarioRepository.findById(request.getId());
        if (!usuarioOptional.isPresent()) {
            response.put("message", "Usuario no encontrado");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        
        Usuario usuario = usuarioOptional.get();
        
        if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPassword())) {
            response.put("message", "La contraseña actual no es correcta");
            return ResponseEntity.badRequest().body(response);
        }
        
        usuario.setPassword(passwordEncoder.encode(request.getNuevaPassword()));
        usuarioRepository.save(usuario);
        
        response.put("message", "Contraseña actualizada exitosamente");
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(response);
    }
}
