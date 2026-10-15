package com.ventasdiscos.msaccounts.service;

import com.ventasdiscos.msaccounts.dto.ActualizarPerfilRequest;
import com.ventasdiscos.msaccounts.dto.CambioPasswordRequest;
import com.ventasdiscos.msaccounts.dto.EstadisticasCuentasResponse;
import com.ventasdiscos.msaccounts.dto.LoginRequest;
import com.ventasdiscos.msaccounts.dto.LoginResponse;
import com.ventasdiscos.msaccounts.dto.PerfilResponse;
import com.ventasdiscos.msaccounts.dto.RecuperarCorreoRequest;
import com.ventasdiscos.msaccounts.dto.RecuperarPasswordRequest;
import com.ventasdiscos.msaccounts.dto.RegistroRequest;
import com.ventasdiscos.msaccounts.exception.NoAutenticadoException;
import com.ventasdiscos.msaccounts.exception.RecursoDuplicadoException;
import com.ventasdiscos.msaccounts.exception.RecursoNoEncontradoException;
import com.ventasdiscos.msaccounts.exception.ValidacionNegocioException;
import com.ventasdiscos.msaccounts.model.Role;
import com.ventasdiscos.msaccounts.model.Usuario;
import com.ventasdiscos.msaccounts.repository.UsuarioRepository;
import com.ventasdiscos.msaccounts.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Reglas de negocio de EP-01 (HU-01..07). Aquí van las validaciones que necesitan
 * comparar datos o consultar la BD; las de formato quedan en los DTO (@Valid).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CuentaService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /** HU-01: registro público. Siempre crea CLIENTE (RN-01). */
    @Transactional
    public PerfilResponse registrar(RegistroRequest req) {
        String rut = validarRut(req.rut());
        String correo = normalizarCorreo(req.correo());
        if (!correo.equals(normalizarCorreo(req.confirmacionCorreo()))) {
            throw new ValidacionNegocioException("El correo y su confirmación no coinciden");
        }
        validarConfirmacionPassword(req.password(), req.confirmacionPassword());
        if (usuarioRepository.existsByRut(rut)) {
            throw new RecursoDuplicadoException("Ya existe una cuenta registrada con ese RUT");
        }
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new RecursoDuplicadoException("Ya existe una cuenta registrada con ese correo");
        }

        Usuario usuario = new Usuario();
        usuario.setRut(rut);
        usuario.setNombres(req.nombres().trim());
        usuario.setApellidos(req.apellidos().trim());
        usuario.setCorreo(correo);
        usuario.setPasswordHash(passwordEncoder.encode(req.password()));
        usuario.setRol(Role.CLIENTE);
        Usuario guardado = usuarioRepository.save(usuario);
        log.info("Cuenta Cliente registrada userId={}", guardado.getId());
        return PerfilResponse.from(guardado);
    }

    /**
     * HU-02: valida que la cuenta exista y la contraseña coincida.
     * Mismo mensaje para "no existe" y "contraseña mala": no revela qué correos están registrados.
     * Sin contador de intentos ni bloqueo (RNF-05).
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        Usuario usuario = usuarioRepository.findByCorreo(normalizarCorreo(req.correo()))
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new NoAutenticadoException("Correo o contraseña incorrectos"));
        String token = jwtService.generar(usuario);
        return new LoginResponse(token, "Bearer", jwtService.getExpiracionSegundos(),
                usuario.getId(), usuario.getNombres(), usuario.getRol());
    }

    /** HU-04: solo la propia cuenta (el id viene del token). */
    @Transactional(readOnly = true)
    public PerfilResponse obtenerPerfil(Long userId) {
        return PerfilResponse.from(buscar(userId));
    }

    /** HU-05: modifica solo campos permitidos; el correo nuevo no puede ser de otra cuenta. */
    @Transactional
    public PerfilResponse actualizarPerfil(Long userId, ActualizarPerfilRequest req) {
        if (req.nombres() == null && req.apellidos() == null && req.correo() == null) {
            throw new ValidacionNegocioException("Debes indicar al menos un dato a modificar");
        }
        Usuario usuario = buscar(userId);
        if (req.correo() != null) {
            String correo = normalizarCorreo(req.correo());
            if (usuarioRepository.existsByCorreoAndIdNot(correo, userId)) {
                throw new RecursoDuplicadoException("Ese correo ya está asociado a otra cuenta");
            }
            usuario.setCorreo(correo);
        }
        if (req.nombres() != null) {
            usuario.setNombres(req.nombres().trim());
        }
        if (req.apellidos() != null) {
            usuario.setApellidos(req.apellidos().trim());
        }
        return PerfilResponse.from(usuarioRepository.save(usuario));
    }

    /** HU-06: la contraseña actual debe ser correcta; si algo falla no se cambia nada. */
    @Transactional
    public void cambiarPassword(Long userId, CambioPasswordRequest req) {
        Usuario usuario = buscar(userId);
        if (!passwordEncoder.matches(req.passwordActual(), usuario.getPasswordHash())) {
            throw new ValidacionNegocioException("La contraseña actual es incorrecta");
        }
        validarConfirmacionPassword(req.nuevaPassword(), req.confirmacionPassword());
        usuario.setPasswordHash(passwordEncoder.encode(req.nuevaPassword()));
        usuarioRepository.save(usuario);
    }

    /** HU-07 (contraseña): identifica la cuenta con correo + RUT (simulado, sin correo real). */
    @Transactional
    public void recuperarPassword(RecuperarPasswordRequest req) {
        String rut = validarRut(req.rut());
        Usuario usuario = usuarioRepository.findByCorreo(normalizarCorreo(req.correo()))
                .filter(u -> u.getRut().equals(rut))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una cuenta registrada con ese correo y RUT"));
        validarConfirmacionPassword(req.nuevaPassword(), req.confirmacionPassword());
        usuario.setPasswordHash(passwordEncoder.encode(req.nuevaPassword()));
        usuarioRepository.save(usuario);
    }

    /** HU-07 (correo): identifica por RUT, valida confirmación y que el correo no esté en uso. */
    @Transactional
    public void recuperarCorreo(RecuperarCorreoRequest req) {
        String rut = validarRut(req.rut());
        Usuario usuario = usuarioRepository.findByRut(rut)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una cuenta registrada con ese RUT"));
        String correo = normalizarCorreo(req.nuevoCorreo());
        if (!correo.equals(normalizarCorreo(req.confirmacionCorreo()))) {
            throw new ValidacionNegocioException("El correo y su confirmación no coinciden");
        }
        if (usuarioRepository.existsByCorreoAndIdNot(correo, usuario.getId())) {
            throw new RecursoDuplicadoException("Ese correo ya está asociado a otra cuenta");
        }
        usuario.setCorreo(correo);
        usuarioRepository.save(usuario);
    }

    /** HU-45: cantidad de usuarios para el Dashboard (solo lectura). */
    @Transactional(readOnly = true)
    public EstadisticasCuentasResponse estadisticas() {
        return new EstadisticasCuentasResponse(usuarioRepository.count(), usuarioRepository.countByRol(Role.CLIENTE));
    }

    // ---------- helpers ----------

    private Usuario buscar(Long userId) {
        return usuarioRepository.findById(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La cuenta no existe"));
    }

    private String validarRut(String rut) {
        if (!RutValidator.esValido(rut)) {
            throw new ValidacionNegocioException("El RUT no es válido o su dígito verificador es incorrecto");
        }
        return RutValidator.normalizar(rut);
    }

    private void validarConfirmacionPassword(String password, String confirmacion) {
        if (!password.equals(confirmacion)) {
            throw new ValidacionNegocioException("La contraseña y su confirmación no coinciden");
        }
    }

    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }
}
