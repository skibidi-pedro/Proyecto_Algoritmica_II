package evacua.simulacion;

import evacua.edificio.Grafo;
import java.util.function.IntPredicate;

/** Una pasada calcula demanda por extremo; la segunda aplica flujos proporcionales simultáneos. */
final class Difusion {
    static void aplicar(Grafo grafo, double[] concentracion, IntPredicate permeable) {
        aplicar(grafo, concentracion, permeable, (desde, hacia) -> true);
    }

    static boolean permiteEntrada(evacua.edificio.Nodo destino, double concentracionEmisora) {
        return destino.tipo != evacua.edificio.TipoNodo.PUERTA || destino.danado || concentracionEmisora >= 60;
    }

    interface Paso { boolean permite(int desde, int hacia); }

    static double ventilar(evacua.edificio.Nodo n, double cantidad) {
        return n.tipo == evacua.edificio.TipoNodo.SALIDA && !n.bloqueado ? Math.max(0, cantidad - 5) : cantidad;
    }

    static void aplicar(Grafo grafo, double[] concentracion, IntPredicate permeable, Paso paso) {
        int n = concentracion.length;
        double[] entrada = new double[n], salida = new double[n], cambio = new double[n];
        for (int fase = 0; fase < 2; fase++) {
            for (int i = 0; i < n; i++) {
                if (!permeable.test(i)) continue;
                for (Grafo.Arista a : grafo.ady[i]) {
                    int j = a.destino;
                    if (j <= i || a.bloqueada() || !permeable.test(j) || concentracion[i] == concentracion[j]) continue;
                    int desde = concentracion[i] > concentracion[j] ? i : j;
                    int hacia = desde == i ? j : i;
                    if (!paso.permite(desde, hacia)) continue;
                    double flujo = (concentracion[desde] - concentracion[hacia]) * 0.25;
                    if (fase == 0) {
                        salida[desde] += flujo;
                        entrada[hacia] += flujo;
                    } else {
                        double escala = Math.min(1, Math.min(concentracion[desde] / salida[desde],
                                (100 - concentracion[hacia]) / entrada[hacia]));
                        cambio[desde] -= escala * flujo;
                        cambio[hacia] += escala * flujo;
                    }
                }
            }
        }
        for (int i = 0; i < n; i++) concentracion[i] = Math.max(0, Math.min(100, concentracion[i] + cambio[i]));
    }
    private Difusion() {}
}
