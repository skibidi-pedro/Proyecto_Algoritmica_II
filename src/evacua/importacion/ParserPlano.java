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

    /** Valida y construye. Lanza IllegalArgumentException con el motivo del error. */
        public static Edificio deTexto(String txt) {
            String[] lineas = txt.split("\\R");
            List<String> grid = new ArrayList<>();
            int i = 0;
            while (i < lineas.length && lineas[i].trim().isEmpty()) i++;
            while (i < lineas.length && !lineas[i].trim().isEmpty() && !lineas[i].startsWith("#")) {
                grid.add(lineas[i].trim());
                i++;
            }
            if (grid.isEmpty()) throw new IllegalArgumentException("El archivo no contiene un plano.");
            int cols = grid.get(0).length();
            Edificio m = new Edificio(grid.size(), cols);
            boolean haySalida = false;
            for (int f = 0; f < grid.size(); f++) {
                String ln = grid.get(f);
                if (ln.length() != cols)
                    throw new IllegalArgumentException("La fila " + (f + 1) + " tiene " + ln.length()
                            + " columnas y se esperaban " + cols + ".");
                for (int c = 0; c < cols; c++) {
                    char ch = ln.charAt(c);
                    TipoNodo t = ch == '.' ? TipoNodo.VACIO : TipoNodo.de(ch);
                    if (t == null)
                        throw new IllegalArgumentException("Caracter invalido '" + ch + "' en fila "
                                + (f + 1) + ", columna " + (c + 1) + ".");
                    m.n[f][c].tipo = t;
                    m.n[f][c].bloqueado = t != TipoNodo.VACIO && Character.isLowerCase(ch);
                    if (t == TipoNodo.SALIDA) haySalida = true;
                }
            }
            if (!haySalida) throw new IllegalArgumentException("El plano debe tener al menos una salida (S).");
            for (; i < lineas.length; i++) {
                String ln = lineas[i].trim();
                if (ln.isEmpty() || ln.startsWith("#")) continue;
                String[] tk = ln.split("\\s+");
                int f, c;
                TipoPersona tp;
                EstadoSalud es;
                try {
                    f = Integer.parseInt(tk[0]);
                    c = Integer.parseInt(tk[1]);
                    tp = TipoPersona.valueOf(tk[2]);
                    es = EstadoSalud.valueOf(tk[3]);
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
