package pe.evacua.escenario;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static pe.evacua.escenario.EscenariosDePrueba.basico;
import static pe.evacua.escenario.EscenariosDePrueba.lista;

import org.junit.Test;
import pe.evacua.estructuras.ListaSimple;

public class ValidadorEscenarioTest {

    private static boolean hayError(ListaSimple<String> errores, String fragmento) {
        for (String e : errores) {
            if (e.contains(fragmento)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void escenarioBasicoEsValido() {
        assertTrue(ValidadorEscenario.validar(basico("e1", "Torre")).estaVacia());
    }

    @Test
    public void rechazaNodosRepetidos() {
        Escenario base = basico("e1", "Torre");
        ListaSimple<NodoDatos> nodos = base.nodos();
        nodos.agregarFinal(new NodoDatos("N1", "Otra", TipoNodo.HABITACION, 1, 1, 5));
        ListaSimple<String> errores = ValidadorEscenario.validar(base);
        assertTrue(hayError(errores, "RN-01"));
        assertTrue(hayError(errores, "N1"));
    }

    @Test
    public void rechazaConexionesYPersonasRepetidas() {
        Escenario base = basico("e1", "Torre");
        base.conexiones().agregarFinal(new ConexionDatos("C1", "N1", "S1", 1, 1));
        base.personas().agregarFinal(new PersonaDatos("P1", TipoPersona.ADULTO, "N1"));
        ListaSimple<String> errores = ValidadorEscenario.validar(base);
        assertTrue(hayError(errores, "conexión repetido"));
        assertTrue(hayError(errores, "persona repetido"));
    }

    @Test
    public void exigeAlMenosUnaSalida() {
        Escenario e = new Escenario("e1", "Torre",
                lista(new NodoDatos("N1", "Sala", TipoNodo.HABITACION, 0, 0, 5)),
                null, null, null, null);
        assertTrue(hayError(ValidadorEscenario.validar(e), "al menos una salida"));
    }

    @Test
    public void rechazaConexionesInvalidas() {
        Escenario base = basico("e1", "Torre");
        base.conexiones().agregarFinal(new ConexionDatos("C3", "N1", "N2", 0, 4));
        base.conexiones().agregarFinal(new ConexionDatos("C4", "N1", "N2", 1, 0));
        base.conexiones().agregarFinal(new ConexionDatos("C5", "N1", "NX", 1, 1));
        ListaSimple<String> errores = ValidadorEscenario.validar(base);
        assertTrue(hayError(errores, "C3 debe tener tiempo"));
        assertTrue(hayError(errores, "C4 debe tener una capacidad"));
        assertTrue(hayError(errores, "NX"));
    }

    @Test
    public void rechazaPersonaEnNodoInexistente() {
        Escenario base = basico("e1", "Torre");
        base.personas().agregarFinal(new PersonaDatos("P9", TipoPersona.ADULTO, "NOEXISTE"));
        assertTrue(hayError(ValidadorEscenario.validar(base), "RN-10"));
    }

    @Test
    public void rechazaBloqueoSobreElementoInexistente() {
        Escenario base = basico("e1", "Torre");
        base.bloqueos().agregarFinal(new BloqueoDatos(TipoElemento.NODO, "ZZ"));
        assertTrue(hayError(ValidadorEscenario.validar(base), "nodo inexistente"));
    }

    @Test
    public void exigirValidoLanzaConTodosLosErrores() {
        Escenario e = new Escenario("", "", null, null, null, null, null);
        try {
            ValidadorEscenario.exigirValido(e);
            throw new AssertionError("debía lanzar EscenarioInvalidoException");
        } catch (EscenarioInvalidoException ex) {
            assertTrue(ex.errores().tamanio() >= 3);
            assertTrue(ex.getMessage().contains("nombre"));
        }
    }

    @Test
    public void escenarioNuloEsInvalido() {
        assertEquals(1, ValidadorEscenario.validar(null).tamanio());
    }
}
