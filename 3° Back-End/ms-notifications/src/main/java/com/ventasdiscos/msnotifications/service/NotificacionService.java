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

    private final NotificacionRepository notificacionRepository;

    public NotificacionService(
            NotificacionRepository notificacionRepository
    ) {
        this.notificacionRepository = notificacionRepository;
    }

    // HU-54: Listar notificaciones del usuario
    @Transactional(readOnly = true)
    public List<Notificacion> listarPorUsuario(Long usuarioId) {

        validarIdentificador(usuarioId, "usuario");

        return notificacionRepository
                .findByUsuarioIdOrderByFechaCreacionDesc(usuarioId);
    }

    // HU-54: Contar no leidas
    @Transactional(readOnly = true)
    public long contarNoLeidas(Long usuarioId) {

        validarIdentificador(usuarioId, "usuario");

        return notificacionRepository
                .countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    // HU-54: Marcar una notificacion como leida
    @Transactional
    public void marcarComoLeida(
            Long notificacionId,
            Long usuarioId
    ) {

        validarIdentificador(notificacionId, "notificacion");
        validarIdentificador(usuarioId, "usuario");

        Notificacion notificacion = notificacionRepository
                .findByIdAndUsuarioId(notificacionId, usuarioId)
                .orElseThrow(() ->
                        new NotificacionNoEncontradaException(
                                "No se encontro la notificacion solicitada"
                        )
                );

        if (!notificacion.estaLeida()) {
            notificacion.marcarComoLeida();
        }
    }

    // HU-54: Marcar todas como leidas
    @Transactional
    public int marcarTodasComoLeidas(Long usuarioId) {

        validarIdentificador(usuarioId, "usuario");

        return notificacionRepository
                .marcarTodasComoLeidas(usuarioId);
    }

    // HU-55: Filtros combinados
    @Transactional(readOnly = true)
    public List<Notificacion> buscarNotificaciones(
            Long usuarioId,
            TipoNotificacion tipo,
            LocalDateTime desde,
            LocalDateTime hasta
    ) {

        validarIdentificador(usuarioId, "usuario");
        validarPeriodo(desde, hasta);

        Specification<Notificacion> filtros =
                (root, query, cb) ->
                        cb.equal(
                                root.get("usuarioId"),
                                usuarioId
                        );

        if (tipo != null) {

            filtros = filtros.and(
                    (root, query, cb) ->
                            cb.equal(
                                    root.get("tipo"),
                                    tipo
                            )
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

        return notificacionRepository.findAll(
                filtros,
                Sort.by(
                        Sort.Direction.DESC,
                        "fechaCreacion"
                )
        );
    }

    @Transactional(readOnly = true)
    public List<Notificacion> filtrarPorTipo(
            Long usuarioId,
            TipoNotificacion tipo
    ) {

        if (tipo == null) {
            throw new IllegalArgumentException(
                    "El tipo de notificacion es obligatorio"
            );
        }

        return buscarNotificaciones(
                usuarioId,
                tipo,
                null,
                null
        );
    }

    @Transactional(readOnly = true)
    public List<Notificacion> filtrarPorPeriodo(
            Long usuarioId,
            LocalDateTime desde,
            LocalDateTime hasta
    ) {

        if (desde == null || hasta == null) {
            throw new IllegalArgumentException(
                    "Debe indicar ambas fechas"
            );
        }

        return buscarNotificaciones(
                usuarioId,
                null,
                desde,
                hasta
        );
    }

    private void validarIdentificador(
            Long identificador,
            String nombre
    ) {

        if (identificador == null || identificador <= 0) {
            throw new IllegalArgumentException(
                    "El identificador de " + nombre
                            + " debe ser positivo"
            );
        }
    }

    private void validarPeriodo(
            LocalDateTime desde,
            LocalDateTime hasta
    ) {

        if (desde != null
                && hasta != null
                && desde.isAfter(hasta)) {

            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la fecha final"
            );
        }
    }
}