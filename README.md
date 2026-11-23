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
| **Estado del nodo** | Libre, dañado, peligroso, congestionado o bloqueado. Daño y peligro pueden coexistir. |
| **Conexión** | Arista del grafo (puerta, pasillo, escalera) con **tiempo de recorrido > 0** y **capacidad máxima**. |
| **Persona** | Clase base con tipos derivados: Adulto, Niño, Persona con movilidad reducida, Personal de rescate. Cada uno con velocidad y comportamiento propios. |
| **Estrés** | Valor de 0 a 100 por persona. Se acumula por daño, peligro, proximidad, espera por un nodo lleno y bloqueos que interrumpen la ruta; al pasar un umbral reduce la velocidad o cambia el comportamiento. |
| **Bitácora** | Registro inmutable por persona: cada nodo visitado + paso de simulación (timestamp) + estado del nodo en ese instante. Lista doble o pila. |
| **Escenario** | Edificio + personas + bloqueos, guardado en base de datos para repetir o comparar simulaciones. |
| **Desastre** | Tipo de emergencia que se simula: Terremoto, Incendio, Inundación, Fuga de gas, etc. Cada tipo define cómo se bloquean zonas, cuánto estrés genera y cómo evoluciona (RN-20). |
| **Mapa de calor** | Ocupación instantánea durante la simulación y visitas acumuladas al terminar. |

## 3. Funcionalidades (módulos)

1. **Modelado del edificio.** CRUD de nodos (habitaciones, pasillos, escaleras, salidas) y conexiones entre ellos en un grafo, con tiempo de recorrido y capacidad.
2. **Gestión de personas.** Registrar personas en un nodo inicial; jerarquía de clases desde `Persona`, con trazabilidad, estrés e historial de eventos; lógica de prioridades diferenciada para rescate.
3. **Cálculo de rutas.** Ruta más rápida a la salida disponible más cercana; evaluación en cada tick; **A\*** (heurística euclidiana o Manhattan) como alternativa a **Dijkstra**, comparando nodos explorados y tiempo de cómputo.
4. **Bloqueo de zonas y peligros.** Marcar nodos/conexiones como bloqueados, dañados o peligrosos y excluirlos del cálculo mientras dure el bloqueo.
5. **Simulación.** Emergencia paso a paso; tiempo de salida por persona, cuellos de botella y personas que no evacúan dentro del tiempo límite.
6. **Escenarios y persistencia.** Guardar/cargar escenarios en una base de datos.
7. **Reportes y estadísticas.** Por simulación: tiempo total, tiempo promedio, evacuados/no evacuados, nodos más congestionados. Comparación entre escenarios del mismo edificio. Exportación de un reporte individual por persona con su línea de tiempo.
8. **Visualización (Swing).** Edificio, personas y rutas en tiempo real; mapa de calor al terminar.
9. **Trazabilidad y comportamiento de `Persona`.** Bitácora, estrés dinámico (0–100), historial de eventos (bloqueos sufridos, recálculos, tiempo de espera en cuellos de botella) y archivo individual con la línea de tiempo.
10. **Importación y exportación.** El selector de archivos admite escenarios TXT, JSON y XML.
11. **Optimización y simulación estocástica.** Rescate prioriza niños y personas con movilidad reducida antes de evacuar; eventos aleatorios (colapso de pasillos) que fuerzan recálculo; acumulación de ocupación para el mapa de calor.
12. **Tipos de desastre.** Al iniciar la emergencia se elige el tipo de desastre (Terremoto, Incendio, Inundación, Fuga de gas, etc.). Cada tipo tiene su propia dinámica: qué zonas bloquea, cómo se propaga o evoluciona en el tiempo, cuánto estrés genera y si afecta la velocidad. Los eventos aleatorios del módulo 11 pasan a depender del desastre elegido.

## 4. Reglas del negocio

Los valores numéricos son **configurables**; los de abajo son valores por defecto.

| Código | Regla | Descripción |
|---|---|---|
| RN-01 | Unicidad | Cada nodo, persona y conexión tiene un identificador único. |
| RN-02 | Tipos de nodo | Habitación, Pasillo, Escalera o Salida. Solo Salida es destino válido. |
| RN-03 | Conexión válida | Tiempo de recorrido > 0 y capacidad máxima definida. |
| RN-04 | Estado del nodo | El bloqueo impide el tránsito. El daño reduce la capacidad en uno (mínimo uno) y el peligro encarece el ingreso. |
| RN-05 | Bloqueo de zonas | Un nodo o conexión bloqueada se excluye del cálculo mientras dure; al desbloquearse vuelve a estar disponible. |
| RN-06 | Cálculo de rutas | Ruta hacia la salida disponible más cercana según tiempo de recorrido, evitando bloqueos. |
| RN-07 | Recálculo dinámico | Cada persona reevalúa su ruta desde la posición actual en cada tick y conserva el recorrido si sigue siendo óptimo. |
| RN-08 | Capacidad de nodos | Al alcanzar la capacidad efectiva se impiden nuevas entradas. La salida de ocupantes libera plazas en el mismo tick. |
| RN-09 | Tiempo límite | Quien no llega a una salida dentro del tiempo límite se registra como **No evacuada**. |
| RN-10 | Persistencia | Un escenario solo se guarda si hay al menos una salida y todas las personas están en un nodo válido. |
| RN-11 | Bitácora | Cada nodo visitado se registra con paso de simulación y estado del nodo. Los registros **no se modifican** una vez escritos. |
| RN-12 | Estrés | Valor 0–100; acumula los factores ambientales del nodo y las interrupciones por bloqueo; al superar un umbral configurable reduce velocidad o altera comportamiento. |
| RN-13 | Historial de eventos | Contabiliza bloqueos sufridos, recálculos y tiempo total de espera en cuellos de botella. |
| RN-14 | Exportación individual | Al terminar se genera un archivo por persona con su línea de tiempo, haya evacuado o vencido el tiempo. |
| RN-15 | Importación de planos | Validar TXT, JSON y XML antes de reemplazar el edificio. |
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
│       │   ├── BaseDatos.java                (base embebida: escenarios .txt + resultados.csv)
│       │   └── GestorEscenarios.java         (validar RN-10, guardar y leer el plano .txt)
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
├── recursos/planos/                          plano de ejemplo: edificio.txt (3 pisos, 9 personas)
├── test/evacua/                              PruebasEstructuras, PruebasPlanos, PruebasPersistencia, PruebasPisos, PruebasCongestion
└── docs/                                     informe, diagrama de casos de uso
```

> Todos los archivos de `src/` existen y compilan. Cada integrante sigue siendo dueño de sus paquetes.

### 6.2 Resumen por integrante

| Integrante | Rol | Paquetes en `src/evacua/` | Reglas |
|---|---|---|---|
| 1 | Núcleo, persistencia (guardar y leer el plano `.txt`), reportes e integración | `Main`, `estructuras`, `persistencia`, `reportes` | RN-01, RN-10, RN-14 |
| 2 | Edificio, rutas e interfaz | `edificio`, `rutas`, `vista`, `recursos/planos` | RN-02 a RN-07, RN-16, RN-19 |
| 3 | Personas, comportamiento y motor de simulación | `personas`, `simulacion` | RN-08, RN-09, RN-11, RN-12, RN-13, RN-17, RN-18, RN-20 |

### 6.3 Puntos de contacto entre paquetes

| Quien provee | Clase / operación | Quien la usa |
|---|---|---|
| Integrante 1 | `estructuras.*` (`ColaPrioridad`, `ListaDoble`, ...) | `rutas.AlgoritmoRuta` (Integrante 2) y `personas.Bitacora` (Integrante 3) |
| Integrante 2 | `RecalculadorRutas`, `Dijkstra`, `AEstrella`, `Edificio` | `simulacion.MotorSimulacion` (Integrante 3) |
| Integrante 3 | `Persona` (`bitacora`, `historial`, `estres`) | `reportes.ReporteSimulacion` y `ExportadorIndividual` (Integrante 1) |
| Integrante 3 | `MotorSimulacion` (tiempos, evacuadas, cuellos de botella) | `reportes.*` (Integrante 1) y `vista.*` (Integrante 2) |
| Integrante 3 | `EventoAleatorio` (bloquea un pasillo) | `MotorSimulacion` y su recálculo (Integrante 2) |
| Integrante 1 | `GestorEscenarios` (`leer`, `deTexto`, `guardar`, `validarParaGuardar`) y `BaseDatos` | `vista.VentanaPrincipal` (Integrante 2) y `Main`; el `.txt` leído se convierte en un `edificio.Edificio` (Integrante 2) |

### 6.4 Tareas por integrante

**Integrante 1: Núcleo, persistencia, exportación e integración** (RN-01, RN-10, RN-14)
*Paquetes:* `Main`, `estructuras`, `persistencia`, `reportes`

- Estructuras base: listas simple/doble, pila, cola, heap.
- Clase `Main` y comunicación entre módulos (cargar y guardar escenarios, exportar reportes individuales y salidas, comparar escenarios).
- Guardar y leer el plano `.txt` (`GestorEscenarios.aTexto` / `deTexto`), rechazando los archivos inválidos con un mensaje y sin cambiar el edificio actual.
- Registro, guardado y carga de escenarios en la base de datos embebida (`.txt`) y tabla de resultados `.csv`.
- Exportación individual por persona (usa la bitácora e historial del Integrante 3).
- Comparación de resultados entre escenarios guardados.
- *(Reasignado)* Reportes y estadísticas de cada simulación: tiempo total, tiempo promedio, evacuados/no evacuados y nodos más congestionados (usa los resultados del motor del Integrante 3).
- Configuración del repositorio, integración completa y coordinación de entregas.

**Integrante 2: Edificio, rutas, bloqueos e interfaz** (RN-02, RN-03, RN-04, RN-05, RN-06, RN-07, RN-16, RN-19)
*Paquetes:* `edificio`, `rutas`, `vista`, `recursos/planos`

- Grafo del edificio con CRUD de nodos, estados y capacidades.
- Gestión de bloqueos (marcar/desbloquear) y su exclusión del cálculo de rutas.
- Dijkstra y A\* con comparación de nodos explorados y tiempo.
- Recálculo dinámico desde la posición actual, expuesto como operación reutilizable para el motor de simulación.
- Plano de prueba `recursos/planos/edificio.txt` (3 pisos, 9 personas).
- Pila de "Deshacer" del editor (`HistorialEdicion`) y selector de pisos.
- *(Reasignado)* Interfaz Swing: dibuja edificio, personas y rutas en tiempo real (aprovecha las coordenadas x, y de los nodos).
- *(Reasignado)* Acumulación de ocupación por nodo y mapa de calor en Swing al finalizar la simulación (RN-19).

**Integrante 3: Personas y comportamiento** (RN-08, RN-09, RN-11, RN-12, RN-13, RN-17, RN-18, RN-20)
*Paquetes:* `personas`, `simulacion`

- Jerarquía: adulto, niño, movilidad reducida, personal de rescate.
- En `Persona`: bitácora (lista doble o pila), estrés e historial de eventos.
- Lógica diferenciada del personal de rescate.
- Eventos aleatorios (colapso de pasillos) con probabilidad y semilla configurables; bloquean el elemento y activan el recálculo del Integrante 2.
- Tipos de desastre (Terremoto, Incendio, Inundación, Fuga de gas, etc.): clase base `Desastre` en `simulacion/` con una clase derivada por tipo (ver 7.1, último punto).
- Control de capacidad de nodos (capacidad efectiva, desvío o espera).
- *(Reasignado)* Motor de simulación paso a paso, que integra eventos aleatorios, recálculo de rutas y control de congestión.
- *(Reasignado)* Cálculo de tiempos de evacuación, cuellos de botella y personas no evacuadas dentro del tiempo límite (RN-09).

### 6.5 Cómo funciona cada parte: código y datos

Esta sección explica, por integrante, qué hace el código de cada funcionalidad y cómo lee o escribe los archivos de datos. Primero el mapa general de archivos y después el detalle.

#### Archivos de datos

| Archivo | Formato | Quién lo escribe | Quién lo lee | Para qué |
|---|---|---|---|---|
| Plano o escenario (`recursos/planos/edificio.txt`) | `.txt` en cuadrícula, con pisos y personas | `GestorEscenarios.guardar` (botón "Guardar escenario") | `GestorEscenarios.leer` → `deTexto` (botón "Cargar escenario") | Edificio completo: un carácter por nodo, bloqueos y personas |
| Escenarios guardados (`datos/escenarios/<nombre>.txt`) | `.txt` (el mismo formato) | `BaseDatos.guardar` | `BaseDatos.cargar` y `listar` | Escenarios para repetir o comparar |
| Resultados (`datos/resultados.csv`) | CSV: `escenario,algoritmo,semilla,personas,evacuadas,ticks,tiempo_promedio,estres_promedio` | `BaseDatos.registrarResultado` (lo llama `ComparadorEscenarios.compararGuardados`) | `BaseDatos.resultados` | Historial de comparaciones |
| Salidas (`datos/salidas.csv`) | CSV: `orden_salida,id,tipo,salud,tiempo_salida,estres_al_salir,asistida,pasos,recalculos` | `ExportadorSalidas.exportar` (solo al terminar la simulación, o con el botón "Exportar salidas (CSV)") | Excel o cualquier lector de CSV | Tiempo y estado de quienes salieron, en orden |
| Reporte individual (`reporte_P<id>.txt`) | `.txt` con bitácora y línea de tiempo | `ExportadorIndividual.exportar` y `exportarTodos` | Personas | Trazabilidad de cada persona (RN-14) |

Recorrido de los datos, de la entrada a la salida:

```
archivo .txt
        │  GestorEscenarios.leer → deTexto (valida el formato)
        ▼
Edificio (grafo + personas)
        │ copia()
        ▼
MotorSimulacion.paso() una vez por tick
  ├─► Persona.bitacora  ──► reporte_P<id>.txt
  ├─► motor.salidas (Cola) ──► salidas.csv
  └─► Nodo.ocupacion / visitas ──► mapa de calor
GestorEscenarios.aTexto ──► BaseDatos ──► ComparadorEscenarios ──► resultados.csv
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
- `guardar(archivo, edificio)`: escribe el `.txt` con `aTexto(edificio, true)`.
- `leer(archivo)`: lee el archivo como texto UTF-8 y se lo pasa a `deTexto`. Si algo falla lanza `IllegalArgumentException` con el motivo y el edificio actual no cambia.
- `deTexto(contenido)`: separa los pisos por las líneas `#PISO`, convierte cada carácter con `TipoNodo.de` (minúscula = bloqueado) y lee las líneas de `#PERSONAS`. Con un solo piso la línea de persona es `fila col TIPO SALUD`; con varios es `piso fila col TIPO SALUD`. Rechaza el archivo con un mensaje claro si no hay plano, si las filas o pisos no tienen el mismo tamaño, si hay un carácter inválido, si no hay ninguna salida (`S`), si una línea de persona está mal escrita o si una persona no está sobre un nodo libre.
- `validarParaGuardar(edificio)` (RN-10): devuelve la lista de errores (falta una salida, o una persona no está en un nodo válido). Si no está vacía, no se escribe nada.

**3. Base de datos de escenarios (`persistencia/BaseDatos`)**

No usa SQL ni servidor: son archivos dentro de la carpeta `datos/` (que está en `.gitignore`).

| Método | Qué hace con los archivos |
|---|---|
| `guardar(nombre, edificio)` | Valida con `validarParaGuardar` y escribe `datos/escenarios/<nombre>.txt` (crea la carpeta si no existe) |
| `cargar(nombre)` | Lee ese `.txt` con `GestorEscenarios.deTexto`; si no existe lanza un error claro |
| `listar()` | Lista los `*.txt` de `datos/escenarios/` sin la extensión y en orden alfabético |
| `eliminar(nombre)` | Borra el `.txt` |
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

#### Integrante 2: edificio, rutas e interfaz

Responsable del **modelo del edificio**, de **encontrar caminos** y de lo que el usuario ve y toca.

**1. Modelo del edificio (`edificio/`)**

- `Edificio` guarda una matriz `n[filas][cols]` de `Nodo` (tipo, bloqueado, `ocupacion`, `visitas`, `esperas`). Los pisos van apilados: el piso k ocupa las filas `k*filasPorPiso` a `(k+1)*filasPorPiso - 1` (`piso(f)` devuelve el número de piso de una fila). El id de un nodo es `fila * cols + col`.
- Edición: `crearNodo`, `modificarNodo`, `eliminarNodo`, `bloquear(f, c, valor)`, `rect` (pinta un rectángulo), `agregar` (una persona), y para conexiones explícitas `conectar`, `desconectar`, `bloquearConexion`. `copia()` duplica todo para simular sin tocar el original; `copiaConPisos(n)` lo hace con más pisos (botón "+ Agregar piso").
- `Grafo.desde(edificio)` arma la lista de adyacencia `ady[]` con `Arista` (destino, tiempo, capacidad):
  - **Modo cuadrícula** (por defecto): vecinos arriba, abajo, izquierda y derecha dentro del mismo piso, con tiempo 1 (3 si el destino es escalera). Las escaleras unen la misma fila y columna de pisos consecutivos, con tiempo 3.
  - **Modo grafo** (`conexionesExplicitas = true`, se conserva en el archivo con `#GRAFO_EXPLICITO`): solo cuentan las `Conexion` creadas con `conectar`. Cada una es bidireccional, con tiempo y capacidad.
  - Los bloqueos no se copian al grafo: se leen en vivo (`Nodo.bloqueado`, `Arista.bloqueada()`), así un bloqueo nuevo se nota sin reconstruirlo.
  - `factorTiempo` es el menor tiempo por unidad de distancia del grafo; lo usa A* para no sobreestimar.
- `HistorialEdicion` (sobre `Pila`): `guardar(actual)` antes de cada cambio, `deshacer()` devuelve el edificio anterior y `puedeDeshacer()`.

**2. Rutas (`rutas/`)**

- `AlgoritmoRuta.buscar(edificio, grafo, filaIni, colIni, metas[, evitar])`: es el algoritmo común. Usa la cola de prioridad con `f = costo acumulado + h`, ignora aristas bloqueadas, nodos no caminables y los que cumpla el predicado `evitar`, y se detiene al sacar el primer nodo que es meta. Devuelve un `ResultadoRuta` con `ruta` (lista de nodos; `null` si no hay camino), `costo`, `explorados` y `nanos` (tiempo de cálculo).
- `Dijkstra`: `h = 0`. `AEstrella`: `h = Heuristica.aTiempo(...)`, es decir, distancia Manhattan en el piso más la diferencia de pisos, multiplicada por `factorTiempo` (admisible, RN-16). Ambos dan el mismo costo óptimo. A* puede explorar menos nodos.
- `BuscadorRutas.buscar(..., astar)`: construye el `Grafo` y elige el algoritmo. `RecalculadorRutas.desdePosicion` busca desde donde está la persona ahora (RN-07), y `evitandoCongestion` pasa un `evitar` que descarta los nodos llenos.

**3. Interfaz (`vista/`)**

- `VentanaPrincipal` (título "EVACUA") agrupa los botones por tarea y es el único que toca archivos y diálogos:

| Botón | Código que llama | Datos |
|---|---|---|
| Cargar / Guardar escenario | `GestorEscenarios.leer` / `guardar` | `.txt` (si el archivo es inválido muestra el motivo y no cambia nada) |
| Guardar / Cargar de base de datos | `BaseDatos.guardar` / `cargar` | `datos/escenarios/*.txt` |
| Comparar escenarios guardados | `ComparadorEscenarios.compararGuardados` | `datos/resultados.csv` |
| Iniciar emergencia | crea `MotorSimulacion` y un `Timer` que llama a `paso()` | edificio en memoria |
| Exportar salidas (CSV), reportes | `ExportadorSalidas`, `ExportadorIndividual` | `.csv`, `reporte_P<id>.txt` |

- `PanelEdificio` dibuja **un piso a la vez** (el que elige el selector de pisos). Cada herramienta (`Herramienta`) actúa al hacer clic o arrastrar; antes de cada cambio la ventana llama a `historial.guardar(mapa)`, y Deshacer solo funciona en modo edición.
- **Mapa de calor**: mientras corre la simulación pinta `ocupacion / capacidadEfectiva` de cada nodo en ese tick, así que una casilla se apaga cuando la persona se va. Al terminar pinta `visitas` acumuladas, normalizadas por el máximo.
- La lista **"Salieron (n)"** se arma leyendo `motor.salidas` (la misma cola del CSV).
- Cuando se bloquea un nodo con la simulación en marcha, la ventana llama a `motor.alBloquear(f, c)`; por eso no existe un botón "Recalcular ruta".

#### Integrante 3: personas, comportamiento y simulación

Responsable de **quién** se evacua, **cómo se comporta** y del **bucle de simulación** que produce los datos que luego exportan los otros dos.

**1. Personas (`personas/`)**

- `Persona` (abstracta) tiene posición `(f, c)`, `salud`, `estres` (0–100, con `aumentarEstres` y `reducirEstres` limitados por `NivelEstres.limitar`), `ruta` (lista de nodos que le faltan), `objetivo`, `acum` (movimiento acumulado), `esperando`, `pasos`, `evacuada`/`tEvac`/`asistida`, `hist` (línea de tiempo en texto), `bitacora` y `historial`. `Persona.crear` devuelve `Adulto`, `Nino`, `MovilidadReducida` o `PersonalRescate` según `TipoPersona`.
- Velocidad base por tipo: Adulto 1.0, Niño 0.8, Movilidad reducida 0.5, Rescate 1.2. Se multiplica por la de la salud: Sano 1.0, Enfermo 0.8, Herido 0.6, Incapacitado 0.
- `Bitacora` (sobre `ListaDoble`): `registrar(paso, fila, col, estadoNodo)` solo agrega, `reiniciar()` se usa al iniciar una simulación nueva. `RegistroBitacora` es inmutable. `HistorialEventos` cuenta `bloqueosSufridos`, `recalculos` y `esperaCuelloBotella` (RN-13).
- **CARGAR (RN-17).** Entre niños y personas con movilidad reducida, el rescate prioriza a los incapacitados y después la cercanía. Un rescatista carga a un niño (0,5 plazas, velocidad normal). Dos cargan a una persona con movilidad reducida (1 plaza, mitad de la velocidad del portador más lento). El equipo comparte posición y ruta. Cada portador consume un cruce del enlace por movimiento. En la salida se registra al paciente y cada rescatista disponible puede atender otro objetivo.

**2. Parámetros y reglas de capacidad (`simulacion/`)**

- `ParametrosSimulacion`: `prob` de evento por tick (0.03), `semilla` (42), `limite` de ticks (300), `umbral` de estrés (8), `capacidad` por nodo (2) y `astar` (true). Los valores por defecto son esos y se cambian con el botón "Configurar parámetros". `copia()` sirve para comparar sin alterar los originales.
- `ControlCapacidad.estado(nodo, capacidad)` devuelve el primer estado aplicable en este orden: bloqueado, congestionado, peligroso, dañado, libre; ese estado es el que se escribe en la bitácora (RN-08).
- `EventoAleatorio.colapsarPasillo(edificio, random)` (RN-18): elige al azar un pasillo libre y lo bloquea. Usa el `Random` del motor con la semilla, por eso el mismo escenario con la misma semilla da el mismo resultado.

**3. Qué hace `MotorSimulacion.paso()` en cada tick**

Antes, `iniciar()` deja a todos en estado inicial (limpia bitácora, historial, estrés y la cola `salidas`), registra la posición de partida en la bitácora y calcula la primera ruta de cada uno con `asignarRuta`. Después, en cada `paso()`:

1. `tick++`. El desastre seleccionado usa la probabilidad y la semilla del motor. Cuando bloquea una zona, el motor cuenta a las personas afectadas y recalcula las rutas.
2. Pone `ocupacion = 0` en todos los nodos y la recuenta con las personas que siguen dentro. Esa ocupación es la que lee el mapa de calor en vivo.
3. En el orden de las personas del plano, el motor evalúa la ruta con el grafo compartido. Descarta nodos llenos y conexiones cuyo cupo ya se consumió. El ocupante puede salir de su propio nodo congestionado. Se conserva el recorrido previo si llega a una meta válida con el mismo costo óptimo.
4. La velocidad por tipo, salud y estrés se acumula hasta permitir un paso. Cada movimiento actualiza inmediatamente la ocupación y el cupo compartido de la conexión. Si no hay ruta viable, la persona espera y vuelve a evaluar en el siguiente tick. Al evacuar se libera la plaza de la salida y se registra el resultado en la cola.
5. Suma 1 a `visitas` de la casilla de cada persona que sigue dentro (o que acaba de salir). Esa es la ocupación acumulada del mapa de calor final.
6. Si todas salieron o `tick >= limite`, marca `terminado`, anota "NO EVACUADA (tiempo limite)" a las que faltan y llama a `alTerminar`, que escribe `datos/salidas.csv` y los reportes individuales (RN-14).

**4. Cómo se conecta con los datos**

| Dato | Entra o sale | Dónde |
|---|---|---|
| Personas, tipo y salud iniciales, bloqueos | **Entra** del `.txt` (ya validado por `GestorEscenarios.deTexto`) | `Edificio.personas`, `Nodo.bloqueado` |
| `RegistroSalida` | **Sale** a la cola `salidas` y luego a `salidas.csv` | `MotorSimulacion` → `ExportadorSalidas` |
| Bitácora, `hist` e historial | **Salen** a `reporte_P<id>.txt` | `Persona` → `ReporteSimulacion.individual` |
| `Nodo.esperas`, `exploradosTotal`, `evacuadas()`, `promedioEvac()`, `promedioEstres()` | **Salen** al reporte general y a `resultados.csv` | `ReporteSimulacion.general`, `ComparadorEscenarios.simular` |

## 7. Estado actual del repositorio

Los requisitos funcionales de los tres integrantes están implementados en `src/evacua/` y cubiertos por pruebas. Es Java Swing, sin dependencias externas.

Qué hay hoy (por integrante):
- **Integrante 1:** estructuras propias (lista simple y doble, pila, cola, heap): el heap lo usan las rutas, la lista doble la bitácora, la cola las salidas y la pila el botón "Deshacer" (Ctrl+Z) del editor, que revierte bloquear, agregar un piso o cargar un plano; CSV de salidas (`orden_salida,id,tipo,salud,tiempo_salida,estres_al_salir,asistida,pasos,recalculos`) que se guarda solo al terminar la simulación en `datos/salidas.csv` y con el botón "Exportar salidas (CSV)"; base de datos embebida (`datos/escenarios/*.txt` y `datos/resultados.csv`) con la validación RN-10; comparación de escenarios; exportación de un reporte por persona.
- **Integrante 2:** grafo con conexiones (tiempo > 0 y capacidad), CRUD de nodos y conexiones, bloqueos de nodos y conexiones; Dijkstra y A\* separados, con la heurística convertida a tiempo; recalculador de rutas.
- **Integrante 3:** recálculo por congestión (cada tick busca un camino con plazas y espera si no hay alternativa); jerarquía `Adulto`, `Nino`, `MovilidadReducida`, `PersonalRescate`; bitácora sobre lista doble; estrés de 0 a 100; historial de eventos; rescate que prioriza incapacitados y luego niños o movilidad reducida; eventos aleatorios con semilla; control de capacidad con estado Congestionado.

Pisos: el edificio puede tener varios pisos. El edificio original es el **piso 1** y el selector de la derecha (Piso 1, 2, 3...) cambia el piso que se dibuja; "+ Agregar piso" agrega uno vacío en modo edición. Las **escaleras** (`E`) en la misma fila y columna de pisos consecutivos los conectan (costo 3). Las personas de pisos altos bajan por ellas hasta una salida. El mapa de calor se actualiza por tick (colorea solo los nodos con gente en ese momento, más rojo cuanto más cerca de la capacidad) y, al terminar, muestra la ocupación acumulada (RN-19). Debajo del selector de pisos hay una lista "Salieron" con el id y el tiempo de quienes ya salieron. Mientras corre la simulación, cada vez que se bloquea o desbloquea una zona se recalculan solas las rutas de todos (por eso no hay botón de recalcular).

Limitaciones conocidas:
- La interfaz dibuja un piso a la vez como cuadrícula: las conexiones no se dibujan como líneas.
- El tiempo de una conexión decide la ruta, pero la persona avanza un nodo por unidad de velocidad. Cada cruce consume un cupo de la conexión durante el tick.
- La "base de datos" es un almacén embebido en archivos (carpeta `datos/`), sin servidor.
- El formato de cuadrícula original conserva personas y bloqueos de nodos. Los escenarios con conexiones editadas o zonas dañadas o peligrosas añaden las secciones opcionales descritas en 7.1.

### 7.1 Funcionalidades integradas

1. **Rutas por piso (2.8).** La cuadrícula dibuja los tramos pendientes de cada persona con el color de su tipo. Al cambiar de piso se muestran los tramos correspondientes.
2. **Estados y costos (RN-04, RN-05, RN-16).** El costo de entrada es el tiempo original del enlace multiplicado por el peso del destino (libre 1, dañado 2, peligroso 5, ambos 10) y por 2 si el propio enlace es peligroso. Un enlace dañado o bloqueado sigue cerrado. Todos los factores son al menos uno, por lo que la distancia Manhattan escalada por el menor tiempo por unidad de distancia sigue siendo una cota inferior, incluso con atajos entre pisos.
3. **Edición y rescate.** La interfaz permite cambiar condiciones y bloquear o desbloquear zonas. El editor permite dibujar los cinco tipos de nodo y borrar casillas. Las conexiones se editan mediante el modelo o al importar el plano. Un bloqueo colocado con la herramienta manual es reparable. Cada rescatista en condiciones de actuar puede despejar un bloqueo reparable contiguo por tick mediante una conexión abierta. Al liberar el paso se mantienen daño y peligro. La recogida puede reunir a los portadores desde un nodo conectado si hay cupo en el enlace y en el destino para el equipo completo.
4. **Reportes automáticos (RN-14).** Al terminar desde Swing se crea una carpeta por ejecución en `datos/reportes/`, con un archivo por persona. Incluye a quienes alcanzaron el tiempo límite. La exportación manual sigue disponible.
5. **Capacidad compartida (RN-03, RN-08).** Cada conexión admite como máximo su capacidad de cruces por tick, sumando ambos sentidos. Quien encuentra el cupo lleno espera y registra la demora. El tiempo de recorrido sigue siendo el peso usado para elegir la ruta.
6. **Desastres existentes (RN-07, RN-18, RN-20).** Queda pendiente ampliar su integración con las condiciones combinables. Al iniciar se elige el tipo. En la configuración se ajustan probabilidad de evento por tick, intensidad (1–10) y semilla. El modo clásico conserva el colapso de un pasillo. El terremoto añade estrés inicial, reduce la velocidad y colapsa pasillos o escaleras con cada réplica. El incendio bloquea un foco y se propaga por conexiones disponibles. La inundación empieza en el piso más bajo disponible y prioriza salidas superiores alcanzables para quienes están en pisos afectados. El gas extiende una zona transitable donde aumenta el estrés y disminuye la velocidad. La intensidad limita los nodos afectados por evento en los cuatro desastres.

**Personal de rescate.** Cada incremento de estrés se multiplica por 0,30. Al alcanzar 100, el rescatista termina la carga en curso y se retira definitivamente en esa simulación. Sus rutas omiten el multiplicador por peligro del nodo. Mantienen el costo por daño, el tiempo del enlace y las restricciones de tránsito.

**Estrés ambiental por tick.** Se evalúa la casilla ocupada al comenzar el turno de la persona. Un nodo libre aporta 0, el daño suma 1 y el peligro suma 4. Si la casilla propia está fuera de peligro, tener uno o más vecinos peligrosos ortogonales en el mismo piso suma 1. Esperar por falta de espacio para entrar en el siguiente nodo añade 2. Un bloqueo que interrumpe la ruta asignada suma 3 una vez por evento. Los recálculos rutinarios aportan 0. Los factores aplicables se suman y el estrés total queda entre 0 y 100, sin recuperación automática.

**Gestionar familiares.** Este botón consulta los ocupantes del departamento de origen, incluso después de que crucen su puerta. TXT, JSON y XML guardan la pertenencia. Los planos antiguos la calculan a partir de la casilla inicial. Los rescatistas quedan fuera de los grupos.

Un adulto acompaña a un integrante de su departamento a la vez, con preferencia por los niños. Cuando lo alcanza, permanece con él o lo sigue hasta la salida. Un incapacitado con ayuda familiar puede avanzar a 0,25 casillas por tick antes de los ajustes por estrés o desastre. La carga profesional toma el relevo de la ayuda familiar para niños y personas con movilidad reducida de cualquier departamento. Si un adulto acompaña a una persona con movilidad reducida y alcanza 50 de estrés, continúa solo el resto de esa evacuación.

Para entrar en un nodo, un adulto o una persona con movilidad reducida necesita una plaza completa. Un niño o un rescatista necesita media plaza. Los cupos de las conexiones siguen midiendo cruces por tick.

**Departamentos.** Una puerta (D en TXT, PUERTA en JSON/XML) conecta una habitación y un pasillo por lados de la cuadrícula. Al editar o cargar la estructura, el detector recorre piso, fila y columna. Desde cada puerta válida explora las habitaciones ortogonalmente conectadas en ese piso mediante BFS y la cola propia. La región y su puerta reciben el mismo identificador global (Dep-001, Dep-002…). Los pasillos quedan fuera. Una puerta incompleta se puede seguir editando. Esta versión contempla una puerta por departamento.

«Mostrar / ocultar departamentos» dibuja contornos por departamento y la etiqueta sobre la puerta del piso visible. Se puede combinar con el mapa de calor. El plano de ejemplo está en recursos/planos/departamentos.txt.

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

Los planos antiguos siguen abriendo sin conversión. Este ejemplo completo muestra las secciones opcionales:

```text
#PISO 1
HE
#PISO 2
SE
#CONDICIONES
1:0:0 DANADO
2:0:1 PELIGROSO
#CONEXIONES
1:0:1 2:0:1 2.5 3 PELIGROSO ABIERTA
```

Cada posición usa `piso:fila:columna`. El piso empieza en 1 y la fila y la columna empiezan en 0 dentro de cada piso. La última línea conecta las dos escaleras con tiempo 2.5 y capacidad 3. Las condiciones admitidas son `NORMAL`, `DANADO` y `PELIGROSO`. El último campo de una conexión, `ABIERTA` o `BLOQUEADA`, conserva el bloqueo independientemente de su condición. Para los nodos, la minúscula de la cuadrícula indica bloqueo. Una misma posición admite varias marcas, por ejemplo `1:0:0 DANADO PELIGROSO REPARABLE`. La última marca requiere un nodo bloqueado. Los planos anteriores siguen siendo válidos.

Por defecto se conservan los vecinos de la cuadrícula y se aplican las conexiones personalizadas. La línea opcional `#GRAFO_EXPLICITO` indica que únicamente se deben usar los enlaces enumerados, incluso si la lista está vacía. Tanto «Guardar escenario» como el guardado en la base de datos usan este formato.

Tipos de nodo: `HABITACION`, `PASILLO`, `ESCALERA`, `SALIDA`. Tipos de persona: `ADULTO`, `NINO`, `MOVILIDAD`, `RESCATE`. Salud: `SANO`, `HERIDO`, `ENFERMO`, `INCAPACITADO`. Un plano `.txt` inválido se rechaza con un mensaje y el edificio actual no cambia.

Compilar, probar y ejecutar (JDK 11 o superior), desde la raíz del repo:
```
javac -d out $(find src test -name "*.java")
java -cp out evacua.PruebasEstructuras
java -cp out evacua.PruebasPlanos
java -cp out evacua.PruebasPersistencia
java -cp out evacua.PruebasPisos
java -cp out evacua.PruebasCongestion
java -cp out evacua.PruebasEmergencia
java -cp out evacua.PruebasCarga
java -cp out evacua.PruebasEstres
java -cp out evacua.PruebasFamiliares
java -cp out evacua.PruebasDepartamentos
java -cp out evacua.PruebasRutasDinamicas
java -cp out evacua.vista.PruebasInterfaz
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
7. **Validar antes de modificar:** la lectura de planos `.txt` y el guardado de escenarios validan primero (RN-10) y nunca dejan el edificio en un estado a medias.
8. **Bitácora inmutable** (RN-11): solo se agrega, nunca se edita.
9. **Valores configurables:** tiempo límite, umbral de estrés, probabilidad de eventos, semilla y capacidades no deben quedar fijos en el código.
10. Idioma del proyecto: **español** (nombres de dominio, mensajes y documentación).
