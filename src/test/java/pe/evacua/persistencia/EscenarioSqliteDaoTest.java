package pe.evacua.persistencia;

import java.io.File;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/** Ejecuta el contrato sobre SQLite real (requiere el driver sqlite-jdbc que baja Maven). */
public class EscenarioSqliteDaoTest extends RepositorioEscenariosContrato {

    @Rule
    public TemporaryFolder carpeta = new TemporaryFolder();

    @Override
    protected RepositorioEscenarios crearRepositorio() throws Exception {
        File archivo = new File(carpeta.getRoot(), "prueba.db");
        return new EscenarioSqliteDao("jdbc:sqlite:" + archivo.getAbsolutePath());
    }
}
