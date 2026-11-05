package evacua.simulacion;

import evacua.edificio.Edificio;
import evacua.edificio.TipoNodo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Evento aleatorio (RN-18): colapso repentino de un pasillo. Reproducible con la semilla del Random. */
public final class EventoAleatorio {
    private EventoAleatorio() {
    }

    /** Bloquea un pasillo libre elegido al azar. Devuelve {fila, col} o null si no hay pasillos libres. */
    public static int[] colapsarPasillo(Edificio e, Random rnd) {
        List<int[]> cand = new ArrayList<>();
        for (int i = 0; i < e.filas; i++)
            for (int j = 0; j < e.cols; j++)
                if (e.n[i][j].tipo == TipoNodo.PASILLO && !e.n[i][j].bloqueado) cand.add(new int[]{i, j});
        if (cand.isEmpty()) return null;
        int[] q = cand.get(rnd.nextInt(cand.size()));
        e.n[q[0]][q[1]].bloqueado = true;
        return q;
    }
}
