package pe.evacua.reportes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static pe.evacua.escenario.EscenariosDePrueba.lista;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import pe.evacua.escenario.EstadoNodo;
import pe.evacua.escenario.TipoPersona;
import pe.evacua.estructuras.ListaDoble;
import pe.evacua.estructuras.ListaSimple;

public class ExportadorReportesTest {

    @Rule
    public TemporaryFolder carpeta = new TemporaryFolder();

    private static ReportePersona reporte(String id, boolean evacuada) {
        ListaDoble<RegistroBitacora> bitacora = new ListaDoble<>();
        bitacora.agregarFinal(new RegistroBitacora(0, "N1", EstadoNodo.LIBRE));
        bitacora.agregarFinal(new RegistroBitacora(3, "N2", EstadoNodo.CONGESTIONADO));
        bitacora.agregarFinal(new RegistroBitacora(7, "S1", EstadoNodo.LIBRE));
        return new ReportePersona(id, TipoPersona.NINO, evacuada, 7, 64, 1, 2, 4, bitacora);
    }

    @Test
    public void elTextoIncluyeResumenYLineaDeTiempoEnOrden() {
        String texto = ExportadorReportes.aTexto(reporte("P1", true));
        assertTrue(texto.contains("Persona P1"));
        assertTrue(texto.contains("Tipo: NINO"));
        assertTrue(texto.contains("EVACUADA en el paso 7"));
        assertTrue(texto.contains("Nivel de estrés final: 64 / 100"));
        assertTrue(texto.contains("Bloqueos sufridos: 1"));
        assertTrue(texto.contains("Recálculos de ruta: 2"));
        assertTrue(texto.contains("cuellos de botella: 4 pasos"));
        assertTrue(texto.contains("3 registros"));
        int i1 = texto.indexOf("N1");
        int i2 = texto.indexOf("N2");
        int i3 = texto.indexOf("S1");
        assertTrue(i1 > 0 && i1 < i2 && i2 < i3);
        assertTrue(texto.contains("CONGESTIONADO"));
    }

    @Test
    public void unaPersonaNoEvacuadaTambienGeneraSuArchivo() throws Exception {
        Path archivo = ExportadorReportes.exportarPersona(reporte("P2", false), carpeta.getRoot().toPath());
        assertTrue(Files.exists(archivo));
        assertEquals("persona_P2.txt", archivo.getFileName().toString());
        String contenido = Files.readString(archivo, StandardCharsets.UTF_8);
        assertTrue(contenido.contains("NO EVACUADA"));
        assertTrue(contenido.contains("N2"));
    }

    @Test
    public void exportarTodosCreaUnArchivoPorPersonaYEvitaColisiones() throws Exception {
        ListaSimple<ReportePersona> reportes = lista(reporte("P1", true), reporte("P/1", true), reporte("P3", false));
        Path destino = carpeta.getRoot().toPath().resolve("salida/sub");
        ListaSimple<Path> escritos = ExportadorReportes.exportarTodos(reportes, destino);
        assertEquals(3, escritos.tamanio());
        assertEquals("persona_P1.txt", escritos.obtener(0).getFileName().toString());
        assertEquals("persona_P_1.txt", escritos.obtener(1).getFileName().toString());
        assertEquals("persona_P3.txt", escritos.obtener(2).getFileName().toString());
        for (Path p : escritos) {
            assertTrue(Files.exists(p));
        }
        // dos ids que se vuelven iguales tras limpiar caracteres no se pisan
        ListaSimple<ReportePersona> choque = lista(reporte("A B", true), reporte("A.B", true));
        ListaSimple<Path> dos = ExportadorReportes.exportarTodos(choque, carpeta.getRoot().toPath().resolve("choque"));
        assertEquals("persona_A_B.txt", dos.obtener(0).getFileName().toString());
        assertEquals("persona_A_B_2.txt", dos.obtener(1).getFileName().toString());
    }
}
