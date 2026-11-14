package ms_ordenes.ms_ordenes.model;

import jakarta.validation.constraints.*;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ms_ordenes.ms_ordenes.exception.ValidacionNegocioException;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * UML 02 Despacho (Pedido "1" *-- "0..1" Despacho). Lote 3 (RF-PED-08, HU-33).
 * numeroSeguimiento es opcional: el seguimiento no exige tracking externo (RF-MIP-03, RN-40).
 */
@Entity
@Table(name = "despacho")
public class Despacho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Atributo id: propiedad que se guarda o utiliza en esta entidad.
    private Long id;

    @Column(name = "fecha_despacho", nullable = false)
    @NotNull
    // Atributo fechaDespacho: propiedad que se guarda o utiliza en esta entidad.
    private LocalDateTime fechaDespacho;

    @Column(nullable = false, length = 100)
    // Atributo transportista: propiedad que se guarda o utiliza en esta entidad.
    private String transportista;

    @Column(name = "numero_seguimiento", length = 100)
    // Atributo numeroSeguimiento: propiedad que se guarda o utiliza en esta entidad.
    private String numeroSeguimiento;

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método Despacho (despacho).
    protected Despacho() {
    }

    Despacho(String transportista, String numeroSeguimiento, LocalDateTime fechaDespacho) {
        actualizar(transportista, numeroSeguimiento, fechaDespacho);
    }

    // Ejecuta actualizar como parte de las responsabilidades de esta clase.
    final void actualizar(String transportista, String numeroSeguimiento, LocalDateTime fechaDespacho) {
        String t = transportista == null ? "" : transportista.trim();
        if (t.isEmpty()) {
            throw new ValidacionNegocioException("Indica el transportista",
                    Map.of("transportista", "El transportista es obligatorio"));
        }
        LocalDateTime fecha = fechaDespacho == null ? LocalDateTime.now() : fechaDespacho;
        if (fecha.isAfter(LocalDateTime.now().plusMinutes(5))) {
            throw new ValidacionNegocioException("La fecha de despacho no puede ser futura",
                    Map.of("fechaDespacho", "No puede ser una fecha futura"));
        }
        this.transportista = t;
        String n = numeroSeguimiento == null ? null : numeroSeguimiento.trim();
        this.numeroSeguimiento = n == null || n.isEmpty() ? null : n;
        this.fechaDespacho = fecha;
    }

    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getId (consultar id).
    public Long getId() { return id; }
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getFechaDespacho (consultar fecha despacho).
    public LocalDateTime getFechaDespacho() { return fechaDespacho; }
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getTransportista (consultar transportista).
    public String getTransportista() { return transportista; }
    // Operación auxiliar que forma parte de la responsabilidad de esta clase. Método getNumeroSeguimiento (consultar numero seguimiento).
    public String getNumeroSeguimiento() { return numeroSeguimiento; }
}
