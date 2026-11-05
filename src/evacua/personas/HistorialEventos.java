package evacua.personas;

/** Contadores de eventos de una persona (RN-13). */
public class HistorialEventos {
    public int bloqueosSufridos;
    public int recalculos;
    public int esperaCuelloBotella; // ticks esperando en nodos llenos

    public void reiniciar() {
        bloqueosSufridos = 0;
        recalculos = 0;
        esperaCuelloBotella = 0;
    }
}
