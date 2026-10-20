package pe.evacua.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import pe.evacua.escenario.Escenario;
import pe.evacua.escenario.EscenarioInvalidoException;
import pe.evacua.escenario.ResultadoSimulacion;
import pe.evacua.estructuras.ListaSimple;
import pe.evacua.modulos.ImportadorPlano;
import pe.evacua.modulos.MotorSimulacion;
import pe.evacua.modulos.PlanoInvalidoException;
import pe.evacua.persistencia.RepositorioEscenarios;
import pe.evacua.persistencia.RepositorioException;
import pe.evacua.persistencia.ResumenEscenario;
import pe.evacua.reportes.ComparacionException;
import pe.evacua.reportes.ComparadorEscenarios;
import pe.evacua.reportes.ExportadorReportes;
import pe.evacua.reportes.SalidaSimulacion;

/**
 * Menú de consola y punto de comunicación entre los módulos (sección 4, Integrante 1).
 * Conoce a los demás módulos solo a través de sus interfaces, de modo que cada integrante
 * pueda enchufar su parte sin tocar este archivo. Un módulo en {@code null} se informa como
 * "aún no disponible" en vez de fallar.
 */
public class MenuPrincipal {

    private final RepositorioEscenarios repositorio;
    private final ImportadorPlano importador;
    private final MotorSimulacion motor;
    private final Path carpetaReportes;
    private final BufferedReader entrada;
    private final PrintStream salida;

    private Escenario actual;
    private SalidaSimulacion ultimaSimulacion;

    public MenuPrincipal(RepositorioEscenarios repositorio, ImportadorPlano importador, MotorSimulacion motor,
                         Path carpetaReportes, InputStream entrada, PrintStream salida) {
        this.repositorio = repositorio;
        this.importador = importador;
        this.motor = motor;
        this.carpetaReportes = carpetaReportes;
        this.entrada = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8));
        this.salida = salida;
    }

    /** Muestra el menú en bucle hasta que la persona elige salir o se acaba la entrada. */
    public void ejecutar() {
        salida.println("=== Evacua: Simulador de Evacuación de Edificios ===");
        while (true) {
            mostrarMenu();
            String opcion = leer("Opción: ");
            if (opcion == null) {
                return;
            }
            try {
                switch (opcion.trim()) {
                    case "1" -> importarPlano();
                    case "2" -> verEscenarioActual();
                    case "3" -> guardarEscenario();
                    case "4" -> cargarEscenario();
                    case "5" -> listarEscenarios();
                    case "6" -> ejecutarSimulacion();
                    case "7" -> exportarReportes();
                    case "8" -> compararEscenarios();
                    case "9" -> eliminarEscenario();
                    case "0" -> {
                        salida.println("Hasta pronto.");
                        return;
                    }
                    default -> salida.println("Opción no válida.");
                }
            } catch (EntradaTerminada fin) {
                return;
            } catch (RepositorioException | IOException e) {
                salida.println("Error: " + e.getMessage());
            } catch (RuntimeException e) {
                salida.println("Error inesperado: " + e);
            }
        }
    }

    private void mostrarMenu() {
        salida.println();
        salida.println("Escenario actual: " + (actual == null ? "(ninguno)" : actual.nombre() + " [" + actual.edificio() + "]"));
        salida.println("1. Importar plano del edificio (.json, .txt, .xml)");
        salida.println("2. Ver escenario actual");
        salida.println("3. Guardar escenario actual");
        salida.println("4. Cargar escenario guardado");
        salida.println("5. Listar escenarios guardados");
        salida.println("6. Ejecutar simulación de evacuación");
        salida.println("7. Exportar reportes individuales por persona");
        salida.println("8. Comparar escenarios");
        salida.println("9. Eliminar escenario guardado");
        salida.println("0. Salir");
    }

    // ------------------------------------------------------------------ opciones

    private void importarPlano() throws IOException {
        if (importador == null) {
            salida.println("El importador de planos aún no está disponible (módulo del Integrante 2).");
            return;
        }
        String ruta = pedir("Ruta del archivo de plano: ");
        String nombre = pedir("Nombre para el escenario: ");
        try {
            actual = importador.importar(Paths.get(ruta), nombre);
            ultimaSimulacion = null;
            salida.println("Plano importado. " + resumen(actual));
        } catch (PlanoInvalidoException e) {
            salida.println("Plano rechazado; el edificio actual no se modificó.");
            for (String error : e.errores()) {
                salida.println(" - " + error);
            }
        }
    }

    private void verEscenarioActual() {
        if (!hayEscenario()) {
            return;
        }
        salida.println("Escenario \"" + actual.nombre() + "\" del edificio \"" + actual.edificio() + "\"");
        salida.println(resumen(actual));
        ResultadoSimulacion r = actual.resultado();
        if (r == null) {
            salida.println("Todavía no se ha simulado.");
        } else {
            salida.println(String.format(java.util.Locale.ROOT,
                    "Última simulación: %d evacuadas, %d no evacuadas, tiempo total %.2f, promedio %.2f",
                    r.evacuadas(), r.noEvacuadas(), r.tiempoTotal(), r.tiempoPromedio()));
        }
    }

    private void guardarEscenario() throws RepositorioException {
        if (!hayEscenario()) {
            return;
        }
        boolean reemplazar = false;
        if (repositorio.existe(actual.nombre())) {
            String respuesta = pedir("Ya existe \"" + actual.nombre() + "\". ¿Reemplazarlo? (s/n): ");
            if (!respuesta.equalsIgnoreCase("s")) {
                salida.println("No se guardó.");
                return;
            }
            reemplazar = true;
        }
        try {
            repositorio.guardar(actual, reemplazar);
            salida.println("Escenario \"" + actual.nombre() + "\" guardado.");
        } catch (EscenarioInvalidoException e) {
            salida.println("No se puede guardar: el escenario incumple las reglas.");
            for (String error : e.errores()) {
                salida.println(" - " + error);
            }
        }
    }

    private void cargarEscenario() throws RepositorioException {
        String nombre = pedir("Nombre del escenario a cargar: ");
        actual = repositorio.cargar(nombre);
        ultimaSimulacion = null;
        salida.println("Escenario cargado. " + resumen(actual));
    }

    private void listarEscenarios() throws RepositorioException {
        ListaSimple<ResumenEscenario> lista = repositorio.listar();
        if (lista.estaVacia()) {
            salida.println("No hay escenarios guardados.");
            return;
        }
        for (ResumenEscenario r : lista) {
            salida.println(" - " + r.nombre() + " [" + r.edificio() + "]" + (r.tieneResultado() ? " (con simulación)" : ""));
        }
    }

    private void ejecutarSimulacion() {
        if (motor == null) {
            salida.println("El motor de simulación aún no está disponible (módulo del Integrante 4).");
            return;
        }
        if (!hayEscenario()) {
            return;
        }
        SalidaSimulacion resultado = motor.simular(actual);
        ultimaSimulacion = resultado;
        actual = actual.conResultado(resultado.resultado());
        salida.println("Simulación terminada. Use la opción 3 para guardar el escenario con su resultado.");
        verEscenarioActual();
    }

    private void exportarReportes() throws IOException {
        if (ultimaSimulacion == null) {
            salida.println("Primero ejecute una simulación (opción 6).");
            return;
        }
        Path destino = carpetaReportes.resolve(nombreCarpeta(actual.nombre()));
        ListaSimple<Path> archivos = ExportadorReportes.exportarTodos(ultimaSimulacion.reportes(), destino);
        salida.println("Se exportaron " + archivos.tamanio() + " reporte(s) individual(es) en: " + destino);
    }

    private void compararEscenarios() throws RepositorioException {
        String nombres = pedir("Nombres de los escenarios a comparar, separados por coma: ");
        ListaSimple<Escenario> elegidos = new ListaSimple<>();
        for (String nombre : nombres.split(",")) {
            if (!nombre.isBlank()) {
                elegidos.agregarFinal(repositorio.cargar(nombre.trim()));
            }
        }
        try {
            salida.print(ComparadorEscenarios.comparar(elegidos).aTexto());
        } catch (ComparacionException e) {
            salida.println("No se puede comparar: " + e.getMessage());
        }
    }

    private void eliminarEscenario() throws RepositorioException {
        String nombre = pedir("Nombre del escenario a eliminar: ");
        String confirmacion = pedir("¿Seguro que desea eliminar \"" + nombre + "\"? (s/n): ");
        if (confirmacion.equalsIgnoreCase("s")) {
            repositorio.eliminar(nombre);
            salida.println("Escenario eliminado.");
        } else {
            salida.println("No se eliminó.");
        }
    }

    // ------------------------------------------------------------------ utilidades

    private boolean hayEscenario() {
        if (actual == null) {
            salida.println("No hay un escenario actual. Importe un plano (1) o cargue uno guardado (4).");
            return false;
        }
        return true;
    }

    private static String resumen(Escenario e) {
        return e.nodos().tamanio() + " nodos, " + e.conexiones().tamanio() + " conexiones, "
                + e.personas().tamanio() + " personas, " + e.bloqueos().tamanio() + " bloqueos.";
    }

    private static String nombreCarpeta(String nombre) {
        return nombre.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    /** Lee una línea; devuelve null si se acabó la entrada. */
    private String leer(String mensaje) {
        salida.print(mensaje);
        salida.flush();
        try {
            return entrada.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    /** Como leer(), pero corta el menú si se acabó la entrada. */
    private String pedir(String mensaje) {
        String linea = leer(mensaje);
        if (linea == null) {
            throw new EntradaTerminada();
        }
        return linea.trim();
    }

    private static final class EntradaTerminada extends RuntimeException {
        EntradaTerminada() {
            super(null, null, false, false);
        }
    }
}
