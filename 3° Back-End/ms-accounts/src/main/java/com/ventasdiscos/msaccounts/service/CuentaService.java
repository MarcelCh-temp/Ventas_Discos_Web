package com.ventasdiscos.msaccounts.service;

import com.ventasdiscos.msaccounts.dto.ActualizarPerfilRequest;
import com.ventasdiscos.msaccounts.dto.CambioPasswordRequest;
import com.ventasdiscos.msaccounts.dto.EstadisticasCuentasResponse;
import com.ventasdiscos.msaccounts.dto.EstadoOperacionResponse;
import com.ventasdiscos.msaccounts.dto.LoginRequest;
import com.ventasdiscos.msaccounts.dto.LoginResponse;
import com.ventasdiscos.msaccounts.dto.PerfilResponse;
import com.ventasdiscos.msaccounts.dto.RecuperarCorreoRequest;
import com.ventasdiscos.msaccounts.dto.RecuperarPasswordRequest;
import com.ventasdiscos.msaccounts.dto.RegistroRequest;
import com.ventasdiscos.msaccounts.exception.AccesoDenegadoException;
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
import com.ventasdiscos.msaccounts.dto.CuentaAdminResponse;

import java.util.Locale;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reglas de negocio de EP-01 (HU-01..07). Aquí van las validaciones que necesitan
 * comparar datos o consultar la BD; las de formato quedan en los DTO (@Valid).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CuentaService {

    // Dependencia inyectada para acceder a usuario repository sin crearla manualmente.
    private final UsuarioRepository usuarioRepository;
    // Atributo passwordEncoder: dato o dependencia utilizada por esta clase.
    private final PasswordEncoder passwordEncoder;
    // Dependencia inyectada para acceder a jwt service sin crearla manualmente.
    private final JwtService jwtService;

    /** HU-01: registro público. Siempre crea CLIENTE (RN-01). */
    @Transactional
    // Procesa registrar aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
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
        // Lote 5 (RF-CTA-02, RN-44): solo una cuenta ACTIVA inicia sesión. Se revisa DESPUÉS de la contraseña para
        // no revelar el estado de una cuenta a quien no conoce sus credenciales.
        exigirOperativa(usuario);
        String token = jwtService.generar(usuario);
        return new LoginResponse(token, "Bearer", jwtService.getExpiracionSegundos(),
                usuario.getId(), usuario.getNombres(), usuario.getRol());
    }

    /** HU-04: solo la propia cuenta (el id viene del token). Lote 5: solo si la cuenta está ACTIVA (RN-44). */
    @Transactional(readOnly = true)
    // Procesa obtener perfil aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public PerfilResponse obtenerPerfil(Long userId) {
        return PerfilResponse.from(buscarOperativa(userId));
    }

    /** HU-05: modifica solo campos permitidos; el correo nuevo no puede ser de otra cuenta. */
    @Transactional
    // Procesa actualizar perfil aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public PerfilResponse actualizarPerfil(Long userId, ActualizarPerfilRequest req) {
        if (req.nombres() == null && req.apellidos() == null && req.correo() == null) {
            throw new ValidacionNegocioException("Debes indicar al menos un dato a modificar");
        }
        Usuario usuario = buscarOperativa(userId);
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
    // Procesa cambiar password aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public void cambiarPassword(Long userId, CambioPasswordRequest req) {
        Usuario usuario = buscarOperativa(userId);
        if (!passwordEncoder.matches(req.passwordActual(), usuario.getPasswordHash())) {
            throw new ValidacionNegocioException("La contraseña actual es incorrecta");
        }
        validarConfirmacionPassword(req.nuevaPassword(), req.confirmacionPassword());
        usuario.setPasswordHash(passwordEncoder.encode(req.nuevaPassword()));
        usuarioRepository.save(usuario);
    }

    /** HU-07 (contraseña): identifica la cuenta con correo + RUT (simulado, sin correo real). */
    @Transactional
    // Procesa recuperar password aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public void recuperarPassword(RecuperarPasswordRequest req) {
        String rut = validarRut(req.rut());
        Usuario usuario = usuarioRepository.findByCorreo(normalizarCorreo(req.correo()))
                .filter(u -> u.getRut().equals(rut))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una cuenta registrada con ese correo y RUT"));
        exigirOperativa(usuario); // Lote 5 (RF-CTA-07): no se recupera una cuenta EN_ELIMINACION o ELIMINADA
        validarConfirmacionPassword(req.nuevaPassword(), req.confirmacionPassword());
        usuario.setPasswordHash(passwordEncoder.encode(req.nuevaPassword()));
        usuarioRepository.save(usuario);
    }

    /** HU-07 (correo): identifica por RUT, valida confirmación y que el correo no esté en uso. */
    @Transactional
    // Procesa recuperar correo aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public void recuperarCorreo(RecuperarCorreoRequest req) {
        String rut = validarRut(req.rut());
        Usuario usuario = usuarioRepository.findByRut(rut)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una cuenta registrada con ese RUT"));
        exigirOperativa(usuario); // Lote 5 (RF-CTA-07)
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

    /**
     * Lote 5 · Contrato INTERNO (ERS 13.8, RN-37, RN-44): estado de la cuenta para que Orders, Support, PostSale y
     * Catalog decidan si permiten una nueva operación. 404 si la cuenta no existe.
     */
    @Transactional(readOnly = true)
    public EstadoOperacionResponse estadoOperacion(Long userId) {
        return EstadoOperacionResponse.from(buscar(userId));
    }

    /**
     * HU-45: cantidad de usuarios para el Dashboard (solo lectura). Lote 5: el Administrador debe estar ACTIVO y las
     * cuentas ELIMINADA (anonimizadas, se conserva su fila) ya no se cuentan como usuarios.
     */
    @Transactional(readOnly = true)
    public EstadisticasCuentasResponse estadisticas(Long adminId) {
        buscarOperativa(adminId);
        return new EstadisticasCuentasResponse(usuarioRepository.contarNoEliminadas(),
                usuarioRepository.contarNoEliminadasPorRol(Role.CLIENTE));
    }

    /** Cierre (RF-PED-01/02, RF-SOP-05): máximo de ids por consulta agrupada. */
    // Constante MAX_IDS_ADMIN: valor compartido que no cambia durante la ejecución.
    static final int MAX_IDS_ADMIN = 100;

    /**
     * Cierre del backend (decisión 3/4): datos mínimos de varios clientes para las vistas administrativas.
     * El administrador debe estar ACTIVO. ids: entre 1 y 100, todos positivos (si no → 400 con errores.ids). Los
     * repetidos se consultan una vez y los inexistentes simplemente no aparecen. Se respeta el orden pedido.
     * No escribe en los logs datos personales.
     */
    @Transactional(readOnly = true)
    public List<CuentaAdminResponse> cuentasParaAdmin(Long adminId, List<Long> ids) {
        buscarOperativa(adminId);
        if (ids == null || ids.isEmpty()) {
            throw new ValidacionNegocioException("Indica al menos un id de cliente",
                    Map.of("ids", "Debe contener entre 1 y " + MAX_IDS_ADMIN + " ids"));
        }
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new ValidacionNegocioException("Hay ids inválidos",
                    Map.of("ids", "Cada id debe ser un número entero positivo"));
        }
        List<Long> distintos = ids.stream().distinct().toList();
        if (distintos.size() > MAX_IDS_ADMIN) {
            throw new ValidacionNegocioException("Se pueden consultar como máximo " + MAX_IDS_ADMIN + " clientes por vez",
                    Map.of("ids", "Debe contener entre 1 y " + MAX_IDS_ADMIN + " ids"));
        }
        Map<Long, Usuario> encontrados = new HashMap<>();
        for (Usuario u : usuarioRepository.findAllById(distintos)) {
            encontrados.put(u.getId(), u);
        }
        return distintos.stream().filter(encontrados::containsKey)
                .map(id -> CuentaAdminResponse.from(encontrados.get(id))).toList();
    }

    // ---------- helpers ----------

    private Usuario buscar(Long userId) {
        return usuarioRepository.findById(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La cuenta no existe"));
    }

    /** Lote 5 (RN-44): un token todavía vigente no basta; la cuenta debe estar ACTIVA. */
    // Procesa buscar operativa aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    private Usuario buscarOperativa(Long userId) {
        Usuario usuario = buscar(userId);
        exigirOperativa(usuario);
        return usuario;
    }

    /** 403 con un mensaje según el estado. Un estado desconocido se trata como NO habilitado (restrictivo). */
    // Procesa exigir operativa aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    static void exigirOperativa(Usuario usuario) {
        if (usuario.puedeOperar()) {
            return;
        }
        if (usuario.estaEnEliminacion()) {
            throw new AccesoDenegadoException("Tu cuenta está en proceso de eliminación: ya no puede iniciar sesión"
                    + " ni realizar nuevas operaciones");
        }
        if (usuario.estaEliminada()) {
            throw new AccesoDenegadoException("La cuenta fue eliminada");
        }
        throw new AccesoDenegadoException("La cuenta no está habilitada para operar");
    }

    // Procesa validar rut aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    private String validarRut(String rut) {
        if (!RutValidator.esValido(rut)) {
            throw new ValidacionNegocioException("El RUT no es válido o su dígito verificador es incorrecto");
        }
        return RutValidator.normalizar(rut);
    }

    // Regla de negocio en Service; aquí se coordinan verificaciones, repositorios y otras operaciones. Método validarConfirmacionPassword (validar confirmacion password).
    private void validarConfirmacionPassword(String password, String confirmacion) {
        if (!password.equals(confirmacion)) {
            throw new ValidacionNegocioException("La contraseña y su confirmación no coinciden");
        }
    }

    // Regla de negocio en Service; aquí se coordinan verificaciones, repositorios y otras operaciones. Método normalizarCorreo (normalizar correo).
    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }
}
