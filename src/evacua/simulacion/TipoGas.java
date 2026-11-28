package evacua.simulacion;

/** Perfiles ficticios del simulador; las columnas corresponden a >10, ≥50, ≥75 y ≥95. */
public enum TipoGas {
    MONOXIDO_CARBONO("Monóxido de carbono (CO)", 4, new int[]{8,5,3,2}, new int[]{0,7,5,3}, new int[]{0,0,10,6},12, .90,.80,.70),
    GAS_NATURAL("Gas natural odorizado (CH₄)", 3, new int[]{1,1,1,1}, new int[4], new int[]{0,0,14,10},20, .95,.90,.85),
    DIOXIDO_CARBONO("Dióxido de carbono (CO₂)", 4, new int[]{6,4,2,1}, new int[4], new int[]{0,0,10,7},14, .90,.80,.70),
    TETRAFLUOROETANO("Tetrafluoroetano (R-134a)", 3, new int[]{7,5,3,2}, new int[]{0,8,5,3}, new int[]{0,0,12,8},16, .90,.85,.75);

    private final String nombre;
    final int estresBase, muerte;
    final int[] aviso, confusion, herida;
    private final double[] movilidad;

    TipoGas(String nombre, int estres, int[] aviso, int[] confusion, int[] herida, int muerte, double... movilidad) {
        this.nombre = nombre;
        estresBase = estres;
        this.aviso = aviso;
        this.confusion = confusion;
        this.herida = herida;
        this.muerte = muerte;
        this.movilidad = movilidad;
    }
    public double velocidad(double gas) {
        return gas < 50 ? 1 : movilidad[gas < 75 ? 0 : gas < 95 ? 1 : 2];
    }
    static boolean alcanza(int[] tiempos, int[] limites) {
        for (int i = 0; i < 4; i++) if (limites[i] > 0 && tiempos[i] >= limites[i]) return true;
        return false;
    }
    @Override public String toString() { return nombre; }
}
