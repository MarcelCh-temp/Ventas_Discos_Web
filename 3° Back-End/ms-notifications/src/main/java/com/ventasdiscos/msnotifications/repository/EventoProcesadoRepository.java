
package com.ventasdiscos.msnotifications.repository;

import com.ventasdiscos.msnotifications.model.EventoProcesado;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoProcesadoRepository
        extends JpaRepository<EventoProcesado, Long> {

    boolean existsByOrigenAndEventoId(
            String origen,
            String eventoId
    );
}
