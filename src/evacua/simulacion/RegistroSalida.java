package evacua.simulacion;

import evacua.personas.Persona;

public final class RegistroSalida {
    public final int orden;
    public final int id;
    public final String tipo, salud;
    public final int tiempo;
    public final double estres;
    public final boolean asistida;
    public final int pasos, recalculos;

    public RegistroSalida(int orden, Persona p, int tiempo) {
        this.orden = orden;
        this.id = p.id;
        this.tipo = p.tipo.name();
        this.salud = p.salud.name();
        this.tiempo = tiempo;
        this.estres = p.estres;
        this.asistida = p.asistida;
        this.pasos = p.pasos;
        this.recalculos = p.historial.recalculos;
    }
}
