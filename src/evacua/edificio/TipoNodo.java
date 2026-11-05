package evacua.edificio;

import java.awt.Color;

public enum TipoNodo {
    VACIO('.', "Vacio", Color.WHITE),
    HABITACION('H', "Habitacion", new Color(173, 216, 230)),
    PASILLO('P', "Pasillo", new Color(240, 235, 190)),
    ESCALERA('E', "Escalera", new Color(221, 190, 235)),
    SALIDA('S', "Salida", new Color(144, 238, 144));

    public final char letra;
    public final String nombre;
    public final Color color;

    TipoNodo(char letra, String nombre, Color color) {
        this.letra = letra;
        this.nombre = nombre;
        this.color = color;
    }

    public static TipoNodo de(char ch) {
        char u = Character.toUpperCase(ch);
        for (TipoNodo t : values()) {
            if (t.letra == u) return t;
        }
        return null;
    }
}
