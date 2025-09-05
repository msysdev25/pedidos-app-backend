package com.backend.pedidos_app.security;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    private final SecretKey jwtSecret;
    private final int jwtExpirationMs;

    public JwtUtils(@Value("${app.jwt.expiration-ms}") int jwtExpirationMs) {
        this.jwtSecret = Keys.secretKeyFor(SignatureAlgorithm.HS512); // Genera clave segura automática
        this.jwtExpirationMs = jwtExpirationMs;
        logger.info("Generated secure JWT key for HS512 algorithm");
    }

    public String generateJwtToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        
        // Obtener los roles como lista de strings
        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .claim("id", userPrincipal.getId())
                .claim("email", userPrincipal.getEmail())
                .claim("roles", roles) // Añadir los roles al token
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(jwtSecret, SignatureAlgorithm.HS512)
                .compact();
    }
    
    // Método para obtener los roles del token
    public List<String> getRolesFromJwtToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(jwtSecret)
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        return claims.get("roles", List.class);
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(jwtSecret)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(jwtSecret)
                .build()
                .parseClaimsJws(authToken);
            return true;
        } catch (JwtException e) {
            logger.error("JWT error: {}", e.getMessage());
        }
        return false;
    }
    
    
}