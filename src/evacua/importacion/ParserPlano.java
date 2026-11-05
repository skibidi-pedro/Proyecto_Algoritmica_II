package evacua.importacion;

import evacua.edificio.Edificio;
import evacua.edificio.TipoNodo;
import evacua.personas.EstadoSalud;
import evacua.personas.TipoPersona;

import java.util.ArrayList;
import java.util.List;

/** Parser del plano (.txt) del edificio: valida el archivo y construye el edificio. */
public class ParserPlano {
    private ParserPlano() {
    }

    /** Lee un plano segun la extension del archivo (.json, .xml o .txt). Si es invalido lanza IllegalArgumentException. */
    public static Edificio leer(String contenido, String nombreArchivo) {
        String n = nombreArchivo == null ? "" : nombreArchivo.toLowerCase();
        if (n.endsWith(".json")) return ConstructorEdificio.construir(ParserJson.parse(contenido));
        if (n.endsWith(".xml")) return ConstructorEdificio.construir(ParserXml.parse(contenido));
        return deTexto(contenido);
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
