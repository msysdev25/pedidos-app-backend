package com.backend.pedidos_app.dto;

public class CambiarPasswordDTO {
    private Long id;
    private String passwordActual;
    private String nuevaPassword;
    
    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPasswordActual() { return passwordActual; }
    public void setPasswordActual(String passwordActual) { this.passwordActual = passwordActual; }
    public String getNuevaPassword() { return nuevaPassword; }
    public void setNuevaPassword(String nuevaPassword) { this.nuevaPassword = nuevaPassword; }
}