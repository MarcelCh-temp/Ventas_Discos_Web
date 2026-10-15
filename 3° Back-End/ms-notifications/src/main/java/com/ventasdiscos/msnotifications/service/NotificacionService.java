
package com.ventasdiscos.msnotifications.service;

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

    // HU-54: Listar notificaciones de un usuario
    @Transactional(readOnly = true)
    public List<Notificacion> listarPorUsuario(Long usuarioId) {
        validarUsuarioId(usuarioId);

        return notificacionRepository
                .findByUsuarioIdOrderByFechaCreacionDesc(usuarioId);
    }

    // HU-54: Contar notificaciones no leidas
    @Transactional(readOnly = true)
    public long contarNoLeidas(Long usuarioId) {
        validarUsuarioId(usuarioId);

        return notificacionRepository
                .countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    // HU-54: Marcar una notificacion como leida
    @Transactional
    public void marcarComoLeida(Long notificacionId, Long usuarioId) {
        validarUsuarioId(usuarioId);

        if (notificacionId == null || notificacionId <= 0) {
            throw new IllegalArgumentException(
                    "El identificador de la notificacion debe ser positivo"
            );
        }

        Notificacion notificacion = notificacionRepository
                .findById(notificacionId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "La notificacion no existe"
                        )
                );

        if (!notificacion.perteneceA(usuarioId)) {
            throw new IllegalArgumentException(
                    "La notificacion no pertenece al usuario"
            );
        }

        notificacion.marcarComoLeida();
        notificacionRepository.save(notificacion);
    }

    // HU-55: Buscar con filtros opcionales combinados
    @Transactional(readOnly = true)
    public List<Notificacion> buscarNotificaciones(
            Long usuarioId,
            TipoNotificacion tipo,
            LocalDateTime desde,
            LocalDateTime hasta
    ) {
        validarUsuarioId(usuarioId);
        validarPeriodo(desde, hasta);

        Specification<Notificacion> filtros = (root, query, cb) ->
                cb.equal(root.get("usuarioId"), usuarioId);

        if (tipo != null) {
            filtros = filtros.and((root, query, cb) ->
                    cb.equal(root.get("tipo"), tipo)
            );
        }

        if (desde != null) {
            filtros = filtros.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(
                            root.get("fechaCreacion"),
                            desde
                    )
            );
        }

        if (hasta != null) {
            filtros = filtros.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(
                            root.get("fechaCreacion"),
                            hasta
                    )
            );
        }

        return notificacionRepository.findAll(
                filtros,
                Sort.by(Sort.Direction.DESC, "fechaCreacion")
        );
    }

    // Compatibilidad con los metodos anteriores
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

        return buscarNotificaciones(usuarioId, tipo, null, null);
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

        return buscarNotificaciones(usuarioId, null, desde, hasta);
    }

    private void validarUsuarioId(Long usuarioId) {
        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del usuario debe ser positivo"
            );
        }
    }

    private void validarPeriodo(
            LocalDateTime desde,
            LocalDateTime hasta
    ) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la fecha final"
            );
        }
    }
}
