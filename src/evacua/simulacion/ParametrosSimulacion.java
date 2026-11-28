package evacua.simulacion;

public class ParametrosSimulacion {
    public TipoDesastre desastre = TipoDesastre.NINGUNO;
    public TipoGas gas = TipoGas.MONOXIDO_CARBONO;
    public Intensidad nivel = Intensidad.BAJA;
    public boolean origenManual;
    public int pisoOrigen = -1, filaOrigen = -1, colOrigen = -1;
    public int intensidad = 1;
    public double prob = 0.03;
    public long semilla = 42;
    public int limite = 300;
    public double umbral = 8;
    public int capacidad = 2;
    public boolean astar = true;

    public ParametrosSimulacion copia() {
        ParametrosSimulacion p = new ParametrosSimulacion();
        p.desastre = desastre;
        p.nivel = nivel;
        p.gas = gas;
        p.origenManual = origenManual;
        p.pisoOrigen = pisoOrigen;
        p.filaOrigen = filaOrigen;
        p.colOrigen = colOrigen;
        p.intensidad = intensidad;
        p.prob = prob;
        p.semilla = semilla;
        p.limite = limite;
        p.umbral = umbral;
        p.capacidad = capacidad;
        p.astar = astar;
        return p;
    }
}
