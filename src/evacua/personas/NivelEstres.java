package evacua.personas;

/** Estres de una persona: valor entre 0 y 100 (RN-12). */
public final class NivelEstres {
    public static final double MIN = 0, MAX = 100;

    private NivelEstres() {
    }

    public static double limitar(double v) {
        return Math.max(MIN, Math.min(MAX, v));
    }

    /** Si el estres alcanza el umbral, la persona se mueve mas lento. */
    public static boolean supera(double estres, double umbral) {
        return estres >= umbral;
    }
}
