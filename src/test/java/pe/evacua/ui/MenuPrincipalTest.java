package pe.evacua.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static pe.evacua.escenario.EscenariosDePrueba.basico;
import static pe.evacua.escenario.EscenariosDePrueba.lista;
import static pe.evacua.escenario.EscenariosDePrueba.resultado;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import pe.evacua.escenario.Escenario;
import pe.evacua.escenario.EstadoNodo;
import pe.evacua.escenario.TipoPersona;
import pe.evacua.estructuras.ListaDoble;
import pe.evacua.modulos.ImportadorPlano;
import pe.evacua.modulos.MotorSimulacion;
import pe.evacua.modulos.PlanoInvalidoException;
import pe.evacua.persistencia.RepositorioEnMemoria;
import pe.evacua.reportes.RegistroBitacora;
import pe.evacua.reportes.ReportePersona;
import pe.evacua.reportes.SalidaSimulacion;

public class MenuPrincipalTest {

    @Rule
    public TemporaryFolder carpeta = new TemporaryFolder();

    private String correr(RepositorioEnMemoria repo, ImportadorPlano imp, MotorSimulacion motor, String... lineas) {
        String guion = String.join("\n", lineas) + "\n";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        new MenuPrincipal(repo, imp, motor, carpeta.getRoot().toPath().resolve("reportes"),
                new ByteArrayInputStream(guion.getBytes(StandardCharsets.UTF_8)), out).ejecutar();
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private static final ImportadorPlano IMPORTADOR_FALSO = (archivo, nombre) -> {
        if (archivo.toString().contains("malo")) {
            throw new PlanoInvalidoException(lista("RN-15: falta al menos una salida"));
        }
        return basico(nombre, "Torre");
    };

    private static final MotorSimulacion MOTOR_FALSO = escenario -> {
        ListaDoble<RegistroBitacora> bitacora = new ListaDoble<>();
        bitacora.agregarFinal(new RegistroBitacora(0, "N1", EstadoNodo.LIBRE));
        ReportePersona p1 = new ReportePersona("P1", TipoPersona.ADULTO, true, 5, 10, 0, 0, 0, bitacora);
        ReportePersona p2 = new ReportePersona("P2", TipoPersona.NINO, false, 0, 90, 2, 3, 6, bitacora);
        return new SalidaSimulacion(resultado(1, 1, 50, 25, "N1"), lista(p1, p2));
    };

    @Test
    public void salirConCero() {
        String salida = correr(new RepositorioEnMemoria(), null, null, "0");
        assertTrue(salida.contains("Hasta pronto"));
    }

    @Test
    public void terminaSiSeAcabaLaEntrada() {
        String salida = correr(new RepositorioEnMemoria(), null, null);
        assertTrue(salida.contains("Opción:"));
    }

    @Test
    public void informaCuandoUnModuloNoEstaDisponible() {
        String salida = correr(new RepositorioEnMemoria(), null, null, "1", "6", "0");
        assertTrue(salida.contains("importador de planos aún no está disponible"));
        assertTrue(salida.contains("motor de simulación aún no está disponible"));
    }

    @Test
    public void importaGuardaYCargaUnEscenario() throws Exception {
        RepositorioEnMemoria repo = new RepositorioEnMemoria();
        String salida = correr(repo, IMPORTADOR_FALSO, null,
                "1", "plano.json", "torre-base",
                "3",
                "5",
                "0");
        assertTrue(salida.contains("Plano importado. 3 nodos, 2 conexiones, 3 personas, 1 bloqueos."));
        assertTrue(salida.contains("Escenario \"torre-base\" guardado."));
        assertTrue(salida.contains(" - torre-base [Torre]"));
        assertTrue(repo.existe("torre-base"));
    }

    @Test
    public void unPlanoInvalidoSeRechazaSinCambiarElEscenarioActual() {
        String salida = correr(new RepositorioEnMemoria(), IMPORTADOR_FALSO, null,
                "1", "plano.json", "bueno",
                "1", "malo.json", "otro",
                "2",
                "0");
        assertTrue(salida.contains("Plano rechazado"));
        assertTrue(salida.contains("falta al menos una salida"));
        assertTrue(salida.contains("Escenario \"bueno\" del edificio \"Torre\""));
        assertFalse(salida.contains("Escenario \"otro\""));
    }

    @Test
    public void guardarPidePermisoParaReemplazar() throws Exception {
        RepositorioEnMemoria repo = new RepositorioEnMemoria();
        String salida = correr(repo, IMPORTADOR_FALSO, null,
                "1", "plano.json", "e1",
                "3",
                "3", "n",
                "3", "s",
                "0");
        assertTrue(salida.contains("¿Reemplazarlo?"));
        assertTrue(salida.contains("No se guardó."));
        assertEquals(1, repo.listar().tamanio());
    }

    @Test
    public void simulaGuardaConResultadoYCompara() throws Exception {
        RepositorioEnMemoria repo = new RepositorioEnMemoria();
        repo.guardar(basico("previo", "Torre").conResultado(resultado(2, 0, 70, 35, "N2")), false);
        String salida = correr(repo, IMPORTADOR_FALSO, MOTOR_FALSO,
                "1", "plano.json", "nuevo",
                "6",
                "3",
                "8", "previo, nuevo",
                "0");
        assertTrue(salida.contains("Simulación terminada"));
        assertTrue(salida.contains("1 evacuadas, 1 no evacuadas"));
        assertTrue(repo.cargar("nuevo").resultado() != null);
        assertTrue(salida.contains("COMPARACIÓN DE ESCENARIOS - Edificio: Torre"));
        assertTrue(salida.contains("Mejor escenario: previo"));
    }

    @Test
    public void exportaUnArchivoPorPersonaTrasSimular() throws Exception {
        String salida = correr(new RepositorioEnMemoria(), IMPORTADOR_FALSO, MOTOR_FALSO,
                "7",
                "1", "plano.json", "torre",
                "6",
                "7",
                "0");
        assertTrue(salida.contains("Primero ejecute una simulación"));
        assertTrue(salida.contains("Se exportaron 2 reporte(s) individual(es)"));
        Path carpetaEscenario = carpeta.getRoot().toPath().resolve("reportes").resolve("torre");
        assertTrue(Files.exists(carpetaEscenario.resolve("persona_P1.txt")));
        assertTrue(Files.readString(carpetaEscenario.resolve("persona_P2.txt")).contains("NO EVACUADA"));
    }

    @Test
    public void compararMuestraErroresSinCaerse() throws Exception {
        RepositorioEnMemoria repo = new RepositorioEnMemoria();
        repo.guardar(basico("a", "Torre"), false);
        repo.guardar(basico("b", "Torre"), false);
        String salida = correr(repo, null, null,
                "8", "a,b",
                "8", "a,fantasma",
                "0");
        assertTrue(salida.contains("todavía no tiene una simulación"));
        assertTrue(salida.contains("No existe un escenario guardado con el nombre \"fantasma\""));
    }

    @Test
    public void eliminaConConfirmacion() throws Exception {
        RepositorioEnMemoria repo = new RepositorioEnMemoria();
        repo.guardar(basico("a", "Torre"), false);
        correr(repo, null, null, "9", "a", "n", "0");
        assertTrue(repo.existe("a"));
        correr(repo, null, null, "9", "a", "s", "0");
        assertFalse(repo.existe("a"));
    }

    @Test
    public void unaOpcionInvalidaNoRompeElMenu() {
        String salida = correr(new RepositorioEnMemoria(), null, null, "xyz", "2", "0");
        assertTrue(salida.contains("Opción no válida."));
        assertTrue(salida.contains("No hay un escenario actual"));
        assertTrue(salida.contains("Hasta pronto"));
    }
}
