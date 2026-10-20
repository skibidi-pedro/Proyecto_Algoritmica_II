package pe.evacua.modulos;

import pe.evacua.escenario.Escenario;
import pe.evacua.reportes.SalidaSimulacion;

/**
 * Contrato que implementa el Integrante 4 (motor de simulación, sección 2.5).
 * Recibe el escenario completo y devuelve el resumen global y el reporte de cada persona
 * (armado con los datos de la clase Persona del Integrante 3).
 */
public interface MotorSimulacion {

    SalidaSimulacion simular(Escenario escenario);
}
