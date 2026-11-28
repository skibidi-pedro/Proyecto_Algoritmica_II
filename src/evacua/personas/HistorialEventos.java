package evacua.personas;

public class HistorialEventos {
    public int bloqueosSufridos;
    public int recalculos;
    public int esperaCuelloBotella;

    public void reiniciar() {
        bloqueosSufridos = 0;
        recalculos = 0;
        esperaCuelloBotella = 0;
    }
}
