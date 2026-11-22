package evacua.importacion;

import evacua.edificio.Conexion;
import evacua.edificio.Edificio;
import evacua.edificio.TipoNodo;
import evacua.personas.EstadoSalud;
import evacua.personas.TipoPersona;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Valida un {@link PlanoDatos} y construye el grafo del edificio. Si hay errores no construye nada (RN-15). */
public final class ConstructorEdificio {
    private ConstructorEdificio() {
    }

    public static Edificio construir(PlanoDatos d) {
        List<String> err = ValidadorPlano.validar(d);
        if (!err.isEmpty()) {
            StringBuilder sb = new StringBuilder("El plano tiene " + err.size() + " error(es):");
            for (String e : err) sb.append("\n - ").append(e);
            throw new IllegalArgumentException(sb.toString());
        }
        int maxX = 0, maxY = 0, maxP = 1;
        for (PlanoDatos.NodoDef n : d.nodos) {
            maxX = Math.max(maxX, n.x);
            maxY = Math.max(maxY, n.y);
            maxP = Math.max(maxP, n.piso);
        }
        int filasPiso = Math.max(maxY + 1, d.filas == null ? 0 : d.filas);
        Edificio e = new Edificio(filasPiso, Math.max(maxX + 1, d.columnas == null ? 0 : d.columnas), Math.max(maxP, d.pisos == null ? 1 : d.pisos));
        Map<String, int[]> pos = new HashMap<>();
        for (PlanoDatos.NodoDef n : d.nodos) {
            int fila = (n.piso - 1) * filasPiso + n.y;
            // El plano ya está validado. Detectar las regiones una vez al terminar la carga.
            e.n[fila][n.x].tipo = ValidadorPlano.tipo(n.tipo);
            e.idANodo.put(n.id, fila * e.cols + n.x);
            e.n[fila][n.x].bloqueado = n.bloqueado;
            e.n[fila][n.x].danado = n.danado;
            e.n[fila][n.x].peligroso = n.peligroso;
            e.n[fila][n.x].bloqueoReparable = n.reparable;
            pos.put(n.id, new int[]{fila, n.x});
        }
        e.conexionesExplicitas = !d.cuadricula;
        for (PlanoDatos.ConexionDef c : d.conexiones) {
            int[] a = pos.get(c.origen), b = pos.get(c.destino);
            e.conexiones.removeIf(k -> k.origen == a[0] * e.cols + a[1] && k.destino == b[0] * e.cols + b[1]);
            Conexion nk = new Conexion("C" + c.origen + "_" + c.destino, a[0] * e.cols + a[1], b[0] * e.cols + b[1], c.tiempo, c.capacidad);
            nk.condicion = evacua.edificio.Condicion.valueOf(c.condicion);
            nk.bloqueada = c.bloqueada;
            e.conexiones.add(nk);
        }
        for (PlanoDatos.PersonaDef p : d.personas) {
            int[] a = pos.get(p.nodo);
            if (!e.caminable(a[0], a[1])) throw new IllegalArgumentException("Persona en nodo bloqueado: " + p.nodo);
            e.agregar(TipoPersona.valueOf(p.tipo), EstadoSalud.valueOf(p.salud), a[0], a[1]);
        }
        evacua.edificio.Departamentos.detectar(e);
        return e;
    }
}
