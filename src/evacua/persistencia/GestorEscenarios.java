package evacua.persistencia;

import evacua.edificio.Edificio;
import evacua.edificio.Nodo;
import evacua.edificio.TipoNodo;
import evacua.importacion.ParserPlano;
import evacua.personas.Persona;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Guardado y carga de escenarios (plano + bloqueos + personas) en archivos .txt. */
public class GestorEscenarios {
    private GestorEscenarios() {
    }

    public static String aTexto(Edificio e, boolean conPersonas) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < e.filas; i++) {
            for (int j = 0; j < e.cols; j++) {
                Nodo nd = e.n[i][j];
                char ch = nd.tipo.letra;
                if (nd.tipo != TipoNodo.VACIO && nd.bloqueado) ch = Character.toLowerCase(ch);
                sb.append(ch);
            }
            sb.append('\n');
        }
        if (conPersonas && !e.personas.isEmpty()) {
            sb.append("#PERSONAS\n");
            for (Persona p : e.personas)
                sb.append(p.f).append(' ').append(p.c).append(' ')
                        .append(p.tipo.name()).append(' ').append(p.salud.name()).append('\n');
        }
        return sb.toString();
    }

    /** Lee y valida un escenario. Lanza IllegalArgumentException si no se puede leer o es invalido. */
    public static Edificio leer(File f) {
        try {
            return ParserPlano.deTexto(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo leer el archivo: " + ex.getMessage());
        }
    }

    public static void guardar(File f, Edificio e) throws IOException {
        Files.write(f.toPath(), aTexto(e, true).getBytes(StandardCharsets.UTF_8));
    }
}
