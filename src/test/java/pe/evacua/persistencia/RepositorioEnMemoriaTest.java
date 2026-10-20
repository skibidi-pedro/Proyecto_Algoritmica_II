package pe.evacua.persistencia;

public class RepositorioEnMemoriaTest extends RepositorioEscenariosContrato {

    @Override
    protected RepositorioEscenarios crearRepositorio() {
        return new RepositorioEnMemoria();
    }
}
