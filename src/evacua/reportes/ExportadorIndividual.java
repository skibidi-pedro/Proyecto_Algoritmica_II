package evacua.reportes;

import evacua.personas.Persona;
import evacua.simulacion.MotorSimulacion;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class ExportadorIndividual {
    private ExportadorIndividual() {
    }

    public static void exportar(Persona p, File archivo) throws IOException {
        Files.write(archivo.toPath(), ReporteSimulacion.individual(p).getBytes(StandardCharsets.UTF_8));
    }

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
