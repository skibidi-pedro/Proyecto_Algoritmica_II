package evacua.edificio;

import evacua.estructuras.Pila;

public class HistorialEdicion {
    private final Pila<Edificio> pila = new Pila<>();

    public void guardar(Edificio actual) {
        pila.apilar(actual.copia());
    }

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
