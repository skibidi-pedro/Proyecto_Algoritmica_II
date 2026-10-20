package pe.evacua;

import java.nio.file.Paths;
import pe.evacua.modulos.ImportadorPlano;
import pe.evacua.modulos.MotorSimulacion;
import pe.evacua.persistencia.EscenarioSqliteDao;
import pe.evacua.persistencia.RepositorioEnMemoria;
import pe.evacua.persistencia.RepositorioEscenarios;
import pe.evacua.persistencia.RepositorioException;
import pe.evacua.ui.MenuPrincipal;

/**
 * Punto de entrada. Aquí se "enchufan" los módulos de cada integrante:
 * cuando existan sus implementaciones, basta con crearlas en las líneas marcadas.
 */
public class Main {

    private static final String BASE_DE_DATOS = "jdbc:sqlite:evacua.db";
    private static final String CARPETA_REPORTES = "reportes_salida";

    public static void main(String[] args) {
        RepositorioEscenarios repositorio = abrirRepositorio();

        // Integrante 2: sustituir null por la implementación del parser de planos.
        ImportadorPlano importador = null;
        // Integrante 4: sustituir null por la implementación del motor de simulación.
        MotorSimulacion motor = null;

        new MenuPrincipal(repositorio, importador, motor, Paths.get(CARPETA_REPORTES), System.in, System.out).ejecutar();
    }

    private static RepositorioEscenarios abrirRepositorio() {
        try {
            return new EscenarioSqliteDao(BASE_DE_DATOS);
        } catch (RepositorioException e) {
            String causa = e.getCause() != null ? ": " + e.getCause().getMessage() : "";
            System.out.println("Aviso: no se pudo abrir la base de datos (" + e.getMessage() + causa
                    + "). Los escenarios solo se guardarán en memoria durante esta sesión.");
            return new RepositorioEnMemoria();
        }
    }
}
