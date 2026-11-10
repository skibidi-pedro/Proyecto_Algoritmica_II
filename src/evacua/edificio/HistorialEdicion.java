package evacua.edificio;

import evacua.estructuras.Pila;

/** Historial para "Deshacer" en el editor: una pila con una copia del edificio antes de cada cambio. */
public class HistorialEdicion {
    private final Pila<Edificio> pila = new Pila<>();

    /** Guarda el estado actual del edificio (antes de modificarlo). */
    public void guardar(Edificio actual) {
        pila.apilar(actual.copia());
    }

    /** Devuelve el estado anterior y lo saca de la pila, o null si no hay nada que deshacer. */
    public Edificio deshacer() {
        return pila.estaVacia() ? null : pila.desapilar();
    }

    public boolean puedeDeshacer() {
        return !pila.estaVacia();
    }

    public int tamano() {
        return pila.tamano();
    }
}
