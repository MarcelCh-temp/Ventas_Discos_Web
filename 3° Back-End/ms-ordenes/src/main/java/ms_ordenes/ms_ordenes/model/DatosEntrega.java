package ms_ordenes.ms_ordenes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ms_ordenes.ms_ordenes.exception.ValidacionNegocioException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UML 02 DatosEntrega (Pedido "1" *-- "1" DatosEntrega). Lote 3.
 * Formato de cada campo: en el DTO (@Size, @Pattern). Qué campos son OBLIGATORIOS según el método: aquí,
 * porque es regla de negocio (RF-COM-05/06: "retiro no debe exigir dirección"). Sin RUT (no está en el UML 02).
 */
@Entity
@Table(name = "datos_entrega")
public class DatosEntrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_entrega", nullable = false, length = 30)
    private MetodoEntrega metodoEntrega;

    @Column(name = "nombre_receptor", nullable = false, length = 100)
    private String nombreReceptor;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(length = 150)
    private String direccion;

    @Column(length = 80)
    private String comuna;

    @Column(length = 200)
    private String referencia;

    protected DatosEntrega() {
    }

    /** Crea los datos validando los obligatorios del método. En retiro no se guarda dirección ni comuna. */
    public static DatosEntrega crear(MetodoEntrega metodo, String nombreReceptor, String telefono,
                                     String direccion, String comuna, String referencia) {
        if (metodo == null) {
            throw new ValidacionNegocioException("Selecciona un método de entrega",
                    Map.of("metodoEntrega", "Selecciona un método de entrega disponible"));
        }
        DatosEntrega d = new DatosEntrega();
        d.metodoEntrega = metodo;
        d.nombreReceptor = limpiar(nombreReceptor);
        d.telefono = limpiar(telefono);
        d.direccion = metodo.requiereDireccion() ? limpiar(direccion) : null;
        d.comuna = metodo.requiereDireccion() ? limpiar(comuna) : null;
        d.referencia = limpiar(referencia);
        d.validarObligatorios();
        return d;
    }

    /**
     * RF-PED-05: corrige solo los campos enviados (null = no cambia). Devuelve los nombres de los campos que
     * cambiaron, para el historial. El método de entrega NO se corrige (no está entre los campos permitidos).
     */
    public List<String> corregir(String nombreReceptor, String telefono, String direccion, String comuna,
                                 String referencia) {
        if (!metodoEntrega.requiereDireccion() && (direccion != null || comuna != null)) {
            Map<String, String> errores = new LinkedHashMap<>();
            if (direccion != null) {
                errores.put("direccion", "No aplica para " + metodoEntrega.getNombre());
            }
            if (comuna != null) {
                errores.put("comuna", "No aplica para " + metodoEntrega.getNombre());
            }
            throw new ValidacionNegocioException("Hay datos que no corresponden al método de entrega", errores);
        }
        List<String> cambios = new ArrayList<>();
        String nuevoNombre = cambiar("nombreReceptor", this.nombreReceptor, nombreReceptor, cambios);
        String nuevoTelefono = cambiar("telefono", this.telefono, telefono, cambios);
        String nuevaDireccion = cambiar("direccion", this.direccion, direccion, cambios);
        String nuevaComuna = cambiar("comuna", this.comuna, comuna, cambios);
        String nuevaReferencia = cambiar("referencia", this.referencia, referencia, cambios);
        DatosEntrega prueba = crear(metodoEntrega, nuevoNombre, nuevoTelefono, nuevaDireccion, nuevaComuna,
                nuevaReferencia); // valida que no quede vacío un obligatorio; si falla, no se cambió nada
        this.nombreReceptor = prueba.nombreReceptor;
        this.telefono = prueba.telefono;
        this.direccion = prueba.direccion;
        this.comuna = prueba.comuna;
        this.referencia = prueba.referencia;
        return cambios;
    }

    private void validarObligatorios() {
        Map<String, String> errores = new LinkedHashMap<>();
        Map<String, String> valores = Map.of(
                "nombreReceptor", nvl(nombreReceptor), "telefono", nvl(telefono),
                "direccion", nvl(direccion), "comuna", nvl(comuna));
        for (String campo : metodoEntrega.camposObligatorios()) {
            if (valores.get(campo).isEmpty()) {
                errores.put(campo, "Es obligatorio para " + metodoEntrega.getNombre());
            }
        }
        if (!errores.isEmpty()) {
            throw new ValidacionNegocioException("Faltan datos de entrega obligatorios", errores);
        }
    }

    private static String cambiar(String campo, String actual, String nuevo, List<String> cambios) {
        if (nuevo == null) {
            return actual;
        }
        String limpio = limpiar(nuevo);
        if (!nvl(limpio).equals(nvl(actual))) {
            cambios.add(campo);
        }
        return limpio;
    }

    private static String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String t = valor.trim();
        return t.isEmpty() ? null : t;
    }

    private static String nvl(String valor) {
        return valor == null ? "" : valor;
    }

    public Long getId() { return id; }
    public MetodoEntrega getMetodoEntrega() { return metodoEntrega; }
    public String getNombreReceptor() { return nombreReceptor; }
    public String getTelefono() { return telefono; }
    public String getDireccion() { return direccion; }
    public String getComuna() { return comuna; }
    public String getReferencia() { return referencia; }
}
