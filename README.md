# Evacua: Simulador de Evacuación de Edificios

Proyecto del curso **Algorítmica II** (primer entregable).
Docente: Juan Ricardo Tapia Carbajal · Lenguaje: **Java** (interfaz con **Swing**).

Integrantes:
- Pucho Lizana, Pedro Eduardo Mar (Integrante 1)
- Zavaleta Calderon, Steven Jack (Integrante 2)
- Ayala Navarro, Piero Daniel (Integrante 3)
- Flores Escalante, Angel Giovanny (Integrante 4)

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

## 5. Estructuras de datos y algoritmos

- **Estructuras propias reutilizables** (se implementan en el proyecto): lista enlazada simple y doble, pila, cola, cola de prioridad (heap).
- **Grafo** para el edificio (nodos + conexiones ponderadas con capacidad).
- **Dijkstra** y **A\*** para rutas; se comparan por nodos explorados y tiempo de cómputo.
- **Lista doble o pila** para la bitácora de recorrido de cada persona.
- **Heap** como cola de prioridad en la búsqueda de caminos.

## 6. Reparto de trabajo por integrante

Cada integrante es responsable de sus módulos y de sus pruebas; todos participan en la integración final. Las interfaces entre roles están definidas para poder trabajar en paralelo.

**Integrante 1: Núcleo, persistencia, exportación e integración** (RN-01, RN-10, RN-14)
- Estructuras base: listas simple/doble, pila, cola, heap.
- Menú principal, clase `Main` y comunicación entre módulos (importar plano, exportar reportes individuales, comparar escenarios).
- Registro, guardado y carga de escenarios en base de datos.
- Exportación individual por persona (usa la bitácora e historial del Integrante 3).
- Comparación de resultados entre escenarios guardados.
- Configuración del repositorio, integración completa y coordinación de entregas.

**Integrante 2: Edificio, rutas, bloqueos e importación de planos** (RN-02, RN-03, RN-04, RN-05, RN-06, RN-07, RN-15, RN-16)
- Grafo del edificio con CRUD de nodos, estados y capacidades.
- Gestión de bloqueos (marcar/desbloquear) y su exclusión del cálculo de rutas.
- Dijkstra y A\* con comparación de nodos explorados y tiempo.
- Recálculo dinámico desde la posición actual, expuesto como operación reutilizable para el motor de simulación.
- Parser de planos `.json`/`.txt`/`.xml` con validación, y archivos de plano de prueba.

**Integrante 3: Personas y comportamiento** (RN-08, RN-11, RN-12, RN-13, RN-17, RN-18)
- Jerarquía: adulto, niño, movilidad reducida, personal de rescate.
- En `Persona`: bitácora (lista doble o pila), estrés e historial de eventos.
- Lógica diferenciada del personal de rescate.
- Eventos aleatorios (colapso de pasillos) con probabilidad y semilla configurables; bloquean el elemento y activan el recálculo del Integrante 2.
- Control de capacidad de nodos (Congestionado, mayor tiempo de recorrido).

**Integrante 4: Simulación, reportes y visualización** (RN-09, RN-19)
- Motor de simulación paso a paso (integra eventos aleatorios, recálculo y congestión).
- Tiempos de evacuación, cuellos de botella y no evacuados dentro del tiempo límite.
- Reportes y estadísticas por simulación.
- Interfaz Swing: edificio, personas y rutas en tiempo real.
- Acumulación de ocupación por nodo y mapa de calor en Swing.

## 7. Estado actual del repositorio

Actualmente el repo contiene un único archivo, **`EvacuaPrototipo.java`**: un **prototipo de interfaz** en Java Swing, de un solo archivo y sin dependencias externas.

Qué ofrece el prototipo:
- Dibuja el edificio como una **cuadrícula**: cada celda es un nodo (`H` habitación, `P` pasillo, `E` escalera, `S` salida, `.` vacío; minúscula = bloqueado).
- Tipos de persona con velocidad propia (Adulto, Niño, Movilidad reducida, Rescate) y estados de salud.
- Motor de simulación con eventos aleatorios (probabilidad y semilla), estrés, umbral, tiempo límite y capacidad por nodo.
- Búsqueda de rutas con **Dijkstra y A\*** conmutables, y comparación de ambos.
- Carga/guardado de escenarios y plano en `.txt`, comparación de escenarios, reporte general y exportación individual.

Formato `.txt` del prototipo:
```
HHHPPSS      <- filas de la cuadrícula (todas con el mismo largo, al menos una S)
H..P.E.      <- '.' = sin nodo, minúscula = bloqueado
#PERSONAS    <- (opcional) luego líneas: fila columna TIPO SALUD
0 1 ADULTO SANO
0 2 RESCATE SANO
```

**Importante:** el prototipo sirve para validar la idea y la interfaz; el diseño final descrito arriba usa un **grafo explícito** (nodos con coordenadas y conexiones con peso y capacidad), una **base de datos**, un parser `.json`/`.txt`/`.xml` y estructuras de datos propias. El prototipo no sigue todavía esa arquitectura ni la separación de módulos por integrante.

Para ejecutarlo (por ejemplo en NetBeans): crear un proyecto Java, colocar el archivo en el paquete por defecto y ejecutar. Desde terminal, con JDK instalado:
```
javac EvacuaPrototipo.java
java EvacuaPrototipo
```

## 8. Instrucciones para una IA que trabaje en este proyecto

1. **Respeta las reglas de negocio (RN-01 a RN-19)**; cítalas por código al implementar o revisar algo.
2. **Lenguaje y UI:** Java; interfaz solo con Swing.
3. **Estructuras propias:** el curso exige implementar las estructuras de datos (listas, pila, cola, heap); no las sustituyas por `java.util` en los módulos donde se piden propias, salvo que el equipo lo indique.
4. **Respeta los límites de módulo** de la sección 6; una tarea corresponde a un integrante. Los puntos de contacto son:
   - Integrante 2 → expone *recálculo de ruta desde posición actual* al motor.
   - Integrante 3 → provee bitácora e historial a la exportación (Integrante 1) y activa el recálculo ante eventos.
   - Integrante 4 → consume rutas, estado de nodos y personas para simular, reportar y dibujar.
5. **A\* debe ser admisible** (RN-16): convierte la distancia heurística a tiempo de recorrido para no sobreestimar.
6. **Determinismo:** los eventos aleatorios deben usar la semilla configurable para poder repetir una simulación.
7. **Validar antes de modificar:** la importación de planos y el guardado de escenarios validan primero (RN-10, RN-15) y nunca dejan el edificio en un estado a medias.
8. **Bitácora inmutable** (RN-11): solo se agrega, nunca se edita.
9. **Valores configurables:** tiempo límite, umbral de estrés, probabilidad de eventos, semilla y capacidades no deben quedar fijos en el código.
10. Idioma del proyecto: **español** (nombres de dominio, mensajes y documentación).
