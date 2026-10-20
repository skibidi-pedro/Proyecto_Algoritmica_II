package pe.evacua.persistencia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static pe.evacua.escenario.EscenariosDePrueba.basico;
import static pe.evacua.escenario.EscenariosDePrueba.resultado;

import org.junit.Test;
import pe.evacua.escenario.Escenario;
import pe.evacua.escenario.EscenarioInvalidoException;
import pe.evacua.escenario.NodoDatos;
import pe.evacua.escenario.PersonaDatos;
import pe.evacua.escenario.TipoNodo;
import pe.evacua.escenario.TipoPersona;

/**
 * Pruebas comunes que debe cumplir cualquier implementación de {@link RepositorioEscenarios}.
 * Se ejecutan sobre la versión en memoria y sobre la de SQLite.
 */
public abstract class RepositorioEscenariosContrato {

    protected abstract RepositorioEscenarios crearRepositorio() throws Exception;

    @Test
    public void guardaYCargaUnEscenarioIgual() throws Exception {
        RepositorioEscenarios repo = crearRepositorio();
        Escenario original = basico("simulacro-1", "Torre Norte");
        repo.guardar(original, false);
        Escenario cargado = repo.cargar("simulacro-1");
        assertEquals(original, cargado);
        assertNull(cargado.resultado());
    }

    @Test
    public void guardaYCargaElResultadoDeLaSimulacion() throws Exception {
        RepositorioEscenarios repo = crearRepositorio();
        Escenario conResultado = basico("simulacro-2", "Torre Norte")
                .conResultado(resultado(8, 2, 95.5, 41.25, "N2"));
        repo.guardar(conResultado, false);
        Escenario cargado = repo.cargar("simulacro-2");
        assertNotNull(cargado.resultado());
        assertEquals(conResultado.resultado(), cargado.resultado());
        assertEquals("N2", cargado.resultado().nodosMasCongestionados().obtenerPrimero().nodoId());
    }

    @Test
    public void noPermiteNombresRepetidosSalvoQueSeReemplace() throws Exception {
        RepositorioEscenarios repo = crearRepositorio();
        repo.guardar(basico("e", "Torre"), false);
        try {
            repo.guardar(basico("e", "Torre"), false);
            throw new AssertionError("debía rechazar el nombre repetido");
        } catch (RepositorioException esperado) {
            assertTrue(esperado.getMessage().contains("Ya existe"));
        }
        Escenario nuevo = basico("e", "Torre").conResultado(resultado(3, 0, 10, 3.3, "N1"));
        repo.guardar(nuevo, true);
        assertEquals(nuevo, repo.cargar("e"));
        assertEquals(1, repo.listar().tamanio());
    }

    @Test
    public void noGuardaEscenariosQueIncumplenLasReglas() throws Exception {
        RepositorioEscenarios repo = crearRepositorio();
        Escenario sinSalida = new Escenario("sin-salida", "Torre",
                pe.evacua.escenario.EscenariosDePrueba.lista(new NodoDatos("N1", "Sala", TipoNodo.HABITACION, 0, 0, 5)),
                null, null, null, null);
        try {
            repo.guardar(sinSalida, false);
            throw new AssertionError("debía rechazar un edificio sin salida");
        } catch (EscenarioInvalidoException esperado) {
            assertTrue(esperado.getMessage().contains("RN-10"));
        }
        Escenario personaPerdida = basico("persona-perdida", "Torre");
        personaPerdida.personas().agregarFinal(new PersonaDatos("PX", TipoPersona.ADULTO, "NO-EXISTE"));
        try {
            repo.guardar(personaPerdida, false);
            throw new AssertionError("debía rechazar una persona fuera del edificio");
        } catch (EscenarioInvalidoException esperado) {
            // ok
        }
        assertFalse(repo.existe("sin-salida"));
        assertFalse(repo.existe("persona-perdida"));
        assertEquals(0, repo.listar().tamanio());
    }

    @Test
    public void listaEnOrdenDeGuardadoIndicandoSiTieneResultado() throws Exception {
        RepositorioEscenarios repo = crearRepositorio();
        repo.guardar(basico("a", "Torre"), false);
        repo.guardar(basico("b", "Anexo").conResultado(resultado(1, 0, 5, 5, "N1")), false);
        ResumenEscenario primero = repo.listar().obtener(0);
        ResumenEscenario segundo = repo.listar().obtener(1);
        assertEquals("a", primero.nombre());
        assertFalse(primero.tieneResultado());
        assertEquals("b", segundo.nombre());
        assertEquals("Anexo", segundo.edificio());
        assertTrue(segundo.tieneResultado());
    }

    @Test
    public void eliminaYReportaLosInexistentes() throws Exception {
        RepositorioEscenarios repo = crearRepositorio();
        repo.guardar(basico("e", "Torre"), false);
        repo.eliminar("e");
        assertFalse(repo.existe("e"));
        try {
            repo.cargar("e");
            throw new AssertionError("debía lanzar EscenarioNoEncontradoException");
        } catch (EscenarioNoEncontradoException esperado) {
            // ok
        }
        try {
            repo.eliminar("e");
            throw new AssertionError("debía lanzar EscenarioNoEncontradoException");
        } catch (EscenarioNoEncontradoException esperado) {
            // ok
        }
    }
}
