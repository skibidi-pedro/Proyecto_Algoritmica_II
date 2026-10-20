# Evacua: Simulador de Evacuación de Edificios

Proyecto de Algorítmica II (Java). Este repositorio contiene, por ahora, la parte del **Integrante 1**:
estructuras base, menú y `Main`, persistencia de escenarios, exportación individual y comparación de escenarios.

## Cómo compilar y probar

Requiere JDK 17 o superior y Maven.

```
mvn test                  # compila y corre todas las pruebas
mvn compile exec:java     # abre el menú de consola
```

La base de datos es un archivo SQLite (`evacua.db`) que se crea solo en la carpeta desde la que se ejecuta.
Si el driver no está disponible, `Main` avisa y trabaja en memoria.

## Estructura

| Paquete | Contenido |
|---|---|
| `pe.evacua.estructuras` | `ListaSimple`, `ListaDoble`, `Pila`, `Cola`, `ColaPrioridad` (heap mínimo). Hechas desde cero, listas para Dijkstra/A* y para la bitácora de cada persona. |
| `pe.evacua.escenario` | Datos que se guardan: `Escenario`, `NodoDatos`, `ConexionDatos`, `PersonaDatos`, `BloqueoDatos`, `ResultadoSimulacion`, y `ValidadorEscenario` (RN-01, RN-03, RN-10). |
| `pe.evacua.persistencia` | `RepositorioEscenarios` (interfaz), `EscenarioSqliteDao` (SQLite/JDBC) y `RepositorioEnMemoria`. El esquema está en `src/main/resources/schema.sql`. |
| `pe.evacua.reportes` | `ReportePersona`, `ExportadorReportes` (RN-14), `ComparadorEscenarios`. |
| `pe.evacua.modulos` | Contratos que deben implementar los demás integrantes: `ImportadorPlano` e `MotorSimulacion`. |
| `pe.evacua.ui` | `MenuPrincipal` (consola). |

## Qué necesita cada integrante de esta base

- **Integrante 2 (grafo, rutas, planos):** implementar `ImportadorPlano` y entregar un `Escenario` (nodos, conexiones, bloqueos).
  Usar `ColaPrioridad` para Dijkstra y A*. Registrarlo en `Main` (línea marcada).
- **Integrante 3 (personas):** la bitácora puede ser una `ListaDoble<RegistroBitacora>` (o `Pila`). Al terminar, cada persona
  debe poder convertirse en un `ReportePersona`.
- **Integrante 4 (simulación y visualización):** implementar `MotorSimulacion`, que devuelve un `SalidaSimulacion`
  (`ResultadoSimulacion` global + lista de `ReportePersona`). Registrarlo en `Main`.

El escenario guardado usa clases de datos simples (records) para que la base de datos no dependa de las clases internas de cada módulo:
cada integrante convierte su modelo a `Escenario` y de vuelta.
