package evacua.simulacion;

final class Reparacion {
    private int progreso;
    private int ultimoTick = -1;

    boolean trabajadoEn(int tick) { return ultimoTick == tick; }

    boolean avanzar(int tick) {
        if (trabajadoEn(tick) || progreso == 2) return false;
        ultimoTick = tick;
        return ++progreso == 2;
    }

    int progreso() { return progreso; }
}
