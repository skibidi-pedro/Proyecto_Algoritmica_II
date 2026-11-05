package evacua.importacion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lector JSON minimo (objetos, arreglos, cadenas, numeros, booleanos, null). Sin dependencias. */
final class JsonMini {
    private final String s;
    private int i;

    private JsonMini(String s) {
        this.s = s;
    }

    static Object parse(String texto) {
        JsonMini p = new JsonMini(texto);
        p.ws();
        Object v = p.valor();
        p.ws();
        if (p.i < p.s.length()) throw p.error("contenido inesperado despues del JSON");
        return v;
    }

    private IllegalArgumentException error(String msg) {
        int linea = 1;
        for (int k = 0; k < Math.min(i, s.length()); k++) if (s.charAt(k) == '\n') linea++;
        return new IllegalArgumentException("JSON invalido (linea " + linea + "): " + msg + ".");
    }

    private void ws() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
    }

    private Object valor() {
        if (i >= s.length()) throw error("fin inesperado");
        char c = s.charAt(i);
        if (c == '{') return objeto();
        if (c == '[') return arreglo();
        if (c == '"') return cadena();
        if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
        if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
        if (s.startsWith("null", i)) { i += 4; return null; }
        return numero();
    }

    private Map<String, Object> objeto() {
        Map<String, Object> m = new LinkedHashMap<>();
        i++;
        ws();
        if (i < s.length() && s.charAt(i) == '}') { i++; return m; }
        while (true) {
            ws();
            if (i >= s.length() || s.charAt(i) != '"') throw error("se esperaba el nombre de un campo");
            String k = cadena();
            ws();
            if (i >= s.length() || s.charAt(i) != ':') throw error("se esperaba ':' despues de \"" + k + "\"");
            i++;
            ws();
            m.put(k, valor());
            ws();
            if (i < s.length() && s.charAt(i) == ',') { i++; continue; }
            if (i < s.length() && s.charAt(i) == '}') { i++; return m; }
            throw error("se esperaba ',' o '}'");
        }
    }

    private List<Object> arreglo() {
        List<Object> l = new ArrayList<>();
        i++;
        ws();
        if (i < s.length() && s.charAt(i) == ']') { i++; return l; }
        while (true) {
            ws();
            l.add(valor());
            ws();
            if (i < s.length() && s.charAt(i) == ',') { i++; continue; }
            if (i < s.length() && s.charAt(i) == ']') { i++; return l; }
            throw error("se esperaba ',' o ']'");
        }
    }

    private String cadena() {
        StringBuilder sb = new StringBuilder();
        i++;
        while (i < s.length() && s.charAt(i) != '"') {
            char c = s.charAt(i++);
            if (c == '\\' && i < s.length()) {
                char e = s.charAt(i++);
                switch (e) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (i + 4 > s.length()) throw error("secuencia \\u incompleta");
                        sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                        break;
                    default: sb.append(e);
                }
            } else sb.append(c);
        }
        if (i >= s.length()) throw error("cadena sin cerrar");
        i++;
        return sb.toString();
    }

    private Double numero() {
        int ini = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
        if (ini == i) throw error("valor no reconocido '" + s.charAt(i) + "'");
        try {
            return Double.valueOf(s.substring(ini, i));
        } catch (NumberFormatException ex) {
            throw error("numero invalido '" + s.substring(ini, i) + "'");
        }
    }
}
