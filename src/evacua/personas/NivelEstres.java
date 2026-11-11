package evacua.personas;

public final class NivelEstres {
    public static final double MIN = 0, MAX = 100;

    private NivelEstres() {
    }

    public static double limitar(double v) {
        return Math.max(MIN, Math.min(MAX, v));
    }

    public static boolean supera(double estres, double umbral) {
        return estres >= umbral;
    }
}
