package evacua.persistencia;

import evacua.edificio.Edificio;
import evacua.edificio.Nodo;
import evacua.edificio.TipoNodo;
import evacua.personas.EstadoSalud;
import evacua.personas.TipoPersona;
import evacua.personas.Persona;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

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
            return deTexto(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo leer el archivo: " + ex.getMessage());
        }
    }

    public static void guardar(File f, Edificio e) throws IOException {
        Files.write(f.toPath(), aTexto(e, true).getBytes(StandardCharsets.UTF_8));
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

    /**
     * Lee el plano en texto. Cada piso es un bloque de filas; "#PISO n" empieza el siguiente piso.
     * Luego "#PERSONAS" y lineas "fila columna TIPO SALUD" (fila global) o "piso fila columna TIPO SALUD".
     * Valida y construye; lanza IllegalArgumentException con el motivo del error.
     */
    public static Edificio deTexto(String txt) {
        String[] lineas = txt.split("\\R");
        List<List<String>> pisos = new ArrayList<>();
        List<String> actual = new ArrayList<>();
        List<String> personas = new ArrayList<>();
        for (String raw : lineas) {
            String ln = raw.trim();
            if (ln.isEmpty()) continue;
            if (ln.toUpperCase().startsWith("#PISO") || ln.startsWith("---")) {
                if (!actual.isEmpty()) pisos.add(actual);
                actual = new ArrayList<>();
            } else if (ln.startsWith("#")) {
                continue;
            } else if (ln.matches("\\S+") && personas.isEmpty()) {
                actual.add(ln);
            } else {
                personas.add(ln);
            }
        }
        if (!actual.isEmpty()) pisos.add(actual);
        if (pisos.isEmpty()) throw new IllegalArgumentException("El archivo no contiene un plano.");
        int filasPiso = pisos.get(0).size();
        int cols = pisos.get(0).get(0).length();
        Edificio m = new Edificio(filasPiso, cols, pisos.size());
        boolean haySalida = false;
        for (int p = 0; p < pisos.size(); p++) {
            List<String> grid = pisos.get(p);
            if (grid.size() != filasPiso)
                throw new IllegalArgumentException("El piso " + (p + 1) + " tiene " + grid.size() + " filas y se esperaban " + filasPiso + ".");
            for (int f = 0; f < filasPiso; f++) {
                String ln = grid.get(f);
                if (ln.length() != cols)
                    throw new IllegalArgumentException("La fila " + (f + 1) + " del piso " + (p + 1) + " tiene " + ln.length()
                            + " columnas y se esperaban " + cols + ".");
                for (int c = 0; c < cols; c++) {
                    char ch = ln.charAt(c);
                    TipoNodo t = ch == '.' ? TipoNodo.VACIO : TipoNodo.de(ch);
                    if (t == null)
                        throw new IllegalArgumentException("Caracter invalido '" + ch + "' en piso " + (p + 1) + ", fila "
                                + (f + 1) + ", columna " + (c + 1) + ".");
                    int g = p * filasPiso + f;
                    m.n[g][c].tipo = t;
                    m.n[g][c].bloqueado = t != TipoNodo.VACIO && Character.isLowerCase(ch);
                    if (t == TipoNodo.SALIDA) haySalida = true;
                }
            }
        }
        if (!haySalida) throw new IllegalArgumentException("El plano debe tener al menos una salida (S).");
        for (String ln : personas) {
            String[] tk = ln.split("\\s+");
            int f, c;
            TipoPersona tp;
            EstadoSalud es;
            try {
                int o = tk.length == 5 ? 1 : 0;
                f = Integer.parseInt(tk[o]);
                c = Integer.parseInt(tk[o + 1]);
                if (tk.length == 5) f = (Integer.parseInt(tk[0]) - 1) * filasPiso + f;
                tp = TipoPersona.valueOf(tk[o + 2]);
                es = EstadoSalud.valueOf(tk[o + 3]);
                if (tk.length != 4 && tk.length != 5) throw new IllegalArgumentException();
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("Linea de persona invalida: \"" + ln + "\".");
            }
            if (!m.caminable(f, c))
                throw new IllegalArgumentException("La persona en (" + f + "," + c + ") no esta sobre un nodo libre.");
            m.agregar(tp, es, f, c);
        }
        return m;
    }
}
