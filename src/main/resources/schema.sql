-- Esquema de la base de datos de escenarios de Evacua (SQLite).
-- Cada escenario es independiente: los identificadores (RN-01) son únicos dentro de su escenario.

CREATE TABLE IF NOT EXISTS escenario (
    id       INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre   TEXT NOT NULL UNIQUE,
    edificio TEXT NOT NULL,
    creado   TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS nodo (
    escenario_id INTEGER NOT NULL REFERENCES escenario(id) ON DELETE CASCADE,
    id           TEXT NOT NULL,
    nombre       TEXT,
    tipo         TEXT NOT NULL,
    x            REAL NOT NULL,
    y            REAL NOT NULL,
    capacidad    INTEGER NOT NULL,
    orden        INTEGER NOT NULL,
    PRIMARY KEY (escenario_id, id)
);

CREATE TABLE IF NOT EXISTS conexion (
    escenario_id INTEGER NOT NULL REFERENCES escenario(id) ON DELETE CASCADE,
    id           TEXT NOT NULL,
    origen       TEXT NOT NULL,
    destino      TEXT NOT NULL,
    tiempo       REAL NOT NULL CHECK (tiempo > 0),
    capacidad    INTEGER NOT NULL CHECK (capacidad > 0),
    orden        INTEGER NOT NULL,
    PRIMARY KEY (escenario_id, id)
);

CREATE TABLE IF NOT EXISTS persona (
    escenario_id INTEGER NOT NULL REFERENCES escenario(id) ON DELETE CASCADE,
    id           TEXT NOT NULL,
    tipo         TEXT NOT NULL,
    nodo_inicial TEXT NOT NULL,
    orden        INTEGER NOT NULL,
    PRIMARY KEY (escenario_id, id)
);

CREATE TABLE IF NOT EXISTS bloqueo (
    escenario_id INTEGER NOT NULL REFERENCES escenario(id) ON DELETE CASCADE,
    elemento     TEXT NOT NULL CHECK (elemento IN ('NODO', 'CONEXION')),
    elemento_id  TEXT NOT NULL,
    orden        INTEGER NOT NULL,
    PRIMARY KEY (escenario_id, elemento, elemento_id)
);

CREATE TABLE IF NOT EXISTS resultado (
    escenario_id    INTEGER PRIMARY KEY REFERENCES escenario(id) ON DELETE CASCADE,
    evacuadas       INTEGER NOT NULL,
    no_evacuadas    INTEGER NOT NULL,
    tiempo_total    REAL NOT NULL,
    tiempo_promedio REAL NOT NULL
);

CREATE TABLE IF NOT EXISTS congestion (
    escenario_id INTEGER NOT NULL REFERENCES escenario(id) ON DELETE CASCADE,
    nodo_id      TEXT NOT NULL,
    ocupacion    INTEGER NOT NULL,
    orden        INTEGER NOT NULL,
    PRIMARY KEY (escenario_id, orden)
);
