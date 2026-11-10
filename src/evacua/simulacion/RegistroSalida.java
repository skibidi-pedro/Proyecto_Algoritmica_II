package evacua.simulacion;

import evacua.personas.Persona;

/** Datos de una persona en el instante en que sale del edificio. Inmutable. */
public final class RegistroSalida {
    public final int orden;          // posicion en la cola de salidas (1 = la primera en salir)
    public final int id;
    public final String tipo, salud;
    public final int tiempo;         // paso de simulacion en que salio
    public final double estres;      // estres al salir (0 a 100)
    public final boolean asistida;   // fue asistida por personal de rescate
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
