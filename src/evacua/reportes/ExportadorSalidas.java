package evacua.reportes;

import evacua.simulacion.MotorSimulacion;
import evacua.simulacion.RegistroSalida;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

/** Escribe un CSV con el tiempo y el estado de cada persona que salio, en orden de salida (usa la cola de salidas). */
public final class ExportadorSalidas {
    public static final String CABECERA = "orden_salida,id,tipo,salud,tiempo_salida,estres_al_salir,asistida,pasos,recalculos";

    private ExportadorSalidas() {
    }

    /** La cola queda intacta: se desencola cada registro y se vuelve a encolar al final, asi se puede exportar varias veces. */
    public static int exportar(MotorSimulacion m, File archivo) throws IOException {
        StringBuilder sb = new StringBuilder(CABECERA).append('\n');
        int n = m.salidas.tamano();
        for (int i = 0; i < n; i++) {
            RegistroSalida r = m.salidas.desencolar();
            sb.append(String.format(Locale.ROOT, "%d,%d,%s,%s,%d,%.1f,%s,%d,%d%n", r.orden, r.id, r.tipo, r.salud, r.tiempo, r.estres, r.asistida, r.pasos, r.recalculos));
            m.salidas.encolar(r);
        }
        File dir = archivo.getAbsoluteFile().getParentFile();
        if (dir != null) Files.createDirectories(dir.toPath());
        Files.write(archivo.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
        return n;
    }
}
