package evacua.simulacion;

import evacua.edificio.*;

class FugaDeGas extends DesastrePropagado {
    @Override void afectar(MotorSimulacion m, int id) {
        m.mapa.n[id / m.mapa.cols][id % m.mapa.cols].condicion = Condicion.PELIGROSO;
        m.recalcularTodas();
    }
}
