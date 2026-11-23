package evacua.personas;

public final class NivelEstres {
    public static final double MIN = 0, MAX = 100;

    public static final int DANO = 1, PELIGRO = 4, PROXIMIDAD = 1, CONGESTION = 2, BLOQUEO = 3;

    private NivelEstres() {
    }

    public static double limitar(double v) {
        return Math.max(MIN, Math.min(MAX, v));
    }

    public static boolean supera(double estres, double umbral) {
        return estres >= umbral;
    }
}
