package evacua.edificio;

public class Nodo {
    public TipoNodo tipo = TipoNodo.VACIO;
    public int departamento;
    public boolean danado;
    public boolean peligroso;
    public boolean bloqueoReparable;

    public int capacidadEfectiva(int base) {
        return bloqueado ? 0 : Math.max(1, base - (danado ? 1 : 0));
    }

    public int pesoEntrada() {
        return (danado ? 2 : 1) * (peligroso ? 5 : 1);
    }
    public boolean bloqueado;
    public double ocupacion;
    public int visitas;
    public int esperas;
}
