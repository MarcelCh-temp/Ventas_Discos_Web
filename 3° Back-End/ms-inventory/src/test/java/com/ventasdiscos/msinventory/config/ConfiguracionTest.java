package com.ventasdiscos.msinventory.config;

import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Auditoría AC2/CA2/CA3: comprueba que la configuración REAL (src/main/resources) lee cada dato
 * desde su variable de entorno y que .env.example declara todas las variables obligatorias.
 */
class ConfiguracionTest {

    private static final String RUTA = "src/main/resources/application.properties";

    private static Properties cargar(String ruta) throws Exception {
        Properties props = new Properties();
        try (Reader r = Files.newBufferedReader(Path.of(ruta))) {
            props.load(r);
        }
        return props;
    }

    @Test
    void propiedadesLeenVariablesDeEntorno() throws Exception {
        Properties props = cargar(RUTA);
        assertEquals("${JWT_SECRET}", props.getProperty("security.jwt.secret"),
                "security.jwt.secret debe ser ${JWT_SECRET} en " + Path.of(RUTA).toAbsolutePath()
                        + "  -> el secreto real va en el .env, no aquí");
        String usuario = props.getProperty("spring.datasource.username");
        assertTrue(usuario != null && usuario.startsWith("${DB_USER"),
                "spring.datasource.username debe ser ${DB_USER:root} pero en " + Path.of(RUTA).toAbsolutePath()
                        + " es: " + usuario + "  -> el valor real va en el .env, no aquí");
        String clave = props.getProperty("spring.datasource.password");
        assertTrue(clave != null && clave.startsWith("${DB_PASSWORD"),
                "spring.datasource.password debe ser ${DB_PASSWORD:} pero es: " + clave
                        + "  -> el valor real va en el .env, no aquí");
        assertTrue(props.getProperty("spring.config.import").contains(".env"),
                "Debe importarse el archivo .env");

    }

    @Test
    void envExampleDeclaraLasVariablesObligatorias() throws Exception {
        Properties props = cargar(RUTA);
        Properties ejemplo = cargar(".env.example");
        // Variables SIN valor por defecto (${VAR}) son obligatorias: deben estar en .env.example
        Matcher m = Pattern.compile("\\$\\{([A-Z_]+)\\}").matcher(String.join("\n",
                props.values().stream().map(Object::toString).toList()));
        while (m.find()) {
            assertTrue(ejemplo.containsKey(m.group(1)), ".env.example no declara " + m.group(1));
        }
        assertTrue(ejemplo.containsKey("DB_USER"));
        assertTrue(ejemplo.containsKey("DB_PASSWORD"));
    }
}
