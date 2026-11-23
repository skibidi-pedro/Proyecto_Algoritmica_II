package evacua.simulacion;

import evacua.edificio.Edificio;
import evacua.estructuras.Cola;
import evacua.personas.NivelEstres;
import evacua.edificio.Nodo;
import evacua.edificio.TipoNodo;
import evacua.personas.EstadoSalud;
import evacua.personas.Persona;
import evacua.personas.PersonalRescate;
import evacua.personas.Adulto;
import evacua.personas.TipoPersona;
import evacua.edificio.Grafo;
import evacua.rutas.AlgoritmoRuta;
import evacua.rutas.AEstrella;
import evacua.rutas.Dijkstra;
import evacua.rutas.ResultadoRuta;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MotorSimulacion {
    public final Edificio mapa;
    public final ParametrosSimulacion params;
    public final Random rnd;
    public final java.util.function.Consumer<String> log;
    public Grafo grafo;
    private Desastre desastre;
    private final java.util.Map<Persona, EquipoCarga> equipos = new java.util.HashMap<>();

    private double ocupacion(Persona p) {
        EquipoCarga equipo = equipos.get(p);
        if (equipo == null) return p.tipo.ocupacion();
        return equipo.lider() == p ? equipo.ocupacion() : 0;
    }
    private final java.util.Map<Long, Integer> cruces = new java.util.HashMap<>();
    private final AlgoritmoRuta dijkstra = new Dijkstra(), estrella = new AEstrella();

    private long enlace(int a, int b) {
        return (long) Math.min(a, b) * grafo.total() + Math.max(a, b);
    }

    private boolean sinCupo(Persona p, int a, int b) {
        Integer usados = cruces.get(enlace(a, b));
        EquipoCarga equipo = equipos.get(p);
        int necesarios = equipo == null ? 1 : equipo.portadores.size();
        return (usados == null ? 0 : usados) + necesarios > grafo.arista(a, b).capacidad;
    }

    private void actualizarOcupacion() {
        for (Nodo[] fila : mapa.n) for (Nodo n : fila) n.ocupacion = 0;
        for (Persona p : mapa.personas) if (!p.evacuada) mapa.n[p.f][p.c].ocupacion += ocupacion(p);
    }
    public int tick;
    public boolean terminado;

    public Cola<RegistroSalida> salidas = new Cola<>();

    public Runnable alTerminar;
    public int exploradosTotal;

    public MotorSimulacion(Edificio mapa, ParametrosSimulacion params, java.util.function.Consumer<String> log) {
        this.mapa = mapa;
        this.params = params;
        this.rnd = new Random(params.semilla);
        this.log = log;
    }

    public void iniciar() {
        equipos.clear();
        grafo = Grafo.desde(mapa);
        rnd.setSeed(params.semilla);
        desastre = new Desastre(this);
        tick = 0;
        terminado = false;
        salidas = new Cola<>();
        for (Persona p : mapa.personas) {
            if (p instanceof PersonalRescate) {
                ((PersonalRescate) p).retirado = false;
                ((PersonalRescate) p).transportando = false;
            }
            p.evacuada = false;
            p.tickMuerte = -1;
            p.ticksFuego = p.ticksGas = p.confusion = 0;
            p.asistida = false;
            p.ayudaFamiliar = false;
            p.objetivo = null;
            if (p instanceof Adulto) ((Adulto) p).evacuacionIndividual = false;
            p.estres = 0;
            p.bitacora.reiniciar();
            p.historial.reiniciar();
            p.acum = 0;
            p.esperando = 0;
            p.hist.clear();
            p.hist.add("t=0 inicio en (" + p.f + "," + p.c + ")");
            p.ruta.clear();
        }
        cruces.clear();
        exploradosTotal = 0;
        actualizarOcupacion();
        for (Persona p : mapa.personas) {
            p.bitacora.registrar(0, p.f, p.c, ControlCapacidad.estado(mapa.n[p.f][p.c], params.capacidad));
        }

        for (Persona p : mapa.personas) asignarRuta(p);
        log.accept("Emergencia iniciada con " + mapa.personas.size() + " personas ("
                + (params.astar ? "A*" : "Dijkstra") + ", semilla " + params.semilla + ").");
    }

    public void asignarRuta(Persona p) {
        EquipoCarga equipo = equipos.get(p);
        if (!p.activa() || equipo != null && equipo.lider() != p) return;
        registrarBloqueos(p);
        if (p.salud == EstadoSalud.INCAPACITADO && !p.asistida && !p.ayudaFamiliar) {
            p.objetivo = null;
            p.ruta.clear();
            return;
        }
        p.objetivo = p instanceof PersonalRescate
                ? ((PersonalRescate) p).elegirObjetivo(mapa.personas, this::rescateAlcanzable)
                : p.elegirObjetivo(mapa.personas);
        List<int[]> metas = mapa.salidas();
        ResultadoRuta r = null;
        if (p.objetivo != null) {
            List<int[]> rescate = metasRescate(p, p.objetivo);
            r = buscar(p, rescate);
            if (r.ruta != null) metas = rescate;
            else if (p.tipo == TipoPersona.ADULTO) {
                p.objetivo.ayudaFamiliar = false;
                p.ruta.clear();
                return;
            } else {
                // Un enlace utilizado en este tick vuelve a tener cupo en el siguiente.
                p.ruta.clear();
                return;
            }
        }
        if (p.objetivo == null) r = buscar(p, metas);
        if (r.ruta == null && p.ruta.isEmpty()) return;
        if (r.ruta != null && rutaEquivalente(p, metas, r.costo)) return;
        p.ruta = r.ruta == null ? new ArrayList<>() : new ArrayList<>(r.ruta);
        if (equipo != null) for (Persona integrante : equipo.integrantes) integrante.ruta = p.ruta;
        if (tick > 0) {
            p.historial.recalculos++;
            p.hist.add("t=" + tick + (p.ruta.isEmpty() ? " espera sin ruta viable" : " cambia de ruta"));
        }
    }

    private boolean sinEspacio(Persona persona, int id) {
        Nodo n = mapa.n[id / mapa.cols][id % mapa.cols];
        return n.ocupacion + ocupacion(persona) > n.capacidadEfectiva(params.capacidad);
    }

    private ResultadoRuta buscar(Persona p, List<int[]> metas) {
        ResultadoRuta r = evaluarRuta(p, metas, params.astar);
        exploradosTotal += r.explorados;
        return r;
    }

    public ResultadoRuta evaluarRuta(Persona p, List<int[]> metas, boolean astar) {
        EquipoCarga equipo = equipos.get(p);
        Persona encargado = equipo == null ? p : equipo.lider();
        return (astar ? estrella : dijkstra).buscar(mapa, grafo, p.f, p.c, metas, id -> sinEspacio(encargado, id), (a,b) -> sinCupo(encargado,a,b), encargado.tipo == TipoPersona.RESCATE);
    }

    private boolean rutaEquivalente(Persona p, List<int[]> metas, double optimo) {
        int id = p.f * mapa.cols + p.c;
        double costo = 0;
        for (int[] q : p.ruta) {
            int destino = q[0] * mapa.cols + q[1];
            Grafo.Arista a = grafo.arista(id, destino);
            if (a == null || a.bloqueada() || !mapa.caminable(q[0], q[1]) || sinEspacio(p, destino)
                    || sinCupo(p, id, destino)) return false;
            costo += a.costoEntrada(mapa.n[q[0]][q[1]], p.tipo == TipoPersona.RESCATE);
            id = destino;
        }
        if (costo > optimo + 4 * Math.ulp(optimo)) return false;
        for (int[] q : metas) if (id == q[0] * mapa.cols + q[1]) return true;
        return false;
    }

    private boolean rescateAlcanzable(PersonalRescate r, Persona paciente) {
        if (!mapa.caminable(paciente.f, paciente.c)) return false;
        AlgoritmoRuta algoritmo = params.astar ? estrella : dijkstra;
        ResultadoRuta ida = algoritmo.buscar(mapa, grafo, r.f, r.c, metasRescate(r, paciente),
                id -> sinEspacio(r, id), (a,b) -> false, true);
        if (ida.ruta == null) return false;
        int portadores = paciente.tipo == TipoPersona.MOVILIDAD ? 2 : 1;
        // Comprueba que el equipo formado pueda salir por enlaces de capacidad suficiente.
        return algoritmo.buscar(mapa, grafo, paciente.f, paciente.c, mapa.salidas(), null,
                (a,b) -> grafo.arista(a,b).capacidad < portadores, true).ruta != null;
    }

    private List<int[]> metasRescate(Persona p, Persona objetivo) {
        int destino = objetivo.f * mapa.cols + objetivo.c;
        List<int[]> metas = new ArrayList<>();
        metas.add(new int[]{objetivo.f, objetivo.c});
        // Con un paciente en la última plaza, la asistencia se presta desde el nodo contiguo.
        if (sinEspacio(p, destino) && mapa.caminable(objetivo.f, objetivo.c))
            for (Grafo.Arista a : grafo.ady[destino])
                if (!a.bloqueada()) metas.add(new int[]{a.destino / mapa.cols, a.destino % mapa.cols});
        return metas;
    }

    public void recalcularTodas() {
        for (Persona p : mapa.personas) asignarRuta(p);
    }

    private void repararBloqueo(Persona p) {
        if (p.tipo != TipoPersona.RESCATE || ((PersonalRescate) p).retirado || equipos.containsKey(p) || p.salud == EstadoSalud.INCAPACITADO) return;
        for (Grafo.Arista a : grafo.ady[p.f * mapa.cols + p.c]) {
            Nodo n = mapa.n[a.destino / mapa.cols][a.destino % mapa.cols];
            if (a.bloqueada() || !n.bloqueado || !n.bloqueoReparable) continue;
            mapa.bloquear(a.destino / mapa.cols, a.destino % mapa.cols, false);
            p.hist.add("t=" + tick + " repara el bloqueo en " + mapa.idDe(a.destino / mapa.cols, a.destino % mapa.cols));
            break;
        }
    }

    public void generarEvento() {
        desastre.evento(this);
    }

    public void alBloquear(int f, int c) {
        recalcularTodas();
    }

    private void registrarBloqueos(Persona p) {
        // La ruta aún es la anterior. Al recalcular se descartan los nodos bloqueados.
        for (int[] q : p.ruta) {
            if (mapa.n[q[0]][q[1]].bloqueado) {
                p.historial.bloqueosSufridos++;
                estresGrupo(p, NivelEstres.BLOQUEO);
            }
        }
    }

    private boolean cercaDePeligro(Persona p) {
        int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] x : d) {
            int nf = p.f + x[0], nc = p.c + x[1];
            if (mapa.dentro(nf, nc) && mapa.piso(nf) == mapa.piso(p.f) && mapa.n[nf][nc].tipo != TipoNodo.VACIO && mapa.n[nf][nc].peligroso) return true;
        }
        return false;
    }

    private void chequearRescate(Persona p) {
        Persona o = p.objetivo;
        if (p.tipo != TipoPersona.ADULTO || o == null || !o.activa()) return;
        o.ayudaFamiliar = false;
        if (!mapa.caminable(o.f, o.c) || !mapa.caminable(p.f, p.c)) return;
        for (int[] q : metasRescate(p, o)) {
            if (q[0] == p.f && q[1] == p.c) {
                o.ayudaFamiliar = !o.asistida;
                return;
            }
        }
    }

    private void estresGrupo(Persona p, double incremento) {
        EquipoCarga equipo = equipos.get(p);
        if (equipo == null) p.aumentarEstres(incremento);
        else for (Persona integrante : equipo.integrantes) integrante.aumentarEstres(incremento);
    }

    private void formarEquipos() {
        for (Persona r : mapa.personas)
            if (r instanceof PersonalRescate && !equipos.containsKey(r)) asignarRuta(r);
        for (Persona paciente : mapa.personas) {
            if (!paciente.activa() || paciente.confusion > 0 || paciente.asistida || !mapa.caminable(paciente.f, paciente.c)) continue;
            if (paciente.tipo != TipoPersona.NINO && paciente.tipo != TipoPersona.MOVILIDAD) continue;
            int necesarios = paciente.tipo == TipoPersona.NINO ? 1 : 2;
            int destino = paciente.f * mapa.cols + paciente.c;
            Nodo nodo = mapa.n[paciente.f][paciente.c];
            double ocupacionFinal = nodo.ocupacion - paciente.tipo.ocupacion();
            List<PersonalRescate> listos = new ArrayList<>();
            java.util.Map<Long, Integer> entradas = new java.util.HashMap<>();
            for (Persona candidato : mapa.personas) {
                if (!(candidato instanceof PersonalRescate) || candidato.objetivo != paciente
                        || !candidato.activa() || equipos.containsKey(candidato)) continue;
                int origen = candidato.f * mapa.cols + candidato.c;
                if (!mapa.caminable(candidato.f, candidato.c)) continue;
                if (origen != destino) {
                    Grafo.Arista arista = grafo.arista(origen, destino);
                    if (arista == null || arista.bloqueada()) continue;
                    long clave = enlace(origen, destino);
                    int usados = entradas.getOrDefault(clave, 0) + cruces.getOrDefault(clave, 0);
                    if (usados >= arista.capacidad) continue;
                    entradas.merge(clave, 1, Integer::sum);
                    ocupacionFinal += candidato.tipo.ocupacion();
                }
                listos.add((PersonalRescate) candidato);
                if (listos.size() == necesarios) break;
            }
            if (listos.size() != necesarios || ocupacionFinal > nodo.capacidadEfectiva(params.capacidad)) continue;
            EquipoCarga equipo = new EquipoCarga(paciente, listos);
            entradas.forEach((clave, cantidad) -> cruces.merge(clave, cantidad, Integer::sum));
            nodo.ocupacion -= paciente.tipo.ocupacion();
            for (PersonalRescate r : listos) {
                if (r.f != paciente.f || r.c != paciente.c) {
                    mapa.n[r.f][r.c].ocupacion -= r.tipo.ocupacion();
                    nodo.ocupacion += r.tipo.ocupacion();
                    registrarMovimiento(r, paciente.f, paciente.c);
                }
                r.transportando = true;
                r.objetivo = null;
                r.acum = 0;
            }
            paciente.asistida = true;
            paciente.ayudaFamiliar = false;
            for (Persona integrante : equipo.integrantes) {
                equipos.put(integrante, equipo);
                integrante.ruta = equipo.lider().ruta;
                integrante.hist.add("t=" + tick + " CARGAR P" + paciente.id);
            }
            equipo.lider().ruta.clear();
        }
    }

    private void registrarMovimiento(Persona p, int f, int c) {
        p.f = f;
        p.c = c;
        p.pasos++;
        p.esperando = 0;
        Nodo n = mapa.n[f][c];
        p.hist.add("t=" + tick + " -> (" + f + "," + c + ") " + n.tipo.nombre);
        p.bitacora.registrar(tick, f, c, ControlCapacidad.estado(n, params.capacidad));
    }

    public boolean paso() {
        if (terminado) return false;
        tick++;
        cruces.clear();
        actualizarOcupacion();
        desastre.paso(this);
        for (Persona p : mapa.personas) {
            Nodo n = mapa.n[p.f][p.c];
            p.registrarExposicion(n.fuegoActivo, n.concentracionGas() > 60);
        }
        formarEquipos();
        java.util.Set<Persona> atendidos = new java.util.HashSet<>();
        for (Persona p : mapa.personas) {
            EquipoCarga equipo = equipos.get(p);
            if (!p.activa() || atendidos.contains(p) || equipo != null && equipo.lider() != p) continue;
            if (equipo != null) atendidos.addAll(equipo.integrantes);
            Nodo ocupado = mapa.n[p.f][p.c];
            int ambiental = ocupado.danado ? NivelEstres.DANO : 0;
            if (ocupado.peligroso) ambiental += NivelEstres.PELIGRO;
            else if (cercaDePeligro(p)) ambiental += NivelEstres.PROXIMIDAD;
            if (p.confusion > 0) {
                p.confusion--;
                p.aumentarEstres(ambiental);
                continue;
            }
            repararBloqueo(p);
            asignarRuta(p);
            chequearRescate(p);
            if (p.objetivo == null && (p.salud != EstadoSalud.INCAPACITADO || p.asistida || p.ayudaFamiliar)
                    && mapa.caminable(p.f, p.c) && mapa.n[p.f][p.c].tipo == TipoNodo.SALIDA) {
                evacuar(p, ambiental);
                continue;
            }
            double v = p.tipo.vel * p.salud.vel;
            if (p.salud == EstadoSalud.INCAPACITADO) v = p.ayudaFamiliar ? 0.25 : 0;
            if (ocupado.peligroso) v *= 0.65;
            double incremento = p.tipo == TipoPersona.RESCATE ? ambiental * PersonalRescate.FACTOR_ESTRES : ambiental;
            if (NivelEstres.supera(NivelEstres.limitar(p.estres + incremento), params.umbral)) v *= 0.6;
            if (equipo != null && equipo.portadores.size() == 2) {
                PersonalRescate segundo = equipo.portadores.get(1);
                double otra = segundo.tipo.vel * segundo.salud.vel;
                if (ocupado.peligroso) otra *= 0.65;
                if (NivelEstres.supera(segundo.estres + incremento, params.umbral)) otra *= 0.6;
                v = Math.min(v, otra) * 0.5;
            }
            if (p instanceof Adulto && ((Adulto) p).evacuacionIndividual) asignarRuta(p);
            if (v <= 0) {
                estresGrupo(p, ambiental);
                continue;
            }
            p.acum += v;
            while (p.acum >= 1 && !p.evacuada) {
                p.acum -= 1;
                if (p.ruta.isEmpty() && p.tipo == TipoPersona.ADULTO && p.objetivo != null
                        && (p.objetivo.ayudaFamiliar || p.objetivo.asistida)) {
                    Persona familiar = p.objetivo;
                    ResultadoRuta salida = (params.astar ? estrella : dijkstra).buscar(mapa, grafo, familiar.f, familiar.c, mapa.salidas());
                    if (salida.ruta == null || salida.ruta.isEmpty()) break;
                    int[] siguiente = salida.ruta.get(0);
                    if (siguiente[0] != p.f || siguiente[1] != p.c) break;
                    if (mapa.n[p.f][p.c].tipo == TipoNodo.SALIDA) {
                        evacuar(p, ambiental);
                        break;
                    }
                    ResultadoRuta despejar = buscar(p, mapa.salidas());
                    if (despejar.ruta == null || despejar.ruta.isEmpty()) break;
                    p.ruta = new ArrayList<>(despejar.ruta);
                }
                if (p.ruta.isEmpty()) {
                    p.esperando++;
                    if (registrarEspera(p)) ambiental += NivelEstres.CONGESTION;
                    break;
                }
                int[] sig = p.ruta.get(0);
                Nodo ns = mapa.n[sig[0]][sig[1]];
                int origen = p.f * mapa.cols + p.c, destino = sig[0] * mapa.cols + sig[1];
                Grafo.Arista arista = grafo.arista(origen, destino);
                if (!mapa.caminable(sig[0], sig[1]) || arista == null || arista.bloqueada()) {
                    p.hist.add("t=" + tick + " ruta bloqueada en (" + sig[0] + "," + sig[1] + "), recalcula");
                    p.historial.recalculos++;
                    asignarRuta(p);
                    break;
                }
                long enlace = enlace(origen, destino);
                int usados = cruces.getOrDefault(enlace, 0);
                if (sinEspacio(p, destino) || sinCupo(p, origen, destino)) break;
                cruces.put(enlace, usados + (equipo == null ? 1 : equipo.portadores.size()));
                if (arista.peligrosa()) estresGrupo(p, params.intensidad);
                mapa.n[p.f][p.c].ocupacion -= ocupacion(p);
                ns.ocupacion += ocupacion(p);
                p.ruta.remove(0);
                if (equipo == null) registrarMovimiento(p, sig[0], sig[1]);
                else for (Persona integrante : equipo.integrantes) registrarMovimiento(integrante, sig[0], sig[1]);
                if (ns.tipo == TipoNodo.SALIDA && p.objetivo == null) {
                    evacuar(p, ambiental);
                    break;
                } else {
                    chequearRescate(p);
                    if (p.acum >= 1) asignarRuta(p);
                }
            }
            if (!p.evacuada && (equipo == null || equipos.containsKey(p))) estresGrupo(p, ambiental);
        }
        boolean todas = true;
        for (Persona p : mapa.personas) {
            if (p.activa() || p.evacuada && p.tEvac == tick) mapa.n[p.f][p.c].visitas++;
            if (p.activa()) todas = false;
        }
        if (todas || tick >= params.limite) {
            terminado = true;
            for (Persona p : mapa.personas)
                if (p.activa()) p.hist.add("t=" + tick + " NO EVACUADA (tiempo limite)");
            if (alTerminar != null) alTerminar.run();
            log.accept("Simulacion terminada en t=" + tick + ": " + evacuadas() + "/" + mapa.personas.size() + " evacuadas.");
        }
        return !terminado;
    }

    private boolean registrarEspera(Persona p) {
        // Distingue un cuello de botella temporal de una zona aislada por bloqueos.
        List<int[]> metas = p.objetivo == null ? mapa.salidas() : metasRescate(p, p.objetivo);
        ResultadoRuta libre = (params.astar ? estrella : dijkstra).buscar(mapa, grafo, p.f, p.c, metas, null, (a,b) -> false, p.tipo == TipoPersona.RESCATE);
        exploradosTotal += libre.explorados;
        if (libre.ruta == null) return false;
        int origen = p.f * mapa.cols + p.c;
        for (int[] q : libre.ruta) {
            int destino = q[0] * mapa.cols + q[1];
            if (sinEspacio(p, destino) || sinCupo(p, origen, destino)) {
                mapa.n[q[0]][q[1]].esperas++;
                p.historial.esperaCuelloBotella++;
                return origen == p.f * mapa.cols + p.c && sinEspacio(p, destino);
            }
            origen = destino;
        }
        return false;
    }

    private void evacuar(Persona p, int ambiental) {
        estresGrupo(p, ambiental);
        EquipoCarga equipo = equipos.get(p);
        if (equipo != null) {
            for (Persona integrante : equipo.integrantes) {
                equipos.remove(integrante);
                integrante.ruta = new ArrayList<>();
                integrante.hist.add("t=" + tick + " entrega de P" + equipo.paciente.id);
            }
            for (PersonalRescate r : equipo.portadores) {
                r.transportando = false;
                r.acum = 0;
            }
            registrarSalida(equipo.paciente);
            for (PersonalRescate r : equipo.portadores) {
                asignarRuta(r);
                if (r.objetivo == null) {
                    mapa.n[r.f][r.c].ocupacion -= r.tipo.ocupacion();
                    registrarSalida(r);
                }
            }
            return;
        }
        mapa.n[p.f][p.c].ocupacion -= ocupacion(p);
        registrarSalida(p);
    }

    private void registrarSalida(Persona p) {
        if (p instanceof Adulto && p.objetivo != null) p.objetivo.ayudaFamiliar = false;
        p.objetivo = null;
        p.evacuada = true;
        p.tEvac = tick;
        p.hist.add("t=" + tick + " EVACUADA");
        salidas.encolar(new RegistroSalida(salidas.tamano() + 1, p, tick));
    }

    public Desastre emergencia() { return desastre; }

    public void estresPorDesastre(Persona p, double incremento) { desastre.estres(this, p, incremento); }

    public void iniciarConfusion() { desastre.iniciarConfusion(this); }

    public void registrarFallecimiento(Persona p, String causa) {
        if (!mapa.personas.contains(p) || !p.activa()) return;
        if (causa == null || causa.trim().isEmpty()) throw new IllegalArgumentException("Indique la causa del fallecimiento.");
        EquipoCarga equipo = equipos.get(p);
        if (equipo != null) {
            for (Persona integrante : equipo.integrantes) {
                equipos.remove(integrante);
                integrante.ruta = new ArrayList<>();
            }
            for (PersonalRescate r : equipo.portadores) r.transportando = false;
            equipo.paciente.asistida = false;
        }
        p.tickMuerte = tick;
        p.hist.add("t=" + tick + " FALLECIDA: " + causa);
        if (p instanceof Adulto && p.objetivo != null) p.objetivo.ayudaFamiliar = false;
        p.objetivo = null;
        p.ayudaFamiliar = false;
        p.ruta.clear();
        actualizarOcupacion();
        recalcularTodas();
    }

    public PersonalRescate incorporarRefuerzo(int f, int c, java.util.function.IntFunction<PersonalRescate> crear) {
        if (terminado || desastre.restantes(tick) != 0 || !mapa.caminable(f, c)
                || mapa.n[f][c].tipo != TipoNodo.SALIDA || mapa.n[f][c].ocupacion + 0.5 > mapa.n[f][c].capacidadEfectiva(params.capacidad))
            throw new IllegalStateException("Los refuerzos requieren una salida disponible y el contador finalizado.");
        PersonalRescate r = crear.apply(mapa.sigId);
        if (r == null || r.id != mapa.sigId || !r.activa()) throw new IllegalArgumentException("Refuerzo inválido.");
        mapa.sigId++;
        r.f = f;
        r.c = c;
        r.departamentoInicial = 0;
        mapa.personas.add(r);
        mapa.n[f][c].ocupacion += r.tipo.ocupacion();
        r.hist.add("t=" + tick + " ingreso de refuerzo");
        r.bitacora.registrar(tick, f, c, ControlCapacidad.estado(mapa.n[f][c], params.capacidad));
        asignarRuta(r);
        return r;
    }

    public int evacuadas() {
        int k = 0;
        for (Persona p : mapa.personas) if (p.evacuada) k++;
        return k;
    }

    public double promedioEvac() {
        double s = 0;
        int k = 0;
        for (Persona p : mapa.personas)
            if (p.evacuada) {
                s += p.tEvac;
                k++;
            }
        return k == 0 ? 0 : s / k;
    }

    public double promedioEstres() {
        if (mapa.personas.isEmpty()) return 0;
        double s = 0;
        for (Persona p : mapa.personas) s += p.estres;
        return s / mapa.personas.size();
    }

}
