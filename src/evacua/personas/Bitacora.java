package evacua.personas;

import evacua.edificio.EstadoNodo;
import evacua.estructuras.ListaDoble;

import java.util.Iterator;

/** Bitacora de recorrido de una persona, sobre lista doble. Solo se agrega (RN-11). */
public class Bitacora implements Iterable<RegistroBitacora> {
    private final ListaDoble<RegistroBitacora> registros = new ListaDoble<>();

    public void registrar(int paso, int fila, int col, EstadoNodo estado) {
        registros.agregarFinal(new RegistroBitacora(paso, fila, col, estado));
    }

    public int tamano() {
        return registros.tamano();
    }

    public RegistroBitacora ultimo() {
        return registros.ultimo();
    }

    /** Solo se usa al iniciar una simulacion nueva; los registros nunca se editan. */
    public void reiniciar() {
        registros.limpiar();
    }

    @Override
    public Iterator<RegistroBitacora> iterator() {
        return registros.iterator();
    }

    public Iterable<RegistroBitacora> alReves() {
        return registros.alReves();
    }
}
