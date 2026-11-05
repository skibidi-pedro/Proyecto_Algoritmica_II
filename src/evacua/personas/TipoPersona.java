package evacua.personas;

import java.awt.Color;

public enum TipoPersona {
    ADULTO("Adulto", 'A', 1.0, new Color(30, 100, 200)),
    NINO("Nino", 'N', 0.8, new Color(235, 140, 20)),
    MOVILIDAD("Mov. reducida", 'M', 0.5, new Color(140, 60, 160)),
    RESCATE("Rescate", 'R', 1.2, new Color(200, 30, 30));

    public final String nombre;
    public final char letra;
    public final double vel;
    public final Color color;

    TipoPersona(String nombre, char letra, double vel, Color color) {
        this.nombre = nombre;
        this.letra = letra;
        this.vel = vel;
        this.color = color;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
