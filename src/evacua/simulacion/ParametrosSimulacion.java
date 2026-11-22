package evacua.simulacion;

public class ParametrosSimulacion {
    public TipoDesastre desastre = TipoDesastre.CLASICO;
    public int intensidad = 1;
    public double prob = 0.03;
    public long semilla = 42;
    public int limite = 300;
    public double umbral = 8;
    public int capacidad = 2;
    public boolean astar = true;
    public int esperaRecalculo = 2;

    public ParametrosSimulacion copia() {
        ParametrosSimulacion p = new ParametrosSimulacion();
        p.desastre = desastre;
        p.intensidad = intensidad;
        p.prob = prob;
        p.semilla = semilla;
        p.limite = limite;
        p.umbral = umbral;
        p.capacidad = capacidad;
        p.astar = astar;
        p.esperaRecalculo = esperaRecalculo;
        return p;
    }
}
