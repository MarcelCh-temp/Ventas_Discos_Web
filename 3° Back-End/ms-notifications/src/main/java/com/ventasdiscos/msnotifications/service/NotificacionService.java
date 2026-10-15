
package com.ventasdiscos.msnotifications.service;

import com.ventasdiscos.msnotifications.exception.NotificacionNoEncontradaException;
import com.ventasdiscos.msnotifications.model.Notificacion;
import com.ventasdiscos.msnotifications.model.TipoNotificacion;
import com.ventasdiscos.msnotifications.repository.NotificacionRepository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificacionService {

    private final NotificacionRepository repository;

    public NotificacionService(NotificacionRepository repository) {
        this.repository = repository;
    }

    // Consultar notificaciones de un usuario.
    @Transactional(readOnly = true)
    public List<Notificacion> listarPorUsuario(Long usuarioId) {
        return buscarNotificaciones(usuarioId, null, null, null);
    }

    // Contar notificaciones no leidas.
    @Transactional(readOnly = true)
    public long contarNoLeidas(Long usuarioId) {
        validarIdentificador(usuarioId);
        return repository.countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    // Marcar una notificacion como leida.
    @Transactional
    public void marcarComoLeida(Long notificacionId, Long usuarioId) {

        validarIdentificador(notificacionId);
        validarIdentificador(usuarioId);

        Notificacion notificacion =
                repository.findByIdAndUsuarioId(
                        notificacionId, usuarioId
                ).orElseThrow(NotificacionNoEncontradaException::new);

        if (!notificacion.estaLeida()) {
            notificacion.marcarComoLeida();
        }
    }

    // Marcar todas las notificaciones no leidas.
    @Transactional
    public int marcarTodasComoLeidas(Long usuarioId) {
        validarIdentificador(usuarioId);
        return repository.marcarTodasComoLeidas(usuarioId);
    }

    // Buscar por usuario, tipo y periodo.
    @Transactional(readOnly = true)
    public List<Notificacion> buscarNotificaciones(
            Long usuarioId,
            TipoNotificacion tipo,
            LocalDateTime desde,
            LocalDateTime hasta) {

        validarIdentificador(usuarioId);

        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede superar la fecha final"
            );
        }

        Specification<Notificacion> filtros =
                (root, query, cb) ->
                        cb.equal(root.get("usuarioId"), usuarioId);

        if (tipo != null) {
            filtros = filtros.and(
                    (root, query, cb) ->
                            cb.equal(root.get("tipo"), tipo)
            );
        }

        if (desde != null) {
            filtros = filtros.and(
                    (root, query, cb) ->
                            cb.greaterThanOrEqualTo(
                                    root.<LocalDateTime>get("fechaCreacion"),
                                    desde
                            )
            );
        }

        if (hasta != null) {
            filtros = filtros.and(
                    (root, query, cb) ->
                            cb.lessThanOrEqualTo(
                                    root.<LocalDateTime>get("fechaCreacion"),
                                    hasta
                            )
            );
        }

        return repository.findAll(
                filtros,
                Sort.by(Sort.Direction.DESC, "fechaCreacion")
        );
    }

    // Filtrar solo por tipo.
    @Transactional(readOnly = true)
    public List<Notificacion> filtrarPorTipo(
            Long usuarioId,
            TipoNotificacion tipo) {

        if (tipo == null) {
            throw new IllegalArgumentException(
                    "El tipo es obligatorio"
            );
        }

        return buscarNotificaciones(usuarioId, tipo, null, null);
    }

    // Filtrar solo por periodo.
    @Transactional(readOnly = true)
    public List<Notificacion> filtrarPorPeriodo(
            Long usuarioId,
            LocalDateTime desde,
            LocalDateTime hasta) {

        if (desde == null || hasta == null) {
            throw new IllegalArgumentException(
                    "Debe indicar ambas fechas"
            );
        }

        return buscarNotificaciones(usuarioId, null, desde, hasta);
    }

    private void validarIdentificador(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "El identificador debe ser positivo"
            );
        }
    }
}
