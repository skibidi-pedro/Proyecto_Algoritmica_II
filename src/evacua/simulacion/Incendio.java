package evacua.simulacion;

import evacua.edificio.*;

class Incendio extends DesastrePropagado {
    @Override void afectar(MotorSimulacion m, int id) { bloquear(m, id); }
}
