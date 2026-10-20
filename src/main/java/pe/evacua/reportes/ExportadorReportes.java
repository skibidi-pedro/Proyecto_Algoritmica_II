package pe.evacua.reportes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import pe.evacua.estructuras.ListaSimple;

/**
 * Exportación individual por persona (sección 2.9, RN-14): un archivo de texto por persona con su
 * línea de tiempo completa, tanto si evacuó como si venció el tiempo límite.
 */
public final class ExportadorReportes {

    private ExportadorReportes() {
    }

    /** Escribe el reporte de una persona en {@code carpeta/persona_<id>.txt} y devuelve la ruta. */
    public static Path exportarPersona(ReportePersona reporte, Path carpeta) throws IOException {
        Files.createDirectories(carpeta);
        Path archivo = carpeta.resolve("persona_" + nombreSeguro(reporte.personaId()) + ".txt");
        Files.writeString(archivo, aTexto(reporte), StandardCharsets.UTF_8);
        return archivo;
    }

    /**
     * Exporta a todas las personas. Devuelve las rutas escritas, en el mismo orden que los reportes.
     * Si dos identificadores producen el mismo nombre de archivo, el segundo recibe un sufijo numérico.
     */
    public static ListaSimple<Path> exportarTodos(ListaSimple<ReportePersona> reportes, Path carpeta) throws IOException {
        Files.createDirectories(carpeta);
        ListaSimple<Path> escritos = new ListaSimple<>();
        Set<String> usados = new HashSet<>();
        for (ReportePersona reporte : reportes) {
            String base = "persona_" + nombreSeguro(reporte.personaId());
            String nombre = base;
            int sufijo = 2;
            while (!usados.add(nombre)) {
                nombre = base + "_" + sufijo++;
            }
            Path archivo = carpeta.resolve(nombre + ".txt");
            Files.writeString(archivo, aTexto(reporte), StandardCharsets.UTF_8);
            escritos.agregarFinal(archivo);
        }
        return escritos;
    }

    /** Contenido del reporte individual. */
    public static String aTexto(ReportePersona r) {
        StringBuilder sb = new StringBuilder();
        sb.append("REPORTE INDIVIDUAL - Persona ").append(r.personaId()).append('\n');
        sb.append("Tipo: ").append(r.tipo()).append('\n');
        if (r.evacuada()) {
            sb.append("Resultado: EVACUADA en el paso ").append(r.pasoSalida()).append('\n');
        } else {
            sb.append("Resultado: NO EVACUADA (venció el tiempo límite)\n");
        }
        sb.append("Nivel de estrés final: ").append(r.estresFinal()).append(" / 100\n");
        sb.append("Bloqueos sufridos: ").append(r.bloqueosSufridos()).append('\n');
        sb.append("Recálculos de ruta: ").append(r.recalculosRuta()).append('\n');
        sb.append("Tiempo de espera en cuellos de botella: ").append(r.pasosEsperaCuelloBotella()).append(" pasos\n");
        sb.append('\n');
        sb.append("LÍNEA DE TIEMPO (").append(r.bitacora().tamanio()).append(" registros)\n");
        sb.append(String.format(Locale.ROOT, "%6s  %-12s  %s%n", "Paso", "Nodo", "Estado del nodo"));
        for (RegistroBitacora b : r.bitacora()) {
            sb.append(String.format(Locale.ROOT, "%6d  %-12s  %s%n", b.paso(), b.nodoId(), b.estadoNodo()));
        }
        return sb.toString();
    }

    private static String nombreSeguro(String id) {
        String seguro = id == null ? "" : id.replaceAll("[^A-Za-z0-9_-]", "_");
        return seguro.isEmpty() ? "sin_id" : seguro;
    }
}
