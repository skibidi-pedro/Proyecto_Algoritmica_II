# Evacua: Simulador de Evacuación de Edificios

Proyecto del curso **Algorítmica II** (primer entregable).
Docente: Juan Ricardo Tapia Carbajal · Lenguaje: **Java** (interfaz con **Swing**).

Integrantes:
- Pucho Lizana, Pedro Eduardo Mar (Integrante 1)
- Zavaleta Calderon, Steven Jack (Integrante 2)
- Ayala Navarro, Piero Daniel (Integrante 3)

> **Para otra IA / colaborador:** este README resume el proyecto para que puedas entender qué se construye, con qué reglas y cómo se reparte el trabajo. Léelo completo antes de generar o modificar código. Si algo del código contradice este documento, pregunta antes de asumir cuál tiene razón.

---

## 1. ¿En qué consiste el proyecto?

**Evacua** simula un edificio formado por habitaciones, pasillos, escaleras y salidas de emergencia. Cuando el usuario inicia una emergencia, distintos tipos de personas buscan y recorren una ruta de evacuación hacia la salida más cercana.

**Problema:** en una emergencia hay que encontrar rápido una ruta segura evitando zonas bloqueadas, dañadas o congestionadas. Diseñar y probar protocolos de evacuación a mano es lento y poco confiable.

**Solución:** un simulador que aplica algoritmos de búsqueda de caminos (Dijkstra y A*), modela distintos tipos de persona (cada una con su velocidad y comportamiento) y permite ejecutar, auditar y comparar escenarios antes de una emergencia real. Además:
- audita el recorrido de cada persona (bitácora),
- importa el plano desde un archivo estructurado,
- modela eventos aleatorios y niveles de estrés.

## 2. Conceptos clave del dominio

| Concepto | Descripción |
|---|---|
| **Nodo** | Habitación, Pasillo, Escalera o Salida. Solo las **Salidas** son destino válido de una ruta. |
| **Estado del nodo** | Libre, Congestionado o Bloqueado. Solo Libre/Congestionado pueden formar parte de una ruta. |
| **Conexión** | Arista del grafo (puerta, pasillo, escalera) con **tiempo de recorrido > 0** y **capacidad máxima**. |
| **Persona** | Clase base con tipos derivados: Adulto, Niño, Persona con movilidad reducida, Personal de rescate. Cada uno con velocidad y comportamiento propios. |
| **Estrés** | Valor de 0 a 100 por persona. Sube por congestión, recálculos de ruta y cercanía a peligros; al pasar un umbral reduce la velocidad o cambia el comportamiento. |
| **Bitácora** | Registro inmutable por persona: cada nodo visitado + paso de simulación (timestamp) + estado del nodo en ese instante. Lista doble o pila. |
| **Escenario** | Edificio + personas + bloqueos, guardado en base de datos para repetir o comparar simulaciones. |
| **Desastre** | Tipo de emergencia que se simula: Terremoto, Incendio, Inundación, Fuga de gas, etc. Cada tipo define cómo se bloquean zonas, cuánto estrés genera y cómo evoluciona (RN-20). |
| **Mapa de calor** | Visualización final de la ocupación/congestión acumulada por nodo. |

## 3. Funcionalidades (módulos)

1. **Modelado del edificio.** CRUD de nodos (habitaciones, pasillos, escaleras, salidas) y conexiones entre ellos en un grafo, con tiempo de recorrido y capacidad.
2. **Gestión de personas.** Registrar personas en un nodo inicial; jerarquía de clases desde `Persona`, con trazabilidad, estrés e historial de eventos; lógica de prioridades diferenciada para rescate.
3. **Cálculo de rutas.** Ruta más rápida a la salida disponible más cercana; recálculo automático si el camino se bloquea o congestiona; **A\*** (heurística euclidiana o Manhattan) como alternativa a **Dijkstra**, comparando nodos explorados y tiempo de cómputo.
4. **Bloqueo de zonas y peligros.** Marcar nodos/conexiones como bloqueados, dañados o peligrosos y excluirlos del cálculo mientras dure el bloqueo.
5. **Simulación.** Emergencia paso a paso; tiempo de salida por persona, cuellos de botella y personas que no evacúan dentro del tiempo límite.
6. **Escenarios y persistencia.** Guardar/cargar escenarios en una base de datos.
7. **Reportes y estadísticas.** Por simulación: tiempo total, tiempo promedio, evacuados/no evacuados, nodos más congestionados. Comparación entre escenarios del mismo edificio. Exportación de un reporte individual por persona con su línea de tiempo.
8. **Visualización (Swing).** Edificio, personas y rutas en tiempo real; mapa de calor al terminar.
9. **Trazabilidad y comportamiento de `Persona`.** Bitácora, estrés dinámico (0–100), historial de eventos (bloqueos sufridos, recálculos, tiempo de espera en cuellos de botella) y archivo individual con la línea de tiempo.
10. **Importación de planos.** Parser Java para `.json`, `.txt` o `.xml` que valida el archivo y construye el grafo sin alterar el edificio actual si hay errores. Las coordenadas (x, y) sirven para la heurística de A* y para dibujar el plano.
11. **Optimización y simulación estocástica.** Rescate prioriza niños y personas con movilidad reducida antes de evacuar; eventos aleatorios (colapso de pasillos) que fuerzan recálculo; acumulación de ocupación para el mapa de calor.
12. **Tipos de desastre.** Al iniciar la emergencia se elige el tipo de desastre (Terremoto, Incendio, Inundación, Fuga de gas, etc.). Cada tipo tiene su propia dinámica: qué zonas bloquea, cómo se propaga o evoluciona en el tiempo, cuánto estrés genera y si afecta la velocidad. Los eventos aleatorios del módulo 11 pasan a depender del desastre elegido.

## 4. Reglas del negocio

Los valores numéricos son **configurables**; los de abajo son valores por defecto.

| Código | Regla | Descripción |
|---|---|---|
| RN-01 | Unicidad | Cada nodo, persona y conexión tiene un identificador único. |
| RN-02 | Tipos de nodo | Habitación, Pasillo, Escalera o Salida. Solo Salida es destino válido. |
| RN-03 | Conexión válida | Tiempo de recorrido > 0 y capacidad máxima definida. |
| RN-04 | Estado del nodo | Libre, Congestionado o Bloqueado. Solo Libre/Congestionado forman parte de una ruta. |
| RN-05 | Bloqueo de zonas | Un nodo o conexión bloqueada se excluye del cálculo mientras dure; al desbloquearse vuelve a estar disponible. |
| RN-06 | Cálculo de rutas | Ruta hacia la salida disponible más cercana según tiempo de recorrido, evitando bloqueos. |
| RN-07 | Recálculo dinámico | Si un camino asignado se bloquea en plena simulación, se recalcula desde la posición actual. |
| RN-08 | Capacidad de nodos | Si las personas en un nodo superan su capacidad, pasa a Congestionado y aumenta el tiempo de recorrido de quienes lo cruzan. |
| RN-09 | Tiempo límite | Quien no llega a una salida dentro del tiempo límite se registra como **No evacuada**. |
| RN-10 | Persistencia | Un escenario solo se guarda si hay al menos una salida y todas las personas están en un nodo válido. |
| RN-11 | Bitácora | Cada nodo visitado se registra con paso de simulación y estado del nodo. Los registros **no se modifican** una vez escritos. |
| RN-12 | Estrés | Valor 0–100; sube por congestión, recálculos y peligros; al superar un umbral configurable reduce velocidad o altera comportamiento. |
| RN-13 | Historial de eventos | Contabiliza bloqueos sufridos, recálculos y tiempo total de espera en cuellos de botella. |
| RN-14 | Exportación individual | Al terminar se genera un archivo por persona con su línea de tiempo, haya evacuado o vencido el tiempo. |
| RN-15 | Importación de planos | El archivo define nodos (id único, tipo, x, y) y conexiones entre nodos existentes (peso > 0, capacidad máxima), con al menos una salida. Si es inválido se rechaza con mensaje de error y el edificio actual no cambia. |
| RN-16 | Búsqueda con A\* | La heurística se convierte a tiempo de recorrido para **no sobreestimar** el costo real (ruta óptima). Mismas exclusiones que RN-04 y RN-05. |
| RN-17 | Personal de rescate | Va primero a nodos con niños o movilidad reducida y solo después a una salida, salvo que su ruta se bloquee o se agote el tiempo. |
| RN-18 | Eventos aleatorios | En cada paso puede ocurrir un evento (p. ej. colapso de pasillo) con probabilidad configurable; el elemento pasa a Bloqueado y se aplica RN-07. Una **semilla configurable** permite repetir la secuencia. |
| RN-19 | Mapa de calor | Se acumula ocupación/congestión por nodo; al finalizar, Swing muestra el mapa de calor. |
| RN-20 | Tipos de desastre | La simulación se inicia con un tipo de desastre (Terremoto, Incendio, Inundación, Fuga de gas, etc.). Cada tipo define qué nodos o conexiones bloquea, su evolución paso a paso (por ejemplo, el incendio se propaga a nodos vecinos y el terremoto colapsa pasillos y escaleras al azar), el estrés que genera y su probabilidad e intensidad configurables. Con la misma semilla el desastre se repite igual (RN-18). Los bloqueos que provoca activan el recálculo de rutas (RN-07). |

## 5. Estructuras de datos y algoritmos

- **Estructuras propias reutilizables** (se implementan en el proyecto): lista enlazada simple y doble, pila, cola, cola de prioridad (heap).
- **Grafo** para el edificio (nodos + conexiones ponderadas con capacidad).
- **Dijkstra** y **A\*** para rutas; se comparan por nodos explorados y tiempo de cómputo.
- **Lista doble o pila** para la bitácora de recorrido de cada persona.
- **Heap** como cola de prioridad en la búsqueda de caminos.

## 6. Reparto de trabajo por integrante

Cada integrante es responsable de sus módulos y de sus pruebas; todos participan en la integración final. Las interfaces entre roles están definidas para poder trabajar en paralelo.

### 6.1 Estructura de carpetas

Cada integrante es **dueño de sus paquetes** dentro de `src/`. No se editan archivos de otro paquete sin avisar; la comunicación entre paquetes se hace solo por las clases públicas indicadas en 6.3.

```
Proyecto_Algoritmica_II/
├── README.md
├── src/
│   └── evacua/
│       ├── Main.java                         [Integrante 1] punto de entrada
│       │
│       ├── estructuras/                      [Integrante 1]
│       │   ├── ListaSimple, ListaDoble, Pila, Cola
│       │   └── ColaPrioridad.java            (heap)
│       ├── persistencia/                     [Integrante 1]
│       │   ├── BaseDatos.java                (base embebida: escenarios .json + resultados.csv)
│       │   └── GestorEscenarios.java         (validar RN-10, guardar/cargar .txt/.json)
│       ├── reportes/                         [Integrante 1]
│       │   ├── ReporteSimulacion.java        (reporte general e individual)
│       │   ├── ComparadorEscenarios.java     (compara escenarios, registra resultados)
│       │   ├── ExportadorIndividual.java     (un archivo por persona, RN-14)
│       │   └── ExportadorSalidas.java        (CSV de quienes salieron, desde la cola de salidas)
│       │
│       ├── edificio/                         [Integrante 2]
│       │   ├── Edificio.java                 (nodos, CRUD, conexiones, bloqueos, copia)
│       │   ├── HistorialEdicion.java         (pila de "Deshacer" del editor)
│       │   ├── Nodo.java, TipoNodo.java, EstadoNodo.java
│       │   ├── Conexion.java                 (tiempo > 0 y capacidad, RN-03)
│       │   └── Grafo.java                    (aristas ponderadas con capacidad)
│       ├── rutas/                            [Integrante 2]
│       │   ├── AlgoritmoRuta.java            (busqueda con heap, excluye bloqueos)
│       │   ├── Dijkstra.java, AEstrella.java
│       │   ├── Heuristica.java               (Manhattan convertida a tiempo, RN-16)
│       │   ├── RecalculadorRutas.java        (RN-07)
│       │   ├── BuscadorRutas.java            (fachada)
│       │   └── ResultadoRuta.java            (ruta, nodos explorados, tiempo)
│       ├── importacion/                      [Integrante 2]
│       │   ├── ParserPlano.java              (elige parser por extension; .txt)
│       │   ├── ParserJson.java, JsonMini.java, ParserXml.java
│       │   ├── PlanoDatos.java               (datos leidos)
│       │   ├── ValidadorPlano.java           (todos los errores, RN-15)
│       │   └── ConstructorEdificio.java      (construye el grafo si es valido)
│       ├── vista/                            [Integrante 2]
│       │   ├── VentanaPrincipal.java         (Swing: botones, estado, registro)
│       │   ├── PanelEdificio.java            (edificio, personas, rutas y mapa de calor)
│       │   └── Herramienta.java
│       │
│       ├── personas/                         [Integrante 3]
│       │   ├── Persona.java                  (clase base)
│       │   ├── Adulto, Nino, MovilidadReducida, PersonalRescate
│       │   ├── Bitacora.java, RegistroBitacora.java   (lista doble, RN-11)
│       │   ├── NivelEstres.java              (0 a 100, RN-12)
│       │   ├── HistorialEventos.java         (RN-13)
│       │   └── TipoPersona.java, EstadoSalud.java
│       └── simulacion/                       [Integrante 3]
│           ├── MotorSimulacion.java          (paso a paso)
│           ├── EventoAleatorio.java          (colapso de pasillos, RN-18)
│           ├── ControlCapacidad.java         (Congestionado, RN-08)
│           └── ParametrosSimulacion.java
├── recursos/planos/                          planos de ejemplo: edificio.txt, edificio_ids.json, edificio_ids.xml y edificio_error.json (inválido a propósito)
├── test/evacua/                              PruebasEstructuras, PruebasPlanos, PruebasPersistencia, PruebasPisos, PruebasCongestion
└── docs/                                     informe, diagrama de casos de uso
```

> Todos los archivos de `src/` existen y compilan. Cada integrante sigue siendo dueño de sus paquetes.

### 6.2 Resumen por integrante

| Integrante | Rol | Paquetes en `src/evacua/` | Reglas |
|---|---|---|---|
| 1 | Núcleo, persistencia, reportes e integración | `Main`, `estructuras`, `persistencia`, `reportes` | RN-01, RN-10, RN-14 |
| 2 | Edificio, rutas, importación e interfaz | `edificio`, `rutas`, `importacion`, `vista`, `recursos/planos` | RN-02 a RN-07, RN-15, RN-16, RN-19 |
| 3 | Personas, comportamiento y motor de simulación | `personas`, `simulacion` | RN-08, RN-09, RN-11, RN-12, RN-13, RN-17, RN-18, RN-20 |

### 6.3 Puntos de contacto entre paquetes

| Quien provee | Clase / operación | Quien la usa |
|---|---|---|
| Integrante 1 | `estructuras.*` (`ColaPrioridad`, `ListaDoble`, ...) | `rutas.AlgoritmoRuta` (Integrante 2) y `personas.Bitacora` (Integrante 3) |
| Integrante 2 | `RecalculadorRutas`, `Dijkstra`, `AEstrella`, `Edificio` | `simulacion.MotorSimulacion` (Integrante 3) |
| Integrante 3 | `Persona` (`bitacora`, `historial`, `estres`) | `reportes.ReporteSimulacion` y `ExportadorIndividual` (Integrante 1) |
| Integrante 3 | `MotorSimulacion` (tiempos, evacuadas, cuellos de botella) | `reportes.*` (Integrante 1) y `vista.*` (Integrante 2) |
| Integrante 3 | `EventoAleatorio` (bloquea un pasillo) | `MotorSimulacion` y su recálculo (Integrante 2) |
| Integrante 2 | `ParserPlano.leer(...)` | `persistencia.GestorEscenarios` y `BaseDatos` (Integrante 1) |
| Integrante 1 | `BaseDatos`, `GestorEscenarios` | `vista.VentanaPrincipal` (Integrante 2) y `Main` |

### 6.4 Tareas por integrante

**Integrante 1: Núcleo, persistencia, exportación e integración** (RN-01, RN-10, RN-14)
*Paquetes:* `Main`, `estructuras`, `persistencia`, `reportes`

- Estructuras base: listas simple/doble, pila, cola, heap.
- Menú principal, clase `Main` y comunicación entre módulos (importar plano, exportar reportes individuales, comparar escenarios).
- Registro, guardado y carga de escenarios en base de datos.
- Exportación individual por persona (usa la bitácora e historial del Integrante 3).
- Comparación de resultados entre escenarios guardados.
- *(Reasignado)* Reportes y estadísticas de cada simulación: tiempo total, tiempo promedio, evacuados/no evacuados y nodos más congestionados (usa los resultados del motor del Integrante 3).
- Configuración del repositorio, integración completa y coordinación de entregas.

**Integrante 2: Edificio, rutas, bloqueos e importación de planos** (RN-02, RN-03, RN-04, RN-05, RN-06, RN-07, RN-15, RN-16, RN-19)
*Paquetes:* `edificio`, `rutas`, `importacion`, `vista`, `recursos/planos`

- Grafo del edificio con CRUD de nodos, estados y capacidades.
- Gestión de bloqueos (marcar/desbloquear) y su exclusión del cálculo de rutas.
- Dijkstra y A\* con comparación de nodos explorados y tiempo.
- Recálculo dinámico desde la posición actual, expuesto como operación reutilizable para el motor de simulación.
- Parser de planos `.json`/`.txt`/`.xml` con validación, y archivos de plano de prueba.
- *(Reasignado)* Interfaz Swing: dibuja edificio, personas y rutas en tiempo real (aprovecha las coordenadas x, y de los nodos).
- *(Reasignado)* Acumulación de ocupación por nodo y mapa de calor en Swing al finalizar la simulación (RN-19).

**Integrante 3: Personas y comportamiento** (RN-08, RN-09, RN-11, RN-12, RN-13, RN-17, RN-18, RN-20)
*Paquetes:* `personas`, `simulacion`

- Jerarquía: adulto, niño, movilidad reducida, personal de rescate.
- En `Persona`: bitácora (lista doble o pila), estrés e historial de eventos.
- Lógica diferenciada del personal de rescate.
- Eventos aleatorios (colapso de pasillos) con probabilidad y semilla configurables; bloquean el elemento y activan el recálculo del Integrante 2.
- *(Pendiente)* Tipos de desastre (Terremoto, Incendio, Inundación, Fuga de gas, etc.): clase base `Desastre` en `simulacion/` con una clase derivada por tipo (ver 7.1, último punto).
- Control de capacidad de nodos (Congestionado, mayor tiempo de recorrido).
- *(Reasignado)* Motor de simulación paso a paso, que integra eventos aleatorios, recálculo de rutas y control de congestión.
- *(Reasignado)* Cálculo de tiempos de evacuación, cuellos de botella y personas no evacuadas dentro del tiempo límite (RN-09).

### 6.5 Cómo funciona cada parte: código y datos

Esta sección explica, por integrante, qué hace el código de cada funcionalidad y cómo lee o escribe los archivos de datos. Primero el mapa general de archivos y después el detalle.

#### Archivos de datos

| Archivo | Formato | Quién lo escribe | Quién lo lee | Para qué |
|---|---|---|---|---|
| Plano o escenario (`recursos/planos/edificio.txt`) | `.txt` en cuadrícula, con pisos y personas | `GestorEscenarios.guardar` (botón "Guardar escenario") | `GestorEscenarios.leer` → `ParserPlano.leer` → `deTexto` | Edificio de cuadrícula: un carácter por nodo |
| Plano con grafo (`edificio_ids.json`, `edificio_ids.xml`) | `.json` y `.xml` con nodos, conexiones y personas | `.json`: `GestorEscenarios.aJson` | `ParserJson` / `ParserXml` → `ValidadorPlano` → `ConstructorEdificio` | Edificio con conexiones de tiempo y capacidad |
| Escenarios guardados (`datos/escenarios/<nombre>.json`) | `.json` | `BaseDatos.guardar` | `BaseDatos.cargar` y `listar` | Escenarios para repetir o comparar |
| Resultados (`datos/resultados.csv`) | CSV: `escenario,algoritmo,semilla,personas,evacuadas,ticks,tiempo_promedio,estres_promedio` | `BaseDatos.registrarResultado` (lo llama `ComparadorEscenarios.compararGuardados`) | `BaseDatos.resultados` | Historial de comparaciones |
| Salidas (`datos/salidas.csv`) | CSV: `orden_salida,id,tipo,salud,tiempo_salida,estres_al_salir,asistida,pasos,recalculos` | `ExportadorSalidas.exportar` (solo al terminar la simulación, o con el botón "Exportar salidas (CSV)") | Excel o cualquier lector de CSV | Tiempo y estado de quienes salieron, en orden |
| Reporte individual (`reporte_P<id>.txt`) | `.txt` con bitácora y línea de tiempo | `ExportadorIndividual.exportar` y `exportarTodos` | Personas | Trazabilidad de cada persona (RN-14) |

Recorrido de los datos, de la entrada a la salida:

```
archivo .txt / .json / .xml
        │  ParserPlano.leer elige el parser por la extensión
        ▼
PlanoDatos ── ValidadorPlano (RN-15) ── ConstructorEdificio ──► Edificio (grafo + personas)
        │                                                         │ copia()
        │                                                         ▼
        │                                            MotorSimulacion.paso() una vez por tick
        │                                              ├─► Persona.bitacora  ──► reporte_P<id>.txt
        │                                              ├─► motor.salidas (Cola) ──► salidas.csv
        │                                              └─► Nodo.ocupacion / visitas ──► mapa de calor
        ▼
GestorEscenarios.aTexto / aJson ──► BaseDatos ──► ComparadorEscenarios ──► resultados.csv
```

#### Integrante 1: núcleo, persistencia y reportes

- **Estructuras propias** (`estructuras/`): `ListaSimple`, `ListaDoble`, `Pila`, `Cola` y `ColaPrioridad` (heap). Quién las usa: el heap en `AlgoritmoRuta` (rutas), la lista doble en `Bitacora`, la cola en `MotorSimulacion.salidas` y la pila en `HistorialEdicion` (botón Deshacer).
- **Escenarios en archivo** (`GestorEscenarios`):
  - `aTexto(edificio, conPersonas)` escribe el `.txt`: un bloque de filas por piso, con una línea `#PISO n` entre pisos, y al final `#PERSONAS`. La letra va en minúscula si el nodo está bloqueado.
  - `aJson(edificio)` escribe el `.json` completo con pisos, nodos, conexiones, bloqueos y personas.
  - `leer(archivo)` lee cualquiera de los tres formatos; `guardar(archivo, edificio)` escribe `.json` si el nombre termina en `.json` y `.txt` en cualquier otro caso.
  - `validarParaGuardar` aplica RN-10: al menos una salida y toda persona sobre un nodo válido. Si falla, no se escribe nada.
- **Base de datos embebida** (`BaseDatos`): guarda cada escenario como `datos/escenarios/<nombre>.json` y lo carga, lista o elimina. Rechaza nombres con rutas (por ejemplo `../x`). Agrega filas a `datos/resultados.csv` y crea la cabecera si el archivo no existe.
- **Comparación** (`ComparadorEscenarios`): `simular` corre una copia del edificio hasta el final con los mismos parámetros y devuelve una fila resumen. `compararGuardados` hace lo mismo con escenarios de la base de datos y agrega cada fila a `resultados.csv`.
- **Salidas a CSV** (`ExportadorSalidas`): `MotorSimulacion` encola un `RegistroSalida` cada vez que alguien sale. `exportar` recorre la cola en orden, escribe una fila por persona y deja la cola intacta, así que se puede exportar varias veces.
- **Reportes** (`ReporteSimulacion`, `ExportadorIndividual`): el reporte general se muestra en un diálogo. El individual incluye la bitácora de nodos visitados, la línea de tiempo y los contadores del historial. `exportarTodos` crea un archivo `reporte_P<id>.txt` por persona.
- **`Main`**: abre `VentanaPrincipal`.

#### Integrante 2: edificio, rutas, importación e interfaz

- **Edificio** (`edificio/`): `Edificio` guarda los nodos en una cuadrícula con varios pisos apilados (el piso k ocupa las filas `k*filasPorPiso` a `(k+1)*filasPorPiso`). Tiene crear, modificar y eliminar nodos, conectar, desconectar y bloquear (nodos y conexiones), y `copia()` para simular sin alterar el original. `Grafo.desde(edificio)` arma las aristas: vecinos de la cuadrícula (escalera = tiempo 3), las escaleras de la misma fila y columna entre pisos consecutivos, y las `Conexion` explícitas del plano. `HistorialEdicion` es la pila del botón Deshacer.
- **Rutas** (`rutas/`): `AlgoritmoRuta` hace la búsqueda con el heap, excluyendo nodos y conexiones bloqueados. `Dijkstra` usa heurística 0 y `AEstrella` usa `Heuristica` (Manhattan por piso, convertida a tiempo para no sobreestimar). `RecalculadorRutas.desdePosicion` rehace la ruta desde donde está la persona, y `evitandoCongestion` lo hace rodeando los nodos llenos.
- **Importación** (`importacion/`): `ParserPlano.leer(contenido, nombre)` elige por extensión.
  - `.txt`: `deTexto` lee los bloques de filas por piso y las líneas de personas (`fila columna TIPO SALUD`, o `piso fila columna TIPO SALUD` con varios pisos).
  - `.json` y `.xml`: `ParserJson` y `ParserXml` leen a un `PlanoDatos` sin validar. `ValidadorPlano.validar` devuelve **todos** los errores: ids repetidos, tipos o coordenadas inválidos, celdas ocupadas, conexiones a nodos inexistentes, tiempo menor o igual a cero, falta de capacidad y falta de salida. Si hay errores, `ConstructorEdificio` lanza una excepción con la lista completa y no construye nada. Si no los hay, construye el `Edificio`.
  - En el `.json`, `"modo": "cuadricula"` usa los vecinos de la cuadrícula y `"modo": "grafo"` usa solo las conexiones listadas.
- **Interfaz** (`vista/`): `VentanaPrincipal` tiene los botones y lee o escribe archivos a través de `GestorEscenarios`, `BaseDatos` y los exportadores. `PanelEdificio` dibuja un piso a la vez, con personas, bloqueos y el mapa de calor. El selector de pisos y la lista "Salieron" están a la derecha. Un clic de la herramienta activa guarda antes una copia en la pila de Deshacer.

#### Integrante 3: personas, comportamiento y simulación

- **Personas** (`personas/`): `Persona` es la clase base y `Adulto`, `Nino`, `MovilidadReducida` y `PersonalRescate` la extienden (`Persona.crear` elige la clase según el tipo). Cada `Persona` lleva su `Bitacora` (lista doble de `RegistroBitacora`: paso, nodo y estado del nodo, que solo se agrega), su `HistorialEventos` (bloqueos sufridos, recálculos y espera en cuellos de botella) y su estrés de 0 a 100 (`NivelEstres`). `PersonalRescate.elegirObjetivo` va primero a incapacitados y luego a niños o movilidad reducida.
- **Motor** (`MotorSimulacion.paso()`, una vez por tick):
  1. Sube el contador de ticks y, con la probabilidad configurada, genera un evento aleatorio (`EventoAleatorio` colapsa un pasillo) y recalcula las rutas.
  2. Recalcula la ocupación de cada nodo (`ControlCapacidad` marca Congestionado si está lleno).
  3. Mueve a cada persona según su velocidad, su salud y su estrés. Si el siguiente nodo está bloqueado recalcula; si está lleno espera los ticks configurados y luego rodea la congestión.
  4. Registra en la bitácora cada nodo visitado y, si alguien sale, lo encola en `salidas`.
  5. Acumula las visitas por nodo (mapa de calor acumulado). Al terminar llama a `alTerminar`, que guarda `datos/salidas.csv`.
- **Datos que consume y produce:** recibe las personas y los bloqueos que vienen del plano `.txt`, `.json` o `.xml`. Produce la bitácora y el historial (que salen en `reporte_P<id>.txt`) y la cola de salidas (que sale en `salidas.csv`).
- **Parámetros** (`ParametrosSimulacion`): probabilidad de evento, semilla, tiempo límite, umbral de estrés, capacidad por nodo, algoritmo y ticks de espera antes de rodear una congestión.

## 7. Estado actual del repositorio

Los requisitos funcionales de los tres integrantes están implementados en `src/evacua/` y cubiertos por pruebas. Es Java Swing, sin dependencias externas.

Qué hay hoy (por integrante):
- **Integrante 1:** estructuras propias (lista simple y doble, pila, cola, heap): el heap lo usan las rutas, la lista doble la bitácora, la cola las salidas y la pila el botón "Deshacer" (Ctrl+Z) del editor, que revierte pintar, borrar, bloquear, agregar un piso o cargar un plano; CSV de salidas (`orden_salida,id,tipo,salud,tiempo_salida,estres_al_salir,asistida,pasos,recalculos`) que se guarda solo al terminar la simulación en `datos/salidas.csv` y con el botón "Exportar salidas (CSV)"; base de datos embebida (`datos/escenarios/*.json` y `datos/resultados.csv`) con la validación RN-10; comparación de escenarios; exportación de un reporte por persona.
- **Integrante 2:** grafo con conexiones (tiempo > 0 y capacidad), CRUD de nodos y conexiones, bloqueos de nodos y conexiones; Dijkstra y A\* separados, con la heurística convertida a tiempo; recalculador de rutas; importación de planos `.json`, `.xml` y `.txt` con validación que informa todos los errores.
- **Integrante 3:** recálculo por congestión (si el siguiente nodo está lleno, la persona espera los ticks configurados y rodea el nodo lleno por otro camino; si no hay otro camino, sigue esperando); jerarquía `Adulto`, `Nino`, `MovilidadReducida`, `PersonalRescate`; bitácora sobre lista doble; estrés de 0 a 100; historial de eventos; rescate que prioriza incapacitados y luego niños o movilidad reducida; eventos aleatorios con semilla; control de capacidad con estado Congestionado.

Pisos: el edificio puede tener varios pisos. El edificio original es el **piso 1** y el selector de la derecha (Piso 1, 2, 3...) cambia el piso que se dibuja; "+ Agregar piso" agrega uno vacío en modo edición. Las **escaleras** (`E`) en la misma fila y columna de pisos consecutivos los conectan (costo 3). Las personas de pisos altos bajan por ellas hasta una salida. El mapa de calor se actualiza por tick (colorea solo los nodos con gente en ese momento, más rojo cuanto más cerca de la capacidad) y, al terminar, muestra la ocupación acumulada (RN-19). Debajo del selector de pisos hay una lista "Salieron" con el id y el tiempo de quienes ya salieron. Mientras corre la simulación, cada vez que se bloquea o desbloquea una zona se recalculan solas las rutas de todos (por eso no hay botón de recalcular).

Limitaciones conocidas:
- La interfaz dibuja un piso a la vez como cuadrícula: un plano importado se ubica por las coordenadas `x` (columna) e `y` (fila), y las conexiones no se dibujan como líneas.
- El tiempo de una conexión decide la ruta, pero la persona avanza un nodo por unidad de velocidad. La capacidad de las conexiones se valida y se guarda, pero la congestión se controla por nodo.
- La "base de datos" es un almacén embebido en archivos (carpeta `datos/`), sin servidor.
- La interfaz no tiene pantallas para crear conexiones: se hacen por código (`Edificio.conectar`) o importando un plano.

### 7.1 Pendiente por implementar

Lo que todavía falta para cumplir el documento por completo:

1. **Dibujar las rutas de evacuación (2.8).** La interfaz dibuja personas, bloqueos y mapa de calor, pero no la ruta asignada a cada persona. Dibujar la ruta en el piso que se está viendo. *(Integrante 2, `vista`)*
2. **Estados dañado y peligroso (2.4).** Solo existe "bloqueado". Agregar los estados dañado y peligroso para nodos y conexiones, y poder bloquear conexiones desde la interfaz (hoy solo por código). *(Integrante 2)*
3. **Editar conexiones desde la interfaz (2.1).** Definir el tiempo de recorrido y la capacidad de cada conexión con una pantalla (hoy se hace importando un plano o por código con `Edificio.conectar`). *(Integrante 2, `vista`)*
4. **Exportar los reportes individuales al terminar (RN-14).** Hoy se exportan con un botón; deben generarse automáticamente cuando termina la simulación, haya evacuado o no cada persona. *(Integrante 1)*
5. **Capacidad de las conexiones (RN-03).** Se valida y se guarda, pero la simulación solo controla la capacidad por nodo. Hacer que también limite cuántas personas cruzan una conexión a la vez. *(Integrante 3 con Integrante 2)*
6. **Tipos de desastre (módulo 12, RN-20).** Elegir el desastre al iniciar la simulación. Propuesta:
   - **Terremoto:** colapsa pasillos y escaleras al azar, con réplicas; mucho estrés al inicio; las personas se mueven más lento.
   - **Incendio:** empieza en un nodo y se propaga a los vecinos cada cierto número de pasos; bloquea los nodos quemados; el estrés sube por cercanía al fuego.
   - **Inundación:** el agua sube desde el piso 1 y bloquea los nodos de los pisos bajos poco a poco; empuja a la gente hacia arriba.
   - **Fuga de gas:** marca una zona como peligrosa que crece con el tiempo; no la bloquea del todo, pero eleva el estrés y la velocidad baja.

   Estructura sugerida: `simulacion/Desastre.java` (clase base con `paso(motor)`), una clase por tipo, `TipoDesastre.java` (enum para el selector) y una lista desplegable en "Configurar parámetros de simulación". Cada desastre lleva su probabilidad, intensidad y semilla. *(Integrante 3, con `vista` del Integrante 2 para el selector)*

Formato `.txt` (cuadrícula, ver `recursos/planos/edificio.txt`):
```
#PISO 1      <- (opcional con un solo piso) cada piso es un bloque de filas del mismo tamaño
HHHPPSS      <- filas del piso (todas con el mismo largo; al menos una S en todo el edificio)
H..P.E.      <- '.' = sin nodo, minúscula = bloqueado
#PISO 2
H..P.E.
#PERSONAS    <- (opcional) luego líneas: fila columna TIPO SALUD  (un piso)
1 0 1 ADULTO SANO   <- o: piso fila columna TIPO SALUD (varios pisos; el piso empieza en 1)
```

Formato `.json` (ver `recursos/planos/edificio_ids.json`; el `.xml` equivale):
```
{ "modo": "grafo",            // "cuadricula" = vecinos de la cuadrícula; "grafo" = solo las conexiones listadas
  "filas": 3, "columnas": 6,  // opcional
  "pisos": 1,                  // opcional
  "nodos": [ {"id": "H1", "tipo": "HABITACION", "piso": 1, "x": 1, "y": 1, "bloqueado": false} ],  // piso opcional (1 por defecto)
  "conexiones": [ {"origen": "H1", "destino": "P1", "tiempo": 1, "capacidad": 3} ],
  "personas": [ {"nodo": "H1", "tipo": "ADULTO", "salud": "SANO"} ] }
```
Tipos de nodo: `HABITACION`, `PASILLO`, `ESCALERA`, `SALIDA`. Tipos de persona: `ADULTO`, `NINO`, `MOVILIDAD`, `RESCATE`. Salud: `SANO`, `HERIDO`, `ENFERMO`, `INCAPACITADO`. Un plano inválido se rechaza listando todos los errores y el edificio actual no cambia (RN-15).

Compilar, probar y ejecutar (JDK 11 o superior), desde la raíz del repo:
```
javac -d out $(find src test -name "*.java")
java -cp out evacua.PruebasEstructuras
java -cp out evacua.PruebasPlanos
java -cp out evacua.PruebasPersistencia
java -cp out evacua.PruebasPisos
java -cp out evacua.PruebasCongestion
java -cp out evacua.Main
```
En NetBeans: crear un proyecto Java con `src` como carpeta de fuentes y ejecutar `evacua.Main`.

## 8. Instrucciones para una IA que trabaje en este proyecto

1. **Respeta las reglas de negocio (RN-01 a RN-20)**; cítalas por código al implementar o revisar algo.
2. **Lenguaje y UI:** Java; interfaz solo con Swing.
3. **Estructuras propias:** el curso exige implementar las estructuras de datos (listas, pila, cola, heap); no las sustituyas por `java.util` en los módulos donde se piden propias, salvo que el equipo lo indique.
4. **Respeta los límites de módulo** de la sección 6; una tarea corresponde a un integrante. Los puntos de contacto son:
   - Integrante 2 → expone *recálculo de ruta desde posición actual* al motor, y consume el estado de la simulación para dibujar y generar el mapa de calor.
   - Integrante 3 → provee bitácora e historial a la exportación (Integrante 1), activa el recálculo ante eventos y es dueño del motor de simulación.
   - Integrante 1 → consume los resultados del motor para reportes, comparación y persistencia.
5. **A\* debe ser admisible** (RN-16): convierte la distancia heurística a tiempo de recorrido para no sobreestimar.
6. **Determinismo:** los eventos aleatorios deben usar la semilla configurable para poder repetir una simulación.
7. **Validar antes de modificar:** la importación de planos y el guardado de escenarios validan primero (RN-10, RN-15) y nunca dejan el edificio en un estado a medias.
8. **Bitácora inmutable** (RN-11): solo se agrega, nunca se edita.
9. **Valores configurables:** tiempo límite, umbral de estrés, probabilidad de eventos, semilla y capacidades no deben quedar fijos en el código.
10. Idioma del proyecto: **español** (nombres de dominio, mensajes y documentación).
