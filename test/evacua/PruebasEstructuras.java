package evacua;

import evacua.estructuras.*;

import java.util.*;

public class PruebasEstructuras {
    static int fallos = 0;

    static void chk(boolean ok, String msg) {
        if (!ok) {
            fallos++;
            System.out.println("FALLO: " + msg);
        }
    }

    public static void main(String[] args) {
        ListaSimple<Integer> ls = new ListaSimple<>();
        for (int i = 1; i <= 5; i++) ls.agregarFinal(i);
        ls.agregarInicio(0);
        chk(ls.tamano() == 6 && ls.obtener(0) == 0 && ls.obtener(5) == 5, "ListaSimple agregar");
        chk(ls.eliminar(3) && !ls.contiene(3) && ls.tamano() == 5, "ListaSimple eliminar");
        chk(ls.eliminar(5) && ls.obtener(ls.tamano() - 1) == 4, "ListaSimple eliminar cola");
        ls.agregarFinal(9);
        chk(ls.obtener(ls.tamano() - 1) == 9, "ListaSimple cola tras eliminar");

        ListaDoble<Integer> ld = new ListaDoble<>();
        for (int i = 0; i < 6; i++) ld.agregarFinal(i);
        ld.agregarInicio(-1);
        chk(ld.primero() == -1 && ld.ultimo() == 5 && ld.tamano() == 7, "ListaDoble agregar");
        chk(ld.obtener(1) == 0 && ld.obtener(5) == 4, "ListaDoble obtener");
        StringBuilder sb = new StringBuilder();
        for (int x : ld.alReves()) sb.append(x).append(',');
        chk(sb.toString().equals("5,4,3,2,1,0,-1,"), "ListaDoble alReves: " + sb);
        chk(ld.quitarFinal() == 5 && ld.quitarInicio() == -1 && ld.tamano() == 5, "ListaDoble quitar");

        Pila<String> p = new Pila<>();
        p.apilar("a");
        p.apilar("b");
        chk(p.cima().equals("b") && p.desapilar().equals("b") && p.desapilar().equals("a") && p.estaVacia(), "Pila LIFO");
        Cola<String> c = new Cola<>();
        c.encolar("a");
        c.encolar("b");
        chk(c.frente().equals("a") && c.desencolar().equals("a") && c.desencolar().equals("b") && c.estaVacia(), "Cola FIFO");

        ColaPrioridad<Integer> h = new ColaPrioridad<>(Comparator.naturalOrder());
        Random r = new Random(1);
        List<Integer> ref = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            int v = r.nextInt(500);
            h.agregar(v);
            ref.add(v);
        }
        Collections.sort(ref);
        boolean ok = true;
        for (int v : ref) ok &= h.sacar() == v;
        chk(ok && h.estaVacia(), "ColaPrioridad ordena como Collections.sort");
        boolean lanzo = false;
        try {
            h.sacar();
        } catch (NoSuchElementException e) {
            lanzo = true;
        }
        chk(lanzo, "ColaPrioridad vacia lanza excepcion");

        System.out.println(fallos == 0 ? "Estructuras: TODAS LAS PRUEBAS OK" : "Estructuras: " + fallos + " fallos");
        if (fallos > 0) System.exit(1);
    }
}
