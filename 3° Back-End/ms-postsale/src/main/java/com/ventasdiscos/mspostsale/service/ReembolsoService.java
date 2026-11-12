package com.ventasdiscos.mspostsale.service;

import com.ventasdiscos.mspostsale.client.AccountsClient;
import com.ventasdiscos.mspostsale.dto.Reembolsar;
import com.ventasdiscos.mspostsale.dto.SolicitudDetalleResponse;
import com.ventasdiscos.mspostsale.exception.OperacionNoPermitidaException;
import com.ventasdiscos.mspostsale.model.SolicitudPostVenta;
import com.ventasdiscos.mspostsale.repository.SolicitudPostVentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Clase original. HU-77 / RF-POS-10 / RN-23 / RN-41: registra la SOLICITUD de reembolso cuando existe una
 * cancelación o devolución APROBADA del Cliente. Queda REGISTRADA: no se aprueba ni se rechaza y la plataforma no
 * procesa ni transfiere dinero (la devolución efectiva es presencial o externa).
 * Lote 4: ahora es una SolicitudPostVenta de tipo REEMBOLSO con solicitudOrigenId (UML 04).
 */
@Service
public class ReembolsoService {

    private final SolicitudPostVentaRepository solicitudes;
    private final AccountsClient accounts;
    private final PostSaleSupport support;

    public ReembolsoService(SolicitudPostVentaRepository solicitudes, AccountsClient accounts, PostSaleSupport support) {
        this.solicitudes = solicitudes;
        this.accounts = accounts;
        this.support = support;
    }

    @Transactional
    public SolicitudDetalleResponse registrar(Long usuarioId, Reembolsar req) {
        // Bloquea la solicitud de origen: dos clics simultáneos no crean dos reembolsos (además de uk_solicitud_origen).
        SolicitudPostVenta origen = solicitudes.bloquear(req.solicitudOrigenId()).orElse(null);
        SolicitudPostVenta reembolso = SolicitudPostVenta.reembolso(origen, usuarioId, req.motivoSolicitud());
        if (solicitudes.existsBySolicitudOrigenId(origen.getId())) {
            throw new OperacionNoPermitidaException("Ya registraste una solicitud de reembolso para la "
                    + SolicitudPostVenta.nombre(origen.getTipo()) + " " + origen.getId());
        }
        // Lote 5 (HU-78, RN-37, RN-44): justo antes de guardar, Accounts confirma que la cuenta está ACTIVA.
        accounts.exigirCuentaActiva(usuarioId);
        SolicitudPostVenta s = solicitudes.saveAndFlush(reembolso);
        support.notificarTrasCommit("reembolso-" + s.getId() + "-registrado", usuarioId, "REEMBOLSO_REGISTRADO",
                "Solicitud de reembolso registrada",
                "Registramos tu solicitud de reembolso asociada a la " + SolicitudPostVenta.nombre(origen.getTipo())
                        + " " + origen.getId() + ". La devolución del dinero se realiza de forma presencial o mediante"
                        + " un procedimiento externo a la plataforma.", s.getId());
        return SolicitudDetalleResponse.from(s, false);
    }
}
