package evacua.personas;

import java.util.ArrayList;
import java.util.List;

public abstract class Persona {
    public final int id;
    public final TipoPersona tipo;
    public final EstadoSalud saludInicial;
    public EstadoSalud salud;
    public boolean alertada;
    public int ticksHumo;
    public final int[] exposicionGas = new int[4];
    public int f, c;
    public double estres, acum;
    public boolean evacuada, asistida;
    public int tEvac = -1, pasos;
    public List<int[]> ruta = new ArrayList<>();
    public Persona objetivo;
    public int departamentoInicial;
    public boolean ayudaFamiliar;

    public int tickMuerte = -1, confusion, ticksFuego, ticksGas;

    public boolean fuera() { return evacuada || (this instanceof PersonalRescate && tEvac >= 0); }
    public boolean activa() { return !fuera() && tickMuerte < 0; }

    public boolean confundida() { return activa() && confusion > 0; }

    public void registrarExposicion(boolean fuego, boolean gas) {
        if (!activa()) return;
        ticksFuego = fuego ? ticksFuego + 1 : 0;
        ticksGas = gas ? ticksGas + 1 : 0;
    }

    public int esperando;
    public final List<String> hist = new ArrayList<>();
    public final Bitacora bitacora = new Bitacora();
    public final HistorialEventos historial = new HistorialEventos();

    protected Persona(int id, TipoPersona tipo, EstadoSalud salud, int f, int c) {
        this.id = id;
        this.tipo = tipo;
        this.saludInicial = this.salud = salud;
        this.f = f;
        this.c = c;
    }

    public static Persona crear(int id, TipoPersona tipo, EstadoSalud salud, int f, int c) {
        switch (tipo) {
            case NINO: return new Nino(id, salud, f, c);
            case MOVILIDAD: return new MovilidadReducida(id, salud, f, c);
            case RESCATE: return new PersonalRescate(id, salud, f, c);
            default: return new Adulto(id, salud, f, c);
        }
    }

    public Persona elegirObjetivo(List<Persona> todas) {
        return null;
    }

    public void cargarDepartamento(String dep) {
        if (dep == null || dep.isEmpty()) return;
        if (dep.equals("SIN_DEPARTAMENTO")) {
            departamentoInicial = 0;
            return;
        }
        if (!dep.matches("Dep-[0-9]{3,}")) throw new IllegalArgumentException("Departamento inválido: " + dep);
        departamentoInicial = tipo == TipoPersona.RESCATE ? 0 : Integer.parseInt(dep.substring(4));
    }

    public double limiteEstres() { return NivelEstres.MAX; }

    public void aumentarEstres(double d) {
        if (activa()) estres = Math.max(0, Math.min(limiteEstres(), estres + d));
    }

    public void reducirEstres(double d) {
        estres = Math.max(0, Math.min(limiteEstres(), estres - d));
    }

    public String etiqueta() {
        return "P" + id + " (" + tipo.nombre + ", " + salud + ")";
    }
}
