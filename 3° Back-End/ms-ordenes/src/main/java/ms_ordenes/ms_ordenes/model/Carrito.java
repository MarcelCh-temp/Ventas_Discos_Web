package ms_ordenes.ms_ordenes.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import ms_ordenes.ms_ordenes.exception.RecursoNoEncontradoException;
import ms_ordenes.ms_ordenes.exception.ValidacionNegocioException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * UML 02 Carrito. Lote 3 (HU-18 a HU-21). Un carrito por Cliente (userId del token), guardado en la BD.
 * Reglas propias del carrito aquí; lo que depende de Catalog (producto activo, stock) lo valida CarritoService.
 */
@Entity
@Table(name = "carrito")
public class Carrito {

    /** Límite de productos distintos: el mismo que acepta la cotización de Catalog (PricingRequest). */
    public static final int MAX_PRODUCTOS = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "carrito", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ItemCarrito> items = new ArrayList<>();

    protected Carrito() {
    }

    public Carrito(Long userId) {
        this.userId = Objects.requireNonNull(userId);
        this.fechaActualizacion = LocalDateTime.now();
    }

    /** UML 02: total = suma de subtotales (con el último precio conocido). */
    public BigDecimal calcularTotal() {
        BigDecimal suma = BigDecimal.ZERO;
        for (ItemCarrito item : items) {
            suma = suma.add(item.calcularSubtotal());
        }
        this.total = suma;
        this.fechaActualizacion = LocalDateTime.now();
        return suma;
    }

    /** Cantidad que quedaría si se agregan "cantidad" unidades (para validar stock ANTES de cambiar). */
    public int cantidadResultante(Long productId, int cantidad) {
        validarCantidad(cantidad);
        if (buscar(productId).isEmpty() && items.size() >= MAX_PRODUCTOS) {
            throw new ValidacionNegocioException("El carrito admite hasta " + MAX_PRODUCTOS + " productos distintos");
        }
        int actual = buscar(productId).map(ItemCarrito::getCantidad).orElse(0);
        try {
            return Math.addExact(actual, cantidad);
        } catch (ArithmeticException e) {
            throw new ValidacionNegocioException("La cantidad es demasiado grande", Map.of("cantidad", "Cantidad inválida"));
        }
    }

    /** HU-18: si el producto ya está, suma las unidades. */
    public void agregar(Long productId, int cantidad, BigDecimal precioUnitario) {
        int nueva = cantidadResultante(productId, cantidad);
        Optional<ItemCarrito> existente = buscar(productId);
        if (existente.isPresent()) {
            existente.get().cambiarCantidad(nueva);
            existente.get().actualizarPrecio(precioUnitario);
        } else {
            items.add(new ItemCarrito(this, productId, nueva, precioUnitario));
        }
        calcularTotal();
    }

    /** HU-20 / RN-12: la cantidad es entera y >= 1; bajar a 0 no elimina (eliminar es otra acción). */
    public void cambiarCantidad(Long productId, int cantidad, BigDecimal precioUnitario) {
        validarCantidad(cantidad);
        ItemCarrito item = buscar(productId).orElseThrow(() -> noEsta(productId));
        item.cambiarCantidad(cantidad);
        item.actualizarPrecio(precioUnitario);
        calcularTotal();
    }

    /** HU-21: acción independiente de bajar la cantidad. */
    public void quitar(Long productId) {
        ItemCarrito item = buscar(productId).orElseThrow(() -> noEsta(productId));
        items.remove(item);
        calcularTotal();
    }

    public void vaciar() {
        items.clear();
        calcularTotal();
    }

    public void actualizarPrecio(Long productId, BigDecimal precio) {
        buscar(productId).ifPresent(i -> i.actualizarPrecio(precio));
    }

    public Optional<ItemCarrito> buscar(Long productId) {
        return items.stream().filter(i -> i.getProductId().equals(productId)).findFirst();
    }

    public boolean estaVacio() {
        return items.isEmpty();
    }

    private static void validarCantidad(int cantidad) {
        if (cantidad <= 0) {
            throw new ValidacionNegocioException("La cantidad debe ser un entero mayor que cero",
                    Map.of("cantidad", "Debe ser mayor que cero; para quitar el producto usa eliminar"));
        }
    }

    private static RecursoNoEncontradoException noEsta(Long productId) {
        return new RecursoNoEncontradoException("El producto " + productId + " no está en tu carrito");
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public BigDecimal getTotal() { return total; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public List<ItemCarrito> getItems() { return Collections.unmodifiableList(items); }
}
