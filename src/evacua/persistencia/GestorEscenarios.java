package evacua.persistencia;

import evacua.importacion.*;
import evacua.edificio.Condicion;
import evacua.edificio.Conexion;
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
import java.util.HashSet;
import java.util.Set;

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
        boolean hayCondiciones = false;
        for (int f = 0; f < e.filas; f++) {
            for (int c = 0; c < e.cols; c++) {
                Nodo nodo = e.n[f][c];
                if (!nodo.danado && !nodo.peligroso && !(nodo.bloqueado && nodo.bloqueoReparable)) continue;
                if (!hayCondiciones) sb.append("#CONDICIONES\n");
                hayCondiciones = true;
                sb.append(posicion(e, f * e.cols + c));
                if (nodo.danado) sb.append(" DANADO");
                if (nodo.peligroso) sb.append(" PELIGROSO");
                if (nodo.bloqueado && nodo.bloqueoReparable) sb.append(" REPARABLE");
                sb.append('\n');
            }
        }
        if (e.conexionesExplicitas) sb.append("#GRAFO_EXPLICITO\n");
        if (!e.conexiones.isEmpty()) sb.append("#CONEXIONES\n");
        for (Conexion k : e.conexiones) {
            sb.append(posicion(e, k.origen)).append(' ').append(posicion(e, k.destino)).append(' ')
                    .append(k.tiempo).append(' ').append(k.capacidad).append(' ').append(k.condicion)
                    .append(' ').append(k.bloqueada ? "BLOQUEADA" : "ABIERTA").append('\n');
        }
        return sb.toString();
    }

    public static Edificio leer(File f) {
        try {
            String texto = Files.readString(f.toPath(), StandardCharsets.UTF_8);
            String nombre = f.getName().toLowerCase(java.util.Locale.ROOT);
            if (nombre.endsWith(".json")) return ConstructorEdificio.construir(ParserJson.parse(texto));
            if (nombre.endsWith(".xml")) return ConstructorEdificio.construir(ParserXml.parse(texto));
            return deTexto(texto);
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo leer el archivo: " + ex.getMessage());
        }
    }

    public static void guardar(File f, Edificio e) throws IOException {
        List<String> errores = validarParaGuardar(e);
        if (!errores.isEmpty()) throw new IllegalArgumentException(String.join(" ", errores));
        String nombre = f.getName().toLowerCase(java.util.Locale.ROOT);
        boolean xml = nombre.endsWith(".xml");
        String texto = xml || nombre.endsWith(".json") ? new EscritorPlano(xml).escribir(e) : aTexto(e, true);
        Files.writeString(f.toPath(), texto, StandardCharsets.UTF_8);
    }

    public static java.util.List<String> validarParaGuardar(Edificio e) {
        java.util.List<String> err = new java.util.ArrayList<>();
        boolean salida = false;
        for (int i = 0; i < e.filas; i++)
            for (int j = 0; j < e.cols; j++) if (e.n[i][j].tipo == TipoNodo.SALIDA) salida = true;
        if (!salida) err.add("El edificio necesita al menos una salida.");
        for (Persona p : e.personas)
            if (!e.dentro(p.f, p.c) || e.n[p.f][p.c].tipo == TipoNodo.VACIO || e.n[p.f][p.c].bloqueado)
                err.add(p.etiqueta() + " no esta ubicada en un nodo valido.");
        return err;
    }

    public static Edificio deTexto(String txt) {
        String[] lineas = txt.split("\\R");
        List<List<String>> pisos = new ArrayList<>();
        List<String> actual = new ArrayList<>();
        List<String> personas = new ArrayList<>();
        List<String> condiciones = new ArrayList<>();
        List<String> conexiones = new ArrayList<>();
        List<String> registros = null;
        boolean explicitas = false;
        for (String raw : lineas) {
            String ln = raw.trim();
            if (ln.isEmpty()) continue;
            if (ln.toUpperCase().startsWith("#PISO") || ln.startsWith("---")) {
                registros = null;
                if (!actual.isEmpty()) pisos.add(actual);
                actual = new ArrayList<>();
            } else if (ln.equals("#PERSONAS")) {
                registros = personas;
            } else if (ln.equals("#CONDICIONES")) {
                registros = condiciones;
            } else if (ln.equals("#CONEXIONES")) {
                registros = conexiones;
            } else if (ln.equals("#GRAFO_EXPLICITO")) {
                explicitas = true;
            } else if (ln.startsWith("#")) {
                continue;
            } else if (registros != null) {
                registros.add(ln);
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
        m.conexionesExplicitas = explicitas;
        leerConexiones(m, conexiones);
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
        // Una zona puede haber sufrido daños después de colocar a sus ocupantes.
        leerCondiciones(m, condiciones);
        evacua.edificio.Departamentos.detectar(m);
        return m;
    }

    private static String posicion(Edificio e, int id) {
        int f = id / e.cols;
        return (e.piso(f) + 1) + ":" + (f % e.filasPorPiso) + ":" + (id % e.cols);
    }

    private static int nodo(Edificio e, String posicion) {
        String[] partes = posicion.split(":", -1);
        if (partes.length != 3) throw new IllegalArgumentException("Se esperaba piso:fila:columna");
        int piso = Integer.parseInt(partes[0]);
        int fila = Integer.parseInt(partes[1]);
        int col = Integer.parseInt(partes[2]);
        if (piso < 1 || piso > e.pisos || fila < 0 || fila >= e.filasPorPiso || col < 0 || col >= e.cols)
            throw new IllegalArgumentException("Coordenada fuera del plano: " + posicion);
        int f = (piso - 1) * e.filasPorPiso + fila;
        if (e.n[f][col].tipo == TipoNodo.VACIO)
            throw new IllegalArgumentException("Celda vacía: " + posicion);
        return f * e.cols + col;
    }

    private static void leerCondiciones(Edificio e, List<String> lineas) {
        Set<Integer> vistos = new HashSet<>();
        for (String linea : lineas) {
            try {
                String[] t = linea.split("\\s+");
                if (t.length < 2) throw new IllegalArgumentException("Se esperaba posición y condición");
                int id = nodo(e, t[0]);
                if (!vistos.add(id)) throw new IllegalArgumentException("Nodo repetido");
                Nodo n = e.n[id / e.cols][id % e.cols];
                for (int i = 1; i < t.length; i++) {
                    switch (t[i]) {
                        case "NORMAL": break;
                        case "DANADO": n.danado = true; break;
                        case "PELIGROSO": n.peligroso = true; break;
                        case "REPARABLE":
                            if (!n.bloqueado) throw new IllegalArgumentException("Se requiere un bloqueo");
                            n.bloqueoReparable = true;
                            break;
                        default: throw new IllegalArgumentException("Condición desconocida: " + t[i]);
                    }
                }
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("#CONDICIONES: " + linea + " (" + ex.getMessage() + ")", ex);
            }
        }
    }

    private static void leerConexiones(Edificio e, List<String> lineas) {
        Set<String> vistas = new HashSet<>();
        for (String linea : lineas) {
            try {
                String[] t = linea.split("\\s+");
                if (t.length != 6) throw new IllegalArgumentException("Se esperaban seis campos");
                int a = nodo(e, t[0]), b = nodo(e, t[1]);
                if (!vistas.add(Math.min(a, b) + ":" + Math.max(a, b)))
                    throw new IllegalArgumentException("Conexión repetida");
                Condicion condicion = Condicion.valueOf(t[4]);
                if (!t[5].equals("ABIERTA") && !t[5].equals("BLOQUEADA"))
                    throw new IllegalArgumentException("Se esperaba ABIERTA o BLOQUEADA");
                Conexion k = e.conectar(a / e.cols, a % e.cols, b / e.cols, b % e.cols,
                        Double.parseDouble(t[2]), Integer.parseInt(t[3]));
                k.condicion = condicion;
                k.bloqueada = t[5].equals("BLOQUEADA");
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("#CONEXIONES: " + linea + " (" + ex.getMessage() + ")", ex);
            }
        }
    }
}
