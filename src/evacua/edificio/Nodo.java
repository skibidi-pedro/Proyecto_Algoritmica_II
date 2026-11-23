package evacua.edificio;

public class Nodo {
    public TipoNodo tipo = TipoNodo.VACIO;
    public int departamento;
    public boolean fuegoActivo;
    private double gas;
    public double concentracionGas() { return gas; }
    public void fijarGas(double porcentaje) {
        if (!Double.isFinite(porcentaje) || porcentaje < 0 || porcentaje > 100)
            throw new IllegalArgumentException("El gas debe estar entre 0 y 100 %.");
        gas = porcentaje;
    }
    public boolean danado;
    public boolean peligroso;
    public boolean bloqueoReparable;

    public int capacidadEfectiva(int base) {
        return capacidadEfectiva(base, false);
    }

    public boolean accesoRescate() {
        return tipo == TipoNodo.SALIDA || tipo == TipoNodo.ESCALERA;
    }

    public int capacidadEfectiva(int base, boolean pasoEspecial) {
        return bloqueado && !(pasoEspecial && accesoRescate()) ? 0 : Math.max(1, base - (danado ? 1 : 0));
    }

    public int pesoEntrada() {
        return (danado ? 2 : 1) * (peligroso ? 5 : 1);
    }
    public boolean bloqueado;
    public double ocupacion;
    public int visitas;
    public int esperas;
}
