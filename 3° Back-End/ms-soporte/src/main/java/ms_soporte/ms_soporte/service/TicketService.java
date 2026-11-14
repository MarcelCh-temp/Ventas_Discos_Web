package ms_soporte.ms_soporte.service;

import ms_soporte.ms_soporte.client.AccountsClient;
import ms_soporte.ms_soporte.client.NotificacionesClient;
import ms_soporte.ms_soporte.client.OrdersClient;
import ms_soporte.ms_soporte.dto.CrearTicketRequest;
import ms_soporte.ms_soporte.dto.ExisteResponse;
import ms_soporte.ms_soporte.dto.PageResponse;
import ms_soporte.ms_soporte.dto.TicketDetalleResponse;
import ms_soporte.ms_soporte.dto.TicketResumenResponse;
import ms_soporte.ms_soporte.exception.RecursoNoEncontradoException;
import ms_soporte.ms_soporte.model.EstadoTicket;
import ms_soporte.ms_soporte.model.MensajeSoporte;
import ms_soporte.ms_soporte.model.RolAutor;
import ms_soporte.ms_soporte.model.Ticket;
import ms_soporte.ms_soporte.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Casos de uso de Soporte (HU-59 a HU-67). Aquí están TODAS las reglas de negocio:
 * dueño de la solicitud, estados permitidos, cierre y avisos. El Controller solo recibe y responde.
 *
 * Errores:
 *  - RecursoNoEncontradoException -> 404 (también si la solicitud es de otro cliente: no se revela que existe)
 *  - IllegalStateException       -> 409 (el estado actual no permite la acción)
 *  - IllegalArgumentException    -> 400 (filtros incoherentes)
 */
@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);
    // Constante TAMANO_MAXIMO_PAGINA: valor compartido que no cambia durante la ejecución.
    private static final int TAMANO_MAXIMO_PAGINA = 100;

    @Autowired
    // Dependencia inyectada para acceder a ticket repository sin crearla manualmente.
    private TicketRepository ticketRepository;

    @Autowired
    // Dependencia inyectada para acceder a notificaciones client sin crearla manualmente.
    private NotificacionesClient notificacionesClient;

    // Lote 5 (integración de HU-78): confirma con Accounts que la cuenta está ACTIVA antes de crear un ticket
    @Autowired
    private AccountsClient accountsClient;

    // Cierre (RF-SOP-01 / HU-59): confirma con Orders que el pedido asociado existe y es del Cliente del token
    @Autowired
    private OrdersClient ordersClient;

    // ---------------------------------------------------------------- Cliente

    /** HU-59: crea la solicitud con estado inicial ABIERTA, primer mensaje y número único. */
    @Transactional
    // Procesa crear ticket aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public TicketDetalleResponse crearTicket(CrearTicketRequest datos, Long idUsuario) {
        Ticket ticket = new Ticket();
        ticket.setIdUsuario(idUsuario);
        // Cierre (RF-SOP-01 / HU-59): el pedido es opcional; si viene, Orders confirma que existe y es de este cliente
        // (userId del JWT). No existe o es ajeno → 404; Orders no responde → 503. En ambos casos no se guarda nada.
        if (datos.orderId() != null) {
            ordersClient.exigirPedidoPropio(datos.orderId(), idUsuario);
        }
        ticket.setOrderId(datos.orderId());
        ticket.setAsunto(datos.asunto().trim());
        ticket.setCategoria(datos.categoria().trim().toUpperCase());
        // "descripcion" ya existía en la tabla (NOT NULL): guarda el mensaje inicial.
        ticket.setDescripcion(datos.mensaje().trim());
        ticket.setFechaCreacion(LocalDateTime.now());
        ticket.registrarEstadoInicial(idUsuario); // responsable: el Cliente del token (RNF-09)
        // UML: la solicitud contiene 1..* mensajes; el primero es el que escribe el cliente al crearla.
        ticket.agregarMensaje(new MensajeSoporte(datos.mensaje().trim(), RolAutor.CLIENTE, idUsuario));

        // Lote 5 (HU-78, RN-37, RN-44, T-06): justo antes de guardar, Accounts confirma que la cuenta del token está
        // ACTIVA. Si no (403) o no responde (503), se lanza la excepción y no se guarda nada.
        accountsClient.exigirCuentaActiva(idUsuario);
        Ticket guardado = ticketRepository.save(ticket);
        guardado.asignarNumeroTicket(); // SOP-000012: necesita el id, por eso va después del primer guardado

        notificarTrasCommit("SOPORTE-" + guardado.getId() + "-CREADO", idUsuario, "SOPORTE_CREADO",
                "Solicitud de soporte creada",
                "Tu solicitud " + guardado.getNumeroTicket() + " fue registrada.", guardado.getId());
        return TicketDetalleResponse.desde(guardado);
    }

    /** Lote 5 · Contrato interno para Accounts (HU-78, RN-36): tickets ABIERTA o EN_PROCESO. Solo lectura. */
    @Transactional(readOnly = true)
    // Procesa tiene tickets abiertos aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public ExisteResponse tieneTicketsAbiertos(Long idUsuario) {
        return new ExisteResponse(idUsuario,
                ticketRepository.existsByIdUsuarioAndEstadoIn(idUsuario, List.of(EstadoTicket.ABIERTA, EstadoTicket.EN_PROCESO)));
    }

    /** HU-60: solo las solicitudes del usuario autenticado. */
    @Transactional(readOnly = true)
    // Procesa listar propios aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public List<TicketResumenResponse> listarPropios(Long idUsuario) {
        return ticketRepository.findByIdUsuarioOrderByFechaCreacionDesc(idUsuario).stream()
                .map(TicketResumenResponse::desde)
                .toList();
    }

    /** HU-61: detalle de una solicitud propia. */
    @Transactional(readOnly = true)
    // Procesa obtener propio aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public TicketDetalleResponse obtenerPropio(Long id, Long idUsuario) {
        return TicketDetalleResponse.desde(buscarPropio(id, idUsuario));
    }

    /** HU-62: el cliente responde en su solicitud, solo si no está cerrada (RN-30). */
    @Transactional
    // Procesa responder como cliente aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public TicketDetalleResponse responderComoCliente(Long id, Long idUsuario, String contenido) {
        Ticket ticket = buscarPropio(id, idUsuario);
        validarQueAceptaRespuestas(ticket);
        ticket.agregarMensaje(new MensajeSoporte(contenido.trim(), RolAutor.CLIENTE, idUsuario));
        ticketRepository.flush(); // guarda el mensaje nuevo (cascade) y le asigna id
        return TicketDetalleResponse.desde(ticket);
    }

    // ---------------------------------------------------------------- Administrador

    /** HU-63, HU-64: listado paginado con filtros combinables (todos opcionales). */
    @Transactional(readOnly = true)
    public PageResponse<TicketResumenResponse> listarParaAdministrador(String texto, EstadoTicket estado,
                                                                       String categoria, LocalDate desde,
                                                                       LocalDate hasta, int pagina, int tamano) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        if (pagina < 0 || tamano < 1 || tamano > TAMANO_MAXIMO_PAGINA) {
            throw new IllegalArgumentException("Página inválida: 'pagina' >= 0 y 'tamano' entre 1 y " + TAMANO_MAXIMO_PAGINA);
        }

        Specification<Ticket> filtros = (root, query, cb) -> cb.conjunction();
        if (texto != null && !texto.isBlank()) {
            String patron = "%" + texto.trim().toLowerCase() + "%";
            Long idBuscado = comoNumero(texto.trim());
            // HU-64 "buscar por identificador": si escribe un número, también se busca ese id exacto
            filtros = filtros.and((root, query, cb) -> idBuscado == null
                    ? cb.or(cb.like(cb.lower(root.<String>get("numeroTicket")), patron),
                            cb.like(cb.lower(root.<String>get("asunto")), patron))
                    : cb.or(cb.equal(root.get("id"), idBuscado),
                            cb.like(cb.lower(root.<String>get("numeroTicket")), patron),
                            cb.like(cb.lower(root.<String>get("asunto")), patron)));
        }
        if (estado != null) {
            filtros = filtros.and((root, query, cb) -> cb.equal(root.get("estado"), estado));
        }
        if (categoria != null && !categoria.isBlank()) {
            String cat = categoria.trim().toUpperCase();
            filtros = filtros.and((root, query, cb) -> cb.equal(root.get("categoria"), cat));
        }
        if (desde != null) {
            filtros = filtros.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.<LocalDateTime>get("fechaCreacion"), desde.atStartOfDay()));
        }
        if (hasta != null) {
            filtros = filtros.and((root, query, cb) ->
                    cb.lessThan(root.<LocalDateTime>get("fechaCreacion"), hasta.plusDays(1).atStartOfDay()));
        }

        PageRequest orden = PageRequest.of(pagina, tamano, Sort.by(Sort.Direction.DESC, "fechaCreacion"));
        return PageResponse.of(ticketRepository.findAll(filtros, orden), TicketResumenResponse::desde);
    }

    @Transactional(readOnly = true)
    // Regla de negocio en Service; aquí se coordinan verificaciones, repositorios y otras operaciones. Método obtenerParaAdministrador (obtener para administrador). @Transactional controla el alcance de la transacción de base de datos.
    public TicketDetalleResponse obtenerParaAdministrador(Long id) {
        return TicketDetalleResponse.desde(obtenerTicketPorId(id));
    }

    /** HU-65: respuesta del administrador; solo en solicitudes ABIERTA o EN_PROCESO. */
    @Transactional
    // Procesa responder como administrador aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    public TicketDetalleResponse responderComoAdministrador(Long id, Long idAdministrador, String contenido) {
        Ticket ticket = obtenerTicketPorId(id);
        validarQueAceptaRespuestas(ticket);
        MensajeSoporte mensaje = new MensajeSoporte(contenido.trim(), RolAutor.ADMINISTRADOR, idAdministrador);
        ticket.agregarMensaje(mensaje);
        ticketRepository.flush(); // guarda el mensaje (cascade) y le asigna id: eventoId único por respuesta

        notificarTrasCommit("SOPORTE-" + ticket.getId() + "-MSG-" + mensaje.getId(), ticket.getIdUsuario(),
                "SOPORTE_RESPONDIDO", "Nueva respuesta de soporte",
                "Tu solicitud " + referencia(ticket) + " tiene una nueva respuesta.", ticket.getId());
        return TicketDetalleResponse.desde(ticket);
    }

    /**
     * HU-66: cambios de estado INTERMEDIOS. Única transición permitida: ABIERTA -> EN_PROCESO.
     * El paso a CERRADA se hace solo con cerrar() (HU-67), como pide el requisito.
     * DOC-02 (aprobado): este cambio queda en el historial y visible para el Cliente, sin aviso adicional.
     * Lote 1.1 (DOC-01): adminId = Administrador autenticado (lo entrega el Controller desde el token).
     */
    @Transactional
    public TicketDetalleResponse actualizarEstado(Long id, EstadoTicket nuevoEstado, Long adminId) {
        Ticket ticket = obtenerTicketPorId(id);
        if (nuevoEstado == EstadoTicket.CERRADA) {
            throw new IllegalStateException("Para cerrar una solicitud usa la acción de cierre (/close)");
        }
        if (nuevoEstado != EstadoTicket.EN_PROCESO) {
            throw new IllegalStateException("Transición no permitida de " + ticket.getEstado() + " a " + nuevoEstado);
        }
        ticket.iniciarAtencion(adminId); // valida que esté ABIERTA y registra el historial con su responsable
        return TicketDetalleResponse.desde(ticket);
    }

    /**
     * HU-67: cierra la solicitud (fecha de cierre + historial + aviso). Si ya está cerrada -> 409.
     * Lote 1.1 (DOC-01): adminId = Administrador autenticado, queda como responsable del cierre (RNF-09).
     */
    @Transactional
    public TicketDetalleResponse cerrar(Long id, Long adminId) {
        Ticket ticket = obtenerTicketPorId(id);
        ticket.cerrar(adminId);
        notificarTrasCommit("SOPORTE-" + ticket.getId() + "-CERRADO", ticket.getIdUsuario(),
                "SOPORTE_CERRADO", "Solicitud de soporte cerrada",
                "Tu solicitud " + referencia(ticket) + " fue cerrada.", ticket.getId());
        return TicketDetalleResponse.desde(ticket);
    }

    // ---------------------------------------------------------------- Apoyo

    public Ticket obtenerTicketPorId(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ticket no encontrado con ID: " + id));
    }

    /** Una solicitud de otro cliente se responde igual que una inexistente (404). */
    // Procesa buscar propio aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    private Ticket buscarPropio(Long id, Long idUsuario) {
        Ticket ticket = obtenerTicketPorId(id);
        if (!ticket.perteneceA(idUsuario)) {
            throw new RecursoNoEncontradoException("Ticket no encontrado con ID: " + id);
        }
        return ticket;
    }

    /**
     * Lote 1.1 (observación A): el aviso se envía DESPUÉS de que la base de datos confirma el guardado.
     * Antes se enviaba dentro de la transacción: si el guardado fallaba al final, el cliente recibía
     * un aviso de algo que no quedó registrado. Es el mismo patrón que ya usa ms-postsale
     * (PostSaleSupport.notificarTrasCommit). Si Notifications falla, la solicitud ya está guardada (D14).
     * Sin transacción activa (por ejemplo, en un test unitario) se envía de inmediato.
     */
    private void notificarTrasCommit(String eventoId, Long usuarioId, String tipo,
                                     String titulo, String mensaje, Long ticketId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            enviarAviso(eventoId, usuarioId, tipo, titulo, mensaje, ticketId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                enviarAviso(eventoId, usuarioId, tipo, titulo, mensaje, ticketId);
            }
        });
    }

    private void enviarAviso(String eventoId, Long usuarioId, String tipo,
                             String titulo, String mensaje, Long ticketId) {
        try {
            notificacionesClient.notificar(eventoId, usuarioId, tipo, titulo, mensaje, ticketId);
        } catch (RuntimeException e) {
            // El ticket ya quedó guardado: un error al avisar no debe convertirse en un error para el usuario.
            log.warn("No se pudo enviar el aviso {} a MS-Notifications: {}", eventoId, e.getMessage());
        }
    }

    /** Tickets anteriores al Lote 1 no tienen numeroTicket: en el aviso se muestra su id (#12). */
    // Procesa referencia aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    private static String referencia(Ticket ticket) {
        return ticket.getNumeroTicket() != null ? ticket.getNumeroTicket() : "#" + ticket.getId();
    }

    // Procesa como numero aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    private static Long comoNumero(String texto) {
        try {
            return Long.valueOf(texto);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** RN-30: una solicitud CERRADA no acepta nuevas respuestas (no hay regla de reapertura aprobada). */
    // Procesa validar que acepta respuestas aplicando las reglas del negocio; consultar validaciones y excepciones dentro del método.
    private void validarQueAceptaRespuestas(Ticket ticket) {
        if (ticket.estaCerrada()) {
            throw new IllegalStateException("La solicitud está cerrada y no acepta nuevas respuestas");
        }
    }
}
