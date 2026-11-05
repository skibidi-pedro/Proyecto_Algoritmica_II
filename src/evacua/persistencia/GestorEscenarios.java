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
        for (int p = 0; p < e.pisos; p++) {
            if (e.pisos > 1) sb.append("#PISO ").append(p + 1).append('\n');
            for (int i = p * e.filasPorPiso; i < (p + 1) * e.filasPorPiso; i++) {
                for (int j = 0; j < e.cols; j++) {
                    Nodo nd = e.n[i][j];
                    char ch = nd.tipo.letra;
                    if (nd.tipo != TipoNodo.VACIO && nd.bloqueado) ch = Character.toLowerCase(ch);
                    sb.append(ch);
                }
                sb.append('\n');
            }
        }
        if (conPersonas && !e.personas.isEmpty()) {
            sb.append("#PERSONAS\n");
            for (Persona p : e.personas) {
                if (e.pisos > 1) sb.append(e.piso(p.f) + 1).append(' ').append(p.f % e.filasPorPiso);
                else sb.append(p.f);
                sb.append(' ').append(p.c).append(' ').append(p.tipo.name()).append(' ').append(p.salud.name()).append('\n');
            }
        }
        return sb.toString();
    }

    /** Lee y valida un escenario. Lanza IllegalArgumentException si no se puede leer o es invalido. */
    public static Edificio leer(File f) {
        try {
            return ParserPlano.leer(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8), f.getName());
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo leer el archivo: " + ex.getMessage());
        }
    }

    public static void guardar(File f, Edificio e) throws IOException {
        String contenido = f.getName().toLowerCase().endsWith(".json") ? aJson(e) : aTexto(e, true);
        Files.write(f.toPath(), contenido.getBytes(StandardCharsets.UTF_8));
    }

    /** RN-10: un escenario solo se guarda si hay al menos una salida y todas las personas estan en un nodo valido. */
    public static java.util.List<String> validarParaGuardar(Edificio e) {
        java.util.List<String> err = new java.util.ArrayList<>();
        boolean salida = false;
        for (int i = 0; i < e.filas; i++)
            for (int j = 0; j < e.cols; j++) if (e.n[i][j].tipo == TipoNodo.SALIDA) salida = true;
        if (!salida) err.add("El edificio necesita al menos una salida.");
        for (Persona p : e.personas)
            if (!e.dentro(p.f, p.c) || e.n[p.f][p.c].tipo == TipoNodo.VACIO)
                err.add(p.etiqueta() + " no esta ubicada en un nodo valido.");
        return err;
    }

    /** Serializa el escenario completo (nodos, conexiones, bloqueos y personas) a JSON; ParserPlano lo vuelve a leer. */
    public static String aJson(Edificio e) {
        StringBuilder sb = new StringBuilder("{\n  \"modo\": \"" + (e.conexionesExplicitas ? "grafo" : "cuadricula") + "\",\n  \"pisos\": " + e.pisos + ",\n  \"filas\": " + e.filasPorPiso + ",\n  \"columnas\": " + e.cols + ",\n  \"nodos\": [");
        boolean primero = true;
        for (int i = 0; i < e.filas; i++)
            for (int j = 0; j < e.cols; j++) {
                Nodo nd = e.n[i][j];
                if (nd.tipo == TipoNodo.VACIO) continue;
                sb.append(primero ? "\n" : ",\n").append("    {\"id\": \"").append(esc(e.idDe(i, j))).append("\", \"tipo\": \"")
                        .append(nd.tipo.name()).append("\", \"piso\": ").append(e.piso(i) + 1).append(", \"x\": ").append(j).append(", \"y\": ").append(i % e.filasPorPiso)
                        .append(", \"bloqueado\": ").append(nd.bloqueado).append('}');
                primero = false;
            }
        sb.append("\n  ],\n  \"conexiones\": [");
        primero = true;
        for (evacua.edificio.Conexion k : e.conexiones) {
            sb.append(primero ? "\n" : ",\n").append("    {\"origen\": \"").append(esc(e.idDe(k.origen / e.cols, k.origen % e.cols)))
                    .append("\", \"destino\": \"").append(esc(e.idDe(k.destino / e.cols, k.destino % e.cols)))
                    .append("\", \"tiempo\": ").append(k.tiempo).append(", \"capacidad\": ").append(k.capacidad)
                    .append(", \"bloqueada\": ").append(k.bloqueada).append('}');
            primero = false;
        }
        sb.append("\n  ],\n  \"personas\": [");
        primero = true;
        for (Persona p : e.personas) {
            sb.append(primero ? "\n" : ",\n").append("    {\"nodo\": \"").append(esc(e.idDe(p.f, p.c))).append("\", \"tipo\": \"")
                    .append(p.tipo.name()).append("\", \"salud\": \"").append(p.salud.name()).append("\"}");
            primero = false;
        }
        return sb.append("\n  ]\n}\n").toString();
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
