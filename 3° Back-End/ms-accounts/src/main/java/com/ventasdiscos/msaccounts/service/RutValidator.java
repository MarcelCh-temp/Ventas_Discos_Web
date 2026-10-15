package com.ventasdiscos.msaccounts.service;

/**
 * Validación de RUT chileno con dígito verificador (módulo 11). HU-01, HU-07.
 * Va como clase aparte para poder probarla sola y reutilizarla.
 */
public final class RutValidator {

    private RutValidator() {
    }

    /** "12.345.678-k" -> "12345678-K". Devuelve null si el formato no es reconocible. */
    public static String normalizar(String rut) {
        if (rut == null) {
            return null;
        }
        String limpio = rut.replace(".", "").replace("-", "").replace(" ", "").toUpperCase();
        if (!limpio.matches("\\d{7,8}[0-9K]")) {
            return null;
        }
        return limpio.substring(0, limpio.length() - 1) + "-" + limpio.charAt(limpio.length() - 1);
    }

    /** true si el formato es correcto y el DV coincide. */
    public static boolean esValido(String rut) {
        String normal = normalizar(rut);
        if (normal == null) {
            return false;
        }
        String cuerpo = normal.substring(0, normal.indexOf('-'));
        char dv = normal.charAt(normal.length() - 1);
        return calcularDv(cuerpo) == dv;
    }

    static char calcularDv(String cuerpo) {
        int suma = 0;
        int multiplicador = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += Character.getNumericValue(cuerpo.charAt(i)) * multiplicador;
            multiplicador = multiplicador == 7 ? 2 : multiplicador + 1;
        }
        int resto = 11 - (suma % 11);
        if (resto == 11) {
            return '0';
        }
        if (resto == 10) {
            return 'K';
        }
        return (char) ('0' + resto);
    }
}
