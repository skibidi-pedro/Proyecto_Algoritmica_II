package pe.evacua.reportes;

import pe.evacua.escenario.ResultadoSimulacion;
import pe.evacua.estructuras.ListaSimple;

/** Lo que devuelve el motor de simulación: el resumen global y el reporte de cada persona. */
public record SalidaSimulacion(ResultadoSimulacion resultado, ListaSimple<ReportePersona> reportes) {
}
