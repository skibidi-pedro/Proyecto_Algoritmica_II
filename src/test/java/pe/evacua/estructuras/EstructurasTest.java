package pe.evacua.estructuras;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import org.junit.Test;

public class EstructurasTest {

    private static <T> List<T> aJava(Iterable<T> it) {
        List<T> out = new ArrayList<>();
        for (T t : it) {
            out.add(t);
        }
        return out;
    }

    // ---------- ListaSimple ----------

    @Test
    public void listaSimpleAgregaYRecorreEnOrden() {
        ListaSimple<Integer> l = new ListaSimple<>();
        l.agregarFinal(2);
        l.agregarFinal(3);
        l.agregarInicio(1);
        assertEquals(List.of(1, 2, 3), aJava(l));
        assertEquals(3, l.tamanio());
        assertEquals(Integer.valueOf(1), l.obtenerPrimero());
        assertEquals(Integer.valueOf(3), l.obtenerUltimo());
        assertEquals(Integer.valueOf(2), l.obtener(1));
    }

    @Test
    public void listaSimpleEliminaCabezaMedioYCola() {
        ListaSimple<Integer> l = new ListaSimple<>();
        for (int i = 1; i <= 5; i++) {
            l.agregarFinal(i);
        }
        assertTrue(l.eliminar(1));
        assertTrue(l.eliminar(3));
        assertTrue(l.eliminar(5));
        assertFalse(l.eliminar(99));
        assertEquals(List.of(2, 4), aJava(l));
        // la cola debe haberse actualizado: agregar al final sigue funcionando
        l.agregarFinal(6);
        assertEquals(List.of(2, 4, 6), aJava(l));
        assertEquals(Integer.valueOf(6), l.obtenerUltimo());
    }

    @Test
    public void listaSimpleVaciaLanzaExcepciones() {
        ListaSimple<String> l = new ListaSimple<>();
        assertTrue(l.estaVacia());
        try {
            l.eliminarPrimero();
            throw new AssertionError("debía lanzar NoSuchElementException");
        } catch (NoSuchElementException esperado) {
            // ok
        }
        try {
            l.obtener(0);
            throw new AssertionError("debía lanzar IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException esperado) {
            // ok
        }
    }

    @Test
    public void listaSimpleVaciarYReutilizar() {
        ListaSimple<Integer> l = new ListaSimple<>();
        l.agregarFinal(1);
        l.eliminarPrimero();
        assertTrue(l.estaVacia());
        l.agregarFinal(7);
        assertEquals(Integer.valueOf(7), l.obtenerUltimo());
        l.limpiar();
        assertEquals(0, l.tamanio());
    }

    // ---------- ListaDoble ----------

    @Test
    public void listaDobleRecorreEnAmbosSentidos() {
        ListaDoble<String> l = new ListaDoble<>();
        l.agregarFinal("b");
        l.agregarFinal("c");
        l.agregarInicio("a");
        assertEquals(List.of("a", "b", "c"), aJava(l));
        assertEquals(List.of("c", "b", "a"), aJava(l.enOrdenInverso()));
        assertEquals("a", l.obtener(0));
        assertEquals("b", l.obtener(1));
        assertEquals("c", l.obtener(2));
    }

    @Test
    public void listaDobleEliminaExtremosYMedio() {
        ListaDoble<Integer> l = new ListaDoble<>();
        for (int i = 1; i <= 5; i++) {
            l.agregarFinal(i);
        }
        assertEquals(Integer.valueOf(1), l.eliminarPrimero());
        assertEquals(Integer.valueOf(5), l.eliminarUltimo());
        assertTrue(l.eliminar(3));
        assertEquals(List.of(2, 4), aJava(l));
        assertEquals(List.of(4, 2), aJava(l.enOrdenInverso()));
        assertEquals(2, l.tamanio());
        l.eliminarPrimero();
        l.eliminarPrimero();
        assertTrue(l.estaVacia());
        l.agregarFinal(9);
        assertEquals(Integer.valueOf(9), l.obtenerPrimero());
        assertEquals(Integer.valueOf(9), l.obtenerUltimo());
    }

    @Test
    public void listaDobleObtenerPorIndiceEnListaGrande() {
        ListaDoble<Integer> l = new ListaDoble<>();
        for (int i = 0; i < 101; i++) {
            l.agregarFinal(i);
        }
        for (int i = 0; i < 101; i++) {
            assertEquals(Integer.valueOf(i), l.obtener(i));
        }
    }

    // ---------- Pila ----------

    @Test
    public void pilaEsLifo() {
        Pila<Integer> p = new Pila<>();
        p.apilar(1);
        p.apilar(2);
        p.apilar(3);
        assertEquals(Integer.valueOf(3), p.cima());
        assertEquals(Integer.valueOf(3), p.desapilar());
        assertEquals(Integer.valueOf(2), p.desapilar());
        assertEquals(Integer.valueOf(1), p.desapilar());
        assertTrue(p.estaVacia());
    }

    @Test(expected = NoSuchElementException.class)
    public void pilaVaciaLanzaExcepcion() {
        new Pila<Integer>().desapilar();
    }

    // ---------- Cola ----------

    @Test
    public void colaEsFifo() {
        Cola<String> c = new Cola<>();
        c.encolar("a");
        c.encolar("b");
        c.encolar("c");
        assertEquals("a", c.frente());
        assertEquals("a", c.desencolar());
        assertEquals("b", c.desencolar());
        c.encolar("d");
        assertEquals("c", c.desencolar());
        assertEquals("d", c.desencolar());
        assertTrue(c.estaVacia());
    }

    @Test(expected = NoSuchElementException.class)
    public void colaVaciaLanzaExcepcion() {
        new Cola<Integer>().desencolar();
    }

    // ---------- ColaPrioridad ----------

    @Test
    public void colaPrioridadSacaEnOrdenAscendente() {
        ColaPrioridad<Integer> cp = ColaPrioridad.ordenNatural();
        int[] datos = {5, 1, 9, 3, 7, 2, 8, 2};
        for (int d : datos) {
            cp.insertar(d);
        }
        assertEquals(Integer.valueOf(1), cp.verMinimo());
        List<Integer> salida = new ArrayList<>();
        while (!cp.estaVacia()) {
            salida.add(cp.extraerMinimo());
        }
        assertEquals(List.of(1, 2, 2, 3, 5, 7, 8, 9), salida);
    }

    @Test
    public void colaPrioridadConComparadorYCrecimiento() {
        // Orden descendente con más elementos que la capacidad inicial (fuerza el crecimiento).
        ColaPrioridad<Integer> cp = new ColaPrioridad<>((a, b) -> Integer.compare(b, a));
        Random azar = new Random(42);
        List<Integer> esperado = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            int v = azar.nextInt(1000);
            cp.insertar(v);
            esperado.add(v);
        }
        esperado.sort((a, b) -> Integer.compare(b, a));
        List<Integer> salida = new ArrayList<>();
        while (!cp.estaVacia()) {
            salida.add(cp.extraerMinimo());
        }
        assertEquals(esperado, salida);
    }

    @Test(expected = NoSuchElementException.class)
    public void colaPrioridadVaciaLanzaExcepcion() {
        ColaPrioridad.<Integer>ordenNatural().extraerMinimo();
    }
}
