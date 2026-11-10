package evacua.simulacion;


public class ParametrosSimulacion {
    public double prob = 0.03;   // probabilidad de evento aleatorio por tick
    public long semilla = 42;
    public int limite = 300;     // tiempo limite (ticks)
    public double umbral = 8;    // umbral de estres
    public int capacidad = 2;    // personas por nodo
    public boolean astar = true;
    public int esperaRecalculo = 2;  // ticks esperando un nodo lleno antes de buscar otro camino

    public ParametrosSimulacion copia() {
        ParametrosSimulacion p = new ParametrosSimulacion();
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
