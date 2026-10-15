package com.ventasdiscos.msaccounts.config;

import com.ventasdiscos.msaccounts.model.Role;
import com.ventasdiscos.msaccounts.model.Usuario;
import com.ventasdiscos.msaccounts.repository.UsuarioRepository;
import com.ventasdiscos.msaccounts.service.RutValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * El registro público solo crea Clientes (RN-01), así que el Administrador inicial se
 * crea al arrancar desde variables de entorno (ADMIN_RUT, ADMIN_EMAIL, ADMIN_PASSWORD).
 * Si no están definidas, no hace nada. Las credenciales nunca van al repositorio y no hay clave
 * predefinida compartida (auditoría AC4): cada integrante define la suya en su .env (mín. 8 caracteres).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    /** Misma regla mínima que el registro y el cambio de contraseña. */
    static final int LARGO_MINIMO_PASSWORD = 8;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.rut:}")
    private String rut;
    @Value("${app.admin.correo:}")
    private String correo;
    @Value("${app.admin.password:}")
    private String password;

    @Override
    public void run(String... args) {
        if (rut.isBlank() || correo.isBlank() || password.isBlank()) {
            return;
        }
        String correoNormal = correo.trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByCorreo(correoNormal)) {
            return;
        }
        if (password.length() < LARGO_MINIMO_PASSWORD) {
            log.warn("ADMIN_PASSWORD debe tener al menos {} caracteres; no se creó el Administrador inicial",
                    LARGO_MINIMO_PASSWORD);
            return;
        }
        if (!RutValidator.esValido(rut)) {
            log.warn("ADMIN_RUT no es válido; no se creó el Administrador inicial");
            return;
        }
        Usuario admin = new Usuario();
        admin.setRut(RutValidator.normalizar(rut));
        admin.setNombres("Administrador");
        admin.setApellidos("Tienda");
        admin.setCorreo(correoNormal);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRol(Role.ADMINISTRADOR);
        usuarioRepository.save(admin);
        log.info("Administrador inicial creado ({})", correoNormal);
    }
}
