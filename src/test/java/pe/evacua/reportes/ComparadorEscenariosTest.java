package pe.evacua.reportes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static pe.evacua.escenario.EscenariosDePrueba.basico;
import static pe.evacua.escenario.EscenariosDePrueba.lista;
import static pe.evacua.escenario.EscenariosDePrueba.resultado;

import org.junit.Test;
import pe.evacua.escenario.Escenario;

public class ComparadorEscenariosTest {

    @Test
    public void eligeElQueEvacuaMasYEnEmpateElMasRapido() throws Exception {
        Escenario a = basico("A", "Torre").conResultado(resultado(8, 2, 100, 40, "N1"));
        Escenario b = basico("B", "Torre").conResultado(resultado(10, 0, 120, 50, "N2"));
        Escenario c = basico("C", "Torre").conResultado(resultado(10, 0, 90, 35, "S1"));
        ComparacionEscenarios comparacion = ComparadorEscenarios.comparar(lista(a, b, c));
        assertEquals("C", comparacion.mejorEscenario());
        assertEquals(3, comparacion.filas().tamanio());
        assertEquals("Torre", comparacion.edificio());
        assertEquals("N1", comparacion.filas().obtener(0).nodoMasCongestionado());
    }

    @Test
    public void elTextoMarcaAlMejor() throws Exception {
        Escenario a = basico("A", "Torre").conResultado(resultado(5, 5, 100, 40, "N1"));
        Escenario b = basico("B", "Torre").conResultado(resultado(9, 1, 80, 30, "N1"));
        String texto = ComparadorEscenarios.comparar(lista(a, b)).aTexto();
        assertTrue(texto.contains("Edificio: Torre"));
        assertTrue(texto.contains("Mejor escenario: B"));
        assertTrue(texto.contains("100.00"));
    }

    private static void esperarError(Escenario[] escenarios, String fragmento) {
        try {
            ComparadorEscenarios.comparar(lista(escenarios));
            throw new AssertionError("debía lanzar ComparacionException");
        } catch (ComparacionException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(fragmento));
        }
    }

    @Test
    public void rechazaEdificiosDistintos() {
        Escenario a = basico("A", "Torre").conResultado(resultado(5, 5, 100, 40, "N1"));
        Escenario b = basico("B", "Anexo").conResultado(resultado(9, 1, 80, 30, "N1"));
        esperarError(new Escenario[] {a, b}, "mismo edificio");
    }

    @Test
    public void rechazaEscenariosSinSimular() {
        Escenario a = basico("A", "Torre").conResultado(resultado(5, 5, 100, 40, "N1"));
        Escenario b = basico("B", "Torre");
        esperarError(new Escenario[] {a, b}, "todavía no tiene una simulación");
    }

    @Test
    public void exigeAlMenosDos() {
        esperarError(new Escenario[] {basico("A", "Torre")}, "al menos dos");
    }
}
