package com.ventasdiscos.msnotifications.security;

import com.ventasdiscos.msnotifications.trazabilidad.CorrelationId;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Lote 6: se agrega al RestTemplate de este MS. En cada llamada saliente:
 * <ul>
 *   <li>DOC-05: si la URL es /api/v1/internal/**, agrega "Authorization: Bearer &lt;JWT de servicio&gt;" con un token
 *       NUEVO cuya audiencia es el MS dueño de esa ruta ({@link Servicios#audienciaPara}).</li>
 *   <li>DOC-06: propaga X-Correlation-Id de la petición en curso (o crea uno si la llamada nace de una tarea
 *       programada, donde no hay petición).</li>
 * </ul>
 * Los clientes (AccountsClient, CatalogClient, ...) no cambian: siguen usando el mismo RestTemplate.
 * Si la ruta interna no tiene dueño conocido, la llamada no se envía (IOException → los clientes la tratan como
 * servicio no disponible) para no mandar peticiones sin autenticar.
 */
public class LlamadaInternaInterceptor implements ClientHttpRequestInterceptor {

    // Atributo servicioJwt: dato o dependencia utilizada por esta clase.
    private final ServicioJwtService servicioJwt;

    // Ejecuta llamada interna interceptor como parte de las responsabilidades de esta clase.
    public LlamadaInternaInterceptor(ServicioJwtService servicioJwt) {
        this.servicioJwt = servicioJwt;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String path = request.getURI().getPath();
        if (path != null && path.startsWith(Servicios.PREFIJO_INTERNO)) {
            String audiencia = Servicios.audienciaPara(path);
            if (audiencia == null) {
                throw new IOException("Ruta interna sin servicio destino conocido: " + path);
            }
            request.getHeaders().setBearerAuth(servicioJwt.emitir(audiencia));
        }
        request.getHeaders().set(CorrelationId.HEADER, CorrelationId.actualOGenerar());
        return execution.execute(request, body);
    }
}
