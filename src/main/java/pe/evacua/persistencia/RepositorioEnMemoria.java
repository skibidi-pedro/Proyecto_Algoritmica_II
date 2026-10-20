package pe.evacua.persistencia;

import java.util.LinkedHashMap;
import java.util.Map;
import pe.evacua.escenario.Escenario;
import pe.evacua.escenario.EscenarioInvalidoException;
import pe.evacua.escenario.ValidadorEscenario;
import pe.evacua.estructuras.ListaSimple;

/**
 * Repositorio en memoria con las mismas reglas que el de base de datos.
 * Sirve para pruebas y para trabajar sin SQLite.
 */
public class RepositorioEnMemoria implements RepositorioEscenarios {

    private final Map<String, Escenario> escenarios = new LinkedHashMap<>();

    @Override
    public void guardar(Escenario escenario, boolean reemplazar) throws EscenarioInvalidoException, RepositorioException {
        ValidadorEscenario.exigirValido(escenario);
        if (escenarios.containsKey(escenario.nombre()) && !reemplazar) {
            throw new RepositorioException("Ya existe un escenario con el nombre \"" + escenario.nombre() + "\"");
        }
        escenarios.remove(escenario.nombre());
        escenarios.put(escenario.nombre(), escenario);
    }

    @Override
    public Escenario cargar(String nombre) throws RepositorioException {
        Escenario e = escenarios.get(nombre);
        if (e == null) {
            throw new EscenarioNoEncontradoException(nombre);
        }
        return e;
    }

    @Override
    public boolean existe(String nombre) {
        return escenarios.containsKey(nombre);
    }

    @Override
    public ListaSimple<ResumenEscenario> listar() {
        ListaSimple<ResumenEscenario> lista = new ListaSimple<>();
        for (Escenario e : escenarios.values()) {
            lista.agregarFinal(new ResumenEscenario(e.nombre(), e.edificio(), e.resultado() != null));
        }
        return lista;
    }

    @Override
    public void eliminar(String nombre) throws RepositorioException {
        if (escenarios.remove(nombre) == null) {
            throw new EscenarioNoEncontradoException(nombre);
        }
    }
}
