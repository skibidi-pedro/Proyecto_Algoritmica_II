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

Responsable de todo lo que **guarda o saca datos** del programa y de las estructuras propias sobre las que se apoya el resto.

**1. Estructuras propias (`estructuras/`)**

| Clase | Idea | Dónde se usa |
|---|---|---|
| `ListaSimple<T>` | Nodos enlazados hacia adelante: `agregarInicio`, `agregarFinal`, `obtener`, `quitarInicio`, `eliminar`, `contiene` | Disponible como estructura de apoyo (se prueba en `PruebasEstructuras`; hoy no la usa otra clase) |
| `ListaDoble<T>` | Enlaces a ambos lados: `agregarInicio/Final`, `primero`, `ultimo`, `quitarInicio/Final`, `alReves()` | `Bitacora` de cada persona (se recorre de inicio a fin o al revés) |
| `Pila<T>` | LIFO: `apilar`, `desapilar`, `cima` | `HistorialEdicion`: cada cambio guarda una copia del edificio y **Deshacer** la desapila |
| `Cola<T>` | FIFO, implementa `Iterable`: `encolar`, `desencolar`, `frente`, `tamano` | `MotorSimulacion.salidas`: cada persona que sale se encola, y de ahí sale el CSV en orden de salida |
| `ColaPrioridad<T>` | Heap binario con comparador (`subir`/`bajar`): `agregar`, `sacar`, `ver` | `AlgoritmoRuta` (Dijkstra y A*) |

Ninguna usa `java.util`. Se prueban en `test/evacua/PruebasEstructuras.java`.

**2. Guardar y cargar escenarios en archivo (`persistencia/GestorEscenarios`)**

- `aTexto(edificio, conPersonas)`: genera el `.txt`. Escribe un bloque de filas por piso (una letra por nodo: `H` habitación, `P` pasillo, `E` escalera, `S` salida, `.` vacío), una línea `#PISO n` entre pisos y, si `conPersonas`, una sección `#PERSONAS` con una línea por persona (`piso fila col TIPO SALUD`). Una letra **en minúscula** significa nodo bloqueado.
- `aJson(edificio)`: genera el `.json` con `filas`, `columnas`, `pisos`, `modo` (`cuadricula` o `grafo`), la lista de `nodos` (id, tipo, piso, x, y, bloqueado), las `conexiones` (origen, destino, tiempo, capacidad, bloqueada) y las `personas`. Es el formato que vuelve a leer `ParserJson`.
- `guardar(archivo, edificio)`: escribe `.json` si el nombre termina en `.json`; en cualquier otro caso escribe `.txt`.
- `leer(archivo)`: lee el archivo como texto UTF-8 y se lo pasa a `ParserPlano.leer(contenido, nombre)` (Integrante 2), que elige el parser por la extensión. Si algo falla lanza `IllegalArgumentException` con el motivo y el edificio actual no cambia.
- `validarParaGuardar(edificio)` (RN-10): devuelve la lista de errores (falta una salida, o una persona no está en un nodo válido). Si no está vacía, no se escribe nada.

**3. Base de datos de escenarios (`persistencia/BaseDatos`)**

No usa SQL ni servidor: son archivos dentro de la carpeta `datos/` (que está en `.gitignore`).

| Método | Qué hace con los archivos |
|---|---|
| `guardar(nombre, edificio)` | Valida con `validarParaGuardar` y escribe `datos/escenarios/<nombre>.json` (crea la carpeta si no existe) |
| `cargar(nombre)` | Lee ese `.json` con `ParserPlano.leer`; si no existe lanza un error claro |
| `listar()` | Lista los `*.json` de `datos/escenarios/` sin la extensión y en orden alfabético |
| `eliminar(nombre)` | Borra el `.json` |
| `registrarResultado(linea)` | Agrega una línea a `datos/resultados.csv`; si el archivo no existe, escribe antes la cabecera |
| `resultados()` | Devuelve las líneas de `datos/resultados.csv` |

El nombre del escenario solo admite letras, números, espacio, punto y guion (hasta 60 caracteres), así que un nombre como `../x` se rechaza y nunca se escribe fuera de `datos/`.

**4. Comparar escenarios (`reportes/ComparadorEscenarios`)**

- `simular(nombre, edificio, params)`: crea un `MotorSimulacion` sobre `edificio.copia()` (el original no cambia) con `params.copia()`, llama a `iniciar()` y repite `paso()` hasta que termina. Devuelve una `Fila` con algoritmo, semilla, personas, evacuadas, ticks, tiempo promedio y estrés promedio.
- `compararGuardados(bd, nombres, params)`: por cada nombre hace `bd.cargar` → `simular` → `bd.registrarResultado(fila.aCsv())`. Así cada comparación queda guardada.
- `aTexto(filas)`: arma la tabla que se muestra en pantalla.

Ejemplo (valores ilustrativos) de `datos/resultados.csv`:

```csv
escenario,algoritmo,semilla,personas,evacuadas,ticks,tiempo_promedio,estres_promedio
simple,A*,42,9,9,41,22.33,1.80
ids,A*,42,5,5,17,10.20,0.40
```

**5. Cola de salidas → `salidas.csv` (`reportes/ExportadorSalidas`)**

- El motor (Integrante 3) crea un `RegistroSalida` inmutable (orden, id, tipo, salud, tiempo, estrés, asistida, pasos, recálculos) y lo encola cuando alguien llega a una salida.
- `exportar(motor, archivo)` desencola cada registro, escribe su línea y **lo vuelve a encolar**, de modo que la cola queda igual y se puede exportar varias veces. Crea la carpeta destino si hace falta.
- Se llama de dos formas: automáticamente al terminar la simulación (`motor.alTerminar` → `VentanaPrincipal.guardarSalidasCsv` → `datos/salidas.csv`) y con el botón "Exportar salidas (CSV)" donde el usuario elija.

Ejemplo (valores ilustrativos) de `datos/salidas.csv`:

```csv
orden_salida,id,tipo,salud,tiempo_salida,estres_al_salir,asistida,pasos,recalculos
1,3,ADULTO,SANO,9,0.0,false,9,1
2,1,NINO,SANO,14,1.2,false,11,0
3,5,MOVILIDAD,HERIDO,31,3.5,true,18,2
```

La columna `orden_salida` sale de la posición en la cola; `tiempo_salida` es el tick en que cruzó la salida.

**6. Reportes (`reportes/ReporteSimulacion`, `ExportadorIndividual`)**

- `ReporteSimulacion.general(motor)`: texto con algoritmo, semilla, ticks, personas evacuadas y no evacuadas, tiempo y estrés promedio, nodos explorados, los **3 nodos con más esperas** (cuellos de botella, de `Nodo.esperas`) y una línea por persona.
- `ReporteSimulacion.individual(persona)`: estado final, contadores (`pasos` y los de `HistorialEventos`), la **bitácora** recorrida de inicio a fin (`RegistroBitacora`: paso, nodo, estado del nodo) y la **línea de tiempo** (`persona.hist`, con eventos como "ruta bloqueada", "congestión", "EVACUADA").
- `ExportadorIndividual.exportar(persona, archivo)` escribe ese texto; `exportarTodos(motor, carpeta)` crea un `reporte_P<id>.txt` por persona, haya evacuado o no.

**7. `Main`**: arranca la aplicación con `VentanaPrincipal`.

#### Integrante 2: edificio, rutas, importación e interfaz

Responsable del **modelo del edificio**, de **encontrar caminos**, de **leer y validar planos** y de lo que el usuario ve y toca.

**1. Modelo del edificio (`edificio/`)**

- `Edificio` guarda una matriz `n[filas][cols]` de `Nodo` (tipo, bloqueado, `ocupacion`, `visitas`, `esperas`). Los pisos van apilados: el piso k ocupa las filas `k*filasPorPiso` a `(k+1)*filasPorPiso - 1` (`piso(f)` devuelve el número de piso de una fila). El id de un nodo es `fila * cols + col`.
- Edición: `crearNodo`, `modificarNodo`, `eliminarNodo`, `bloquear(f, c, valor)`, `rect` (pinta un rectángulo), `agregar` (una persona), y para conexiones explícitas `conectar`, `desconectar`, `bloquearConexion`. `copia()` duplica todo para simular sin tocar el original; `copiaConPisos(n)` lo hace con más pisos (botón "+ Agregar piso").
- `Grafo.desde(edificio)` arma la lista de adyacencia `ady[]` con `Arista` (destino, tiempo, capacidad):
  - **Modo cuadrícula** (por defecto): vecinos arriba, abajo, izquierda y derecha dentro del mismo piso, con tiempo 1 (3 si el destino es escalera). Las escaleras unen la misma fila y columna de pisos consecutivos, con tiempo 3.
  - **Modo grafo** (`conexionesExplicitas`): solo cuentan las `Conexion` del plano. Cada una es bidireccional, con tiempo y capacidad.
  - Los bloqueos no se copian al grafo: se leen en vivo (`Nodo.bloqueado`, `Arista.bloqueada()`), así un bloqueo nuevo se nota sin reconstruirlo.
  - `factorTiempo` es el menor tiempo por unidad de distancia del grafo; lo usa A* para no sobreestimar.
- `HistorialEdicion` (sobre `Pila`): `guardar(actual)` antes de cada cambio, `deshacer()` devuelve el edificio anterior y `puedeDeshacer()`.

**2. Rutas (`rutas/`)**

- `AlgoritmoRuta.buscar(edificio, grafo, filaIni, colIni, metas[, evitar])`: es el algoritmo común. Usa la cola de prioridad con `f = costo acumulado + h`, ignora aristas bloqueadas, nodos no caminables y los que cumpla el predicado `evitar`, y se detiene al sacar el primer nodo que es meta. Devuelve un `ResultadoRuta` con `ruta` (lista de nodos; `null` si no hay camino), `costo`, `explorados` y `nanos` (tiempo de cálculo).
- `Dijkstra`: `h = 0`. `AEstrella`: `h = Heuristica.aTiempo(...)`, es decir, distancia Manhattan en el piso más la diferencia de pisos, multiplicada por `factorTiempo` (admisible, RN-16). Por eso ambos dan el mismo costo, pero A* explora menos nodos.
- `BuscadorRutas.buscar(..., astar)`: construye el `Grafo` y elige el algoritmo. `RecalculadorRutas.desdePosicion` busca desde donde está la persona ahora (RN-07), y `evitandoCongestion` pasa un `evitar` que descarta los nodos llenos (salvo las salidas).

**3. Importación y validación de planos (`importacion/`)**

Flujo: `ParserPlano.leer(contenido, nombre)` → según la extensión:

- **`.txt`**: `deTexto` separa los pisos por las líneas `#PISO`, lee cada carácter con `TipoNodo.de` (minúscula = bloqueado) y lee `#PERSONAS`. Con un solo piso la línea de persona es `fila col TIPO SALUD`; con varios es `piso fila col TIPO SALUD`.
- **`.json`**: `JsonMini` (analizador propio, sin librerías) → `ParserJson.parse` → `PlanoDatos`.
- **`.xml`**: `ParserXml.parse` (DOM de la librería estándar) → `PlanoDatos`.

Los parsers no validan: solo llenan `PlanoDatos`. Luego `ValidadorPlano.validar(plano)` (RN-15) recorre todo y devuelve **todos** los errores a la vez: plano sin nodos, id faltante o repetido, tipo inválido, coordenadas inválidas o fuera del tamaño declarado, dos nodos en la misma celda, piso inválido, falta de salida, conexión con origen o destino inexistente, a sí mismo, con tiempo ≤ 0 o sin capacidad ≥ 1, y personas con nodo, tipo o salud inválidos. Si la lista no está vacía, `ConstructorEdificio.construir` lanza `IllegalArgumentException` con todos los mensajes y **no cambia nada**; si está vacía, construye el `Edificio`. `recursos/planos/edificio_error.json` está hecho a propósito para probar esto.

**4. Interfaz (`vista/`)**

- `VentanaPrincipal` (título "EVACUA") agrupa los botones por tarea y es el único que toca archivos y diálogos:

| Botón | Código que llama | Datos |
|---|---|---|
| Cargar / Guardar escenario | `GestorEscenarios.leer` / `guardar` | `.txt` |
| Importar plano del edificio | `GestorEscenarios.leer` | `.json`, `.xml` o `.txt` (muestra los errores si es inválido) |
| Guardar / Cargar de base de datos | `BaseDatos.guardar` / `cargar` | `datos/escenarios/*.json` |
| Comparar escenarios guardados | `ComparadorEscenarios.compararGuardados` | `datos/resultados.csv` |
| Iniciar emergencia | crea `MotorSimulacion` y un `Timer` que llama a `paso()` | edificio en memoria |
| Exportar salidas (CSV), reportes | `ExportadorSalidas`, `ExportadorIndividual` | `.csv`, `reporte_P<id>.txt` |

- `PanelEdificio` dibuja **un piso a la vez** (el que elige el selector de pisos). Cada herramienta (`Herramienta`) actúa al hacer clic o arrastrar; antes de cada cambio la ventana llama a `historial.guardar(mapa)`, y Deshacer solo funciona en modo edición.
- **Mapa de calor**: mientras corre la simulación pinta `ocupacion / capacidad` de cada nodo en ese tick, así que una casilla se apaga cuando la persona se va. Al terminar pinta `visitas` acumuladas, normalizadas por el máximo.
- La lista **"Salieron (n)"** se arma leyendo `motor.salidas` (la misma cola del CSV).
- Cuando se bloquea un nodo con la simulación en marcha, la ventana llama a `motor.alBloquear(f, c)`; por eso no existe un botón "Recalcular ruta".

#### Integrante 3: personas, comportamiento y simulación

Responsable de **quién** se evacua, **cómo se comporta** y del **bucle de simulación** que produce los datos que luego exportan los otros dos.

**1. Personas (`personas/`)**

- `Persona` (abstracta) tiene posición `(f, c)`, `salud`, `estres` (0–100, con `aumentarEstres` y `reducirEstres` limitados por `NivelEstres.limitar`), `ruta` (lista de nodos que le faltan), `objetivo`, `acum` (movimiento acumulado), `esperando`, `pasos`, `evacuada`/`tEvac`/`asistida`, `hist` (línea de tiempo en texto), `bitacora` y `historial`. `Persona.crear` devuelve `Adulto`, `Nino`, `MovilidadReducida` o `PersonalRescate` según `TipoPersona`.
- Velocidad base por tipo: Adulto 1.0, Niño 0.8, Movilidad reducida 0.5, Rescate 1.2. Se multiplica por la de la salud: Sano 1.0, Enfermo 0.8, Herido 0.6, Incapacitado 0.
- `Bitacora` (sobre `ListaDoble`): `registrar(paso, fila, col, estadoNodo)` solo agrega, `reiniciar()` se usa al iniciar una simulación nueva. `RegistroBitacora` es inmutable. `HistorialEventos` cuenta `bloqueosSufridos`, `recalculos` y `esperaCuelloBotella` (RN-13).
- `PersonalRescate.elegirObjetivo(todas)` (RN-17): busca la persona **incapacitada** más cercana (distancia Manhattan) que no esté evacuada ni asistida; si no hay, la persona prioritaria más cercana (niño, movilidad reducida o incapacitado). Cuando la alcanza, `MotorSimulacion.chequearRescate` marca `asistida = true` y recalcula las rutas de ambos. Una persona incapacitada no se mueve hasta que la asisten (entonces avanza a 0.5).

**2. Parámetros y reglas de capacidad (`simulacion/`)**

- `ParametrosSimulacion`: `prob` de evento por tick (0.03), `semilla` (42), `limite` de ticks (300), `umbral` de estrés (8), `capacidad` por nodo (2), `astar` (true) y `esperaRecalculo` (2 ticks). Los valores por defecto son esos y se cambian con el botón "Configurar parámetros". `copia()` sirve para comparar sin alterar los originales.
- `ControlCapacidad.estado(nodo, capacidad)` devuelve `BLOQUEADO`, `CONGESTIONADO` (si `ocupacion >= capacidad`) o `LIBRE`; ese estado es el que se escribe en la bitácora (RN-08).
- `EventoAleatorio.colapsarPasillo(edificio, random)` (RN-18): elige al azar un pasillo libre y lo bloquea. Usa el `Random` del motor con la semilla, por eso el mismo escenario con la misma semilla da el mismo resultado.

**3. Qué hace `MotorSimulacion.paso()` en cada tick**

Antes, `iniciar()` deja a todos en estado inicial (limpia bitácora, historial, estrés y la cola `salidas`), registra la posición de partida en la bitácora y calcula la primera ruta de cada uno con `asignarRuta`. Después, en cada `paso()`:

1. `tick++`. Con probabilidad `prob` llama a `generarEvento()`: colapsa un pasillo, cuenta con `contarBloqueo` a quién le afecta (el nodo está en su `ruta`) y llama a `recalcularTodas()`.
2. Pone `ocupacion = 0` en todos los nodos y la recuenta con las personas que siguen dentro. Esa ocupación es la que lee el mapa de calor en vivo.
3. Para cada persona no evacuada calcula la velocidad: `tipo.vel × salud.vel`; si el estrés supera el umbral, ×0.6. Si hay un nodo bloqueado al lado, el estrés sube 0.1. Si la velocidad es 0, el estrés sube 0.3 y se pasa a la siguiente.
4. Suma la velocidad a `acum`; por cada unidad completa intenta dar un paso:
   - **Ruta vacía** → `asignarRuta` otra vez; si sigue vacía, el estrés sube 0.5.
   - **Siguiente nodo bloqueado** → recalcula desde donde está (`bloqueosSufridos++`, `recalculos++`).
   - **Siguiente nodo lleno** (y no es salida) → espera: estrés +1, `Nodo.esperas++`, `esperaCuelloBotella++`. Si lleva `esperaRecalculo` ticks esperando, `rodearCongestion` busca otra ruta que evite los nodos llenos (una vez por espera). Si no hay otra, sigue esperando.
   - **Si no, avanza**: actualiza `(f, c)`, quita el nodo de la ruta, `pasos++`, escribe en `hist` y en la bitácora (`ControlCapacidad.estado`) y baja el estrés 0.2 si se movió.
   - Si el nodo es **salida** y no tiene objetivo de rescate, marca `evacuada`, guarda `tEvac` y hace `salidas.encolar(new RegistroSalida(...))`.
5. Suma 1 a `visitas` de la casilla de cada persona que sigue dentro (o que acaba de salir). Esa es la ocupación acumulada del mapa de calor final.
6. Si todas salieron o `tick >= limite`, marca `terminado`, anota "NO EVACUADA (tiempo limite)" a las que faltan y llama a `alTerminar`, que escribe `datos/salidas.csv`.

**4. Cómo se conecta con los datos**

| Dato | Entra o sale | Dónde |
|---|---|---|
| Personas, tipo y salud iniciales, bloqueos | **Entra** del `.txt`/`.json`/`.xml` (ya validado por el Integrante 2) | `Edificio.personas`, `Nodo.bloqueado` |
| `RegistroSalida` | **Sale** a la cola `salidas` y luego a `salidas.csv` | `MotorSimulacion` → `ExportadorSalidas` |
| Bitácora, `hist` e historial | **Salen** a `reporte_P<id>.txt` | `Persona` → `ReporteSimulacion.individual` |
| `Nodo.esperas`, `exploradosTotal`, `evacuadas()`, `promedioEvac()`, `promedioEstres()` | **Salen** al reporte general y a `resultados.csv` | `ReporteSimulacion.general`, `ComparadorEscenarios.simular` |

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
