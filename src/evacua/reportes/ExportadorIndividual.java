package evacua.reportes;

import evacua.personas.Persona;
import evacua.simulacion.MotorSimulacion;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Exporta el reporte individual (linea de tiempo completa) de cada persona (RN-14). */
public final class ExportadorIndividual {
    private ExportadorIndividual() {
    }

    public static void exportar(Persona p, File archivo) throws IOException {
        Files.write(archivo.toPath(), ReporteSimulacion.individual(p).getBytes(StandardCharsets.UTF_8));
    }

    /** Un archivo por persona (reporte_P{id}.txt), tanto si evacuo como si vencio el tiempo limite. */
    public static int exportarTodos(MotorSimulacion m, File carpeta) throws IOException {
        Files.createDirectories(carpeta.toPath());
        int k = 0;
        for (Persona p : m.mapa.personas) {
            exportar(p, new File(carpeta, "reporte_P" + p.id + ".txt"));
            k++;
        }
        return k;
    }
}
