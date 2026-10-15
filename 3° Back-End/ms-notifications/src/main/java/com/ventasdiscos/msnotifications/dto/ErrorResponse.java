package com.ventasdiscos.msnotifications.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private String codigo;
    private String mensaje;
    private String campo;
    private LocalDateTime timestamp;
}