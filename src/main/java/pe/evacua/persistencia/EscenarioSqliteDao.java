package pe.evacua.persistencia;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import pe.evacua.escenario.BloqueoDatos;
import pe.evacua.escenario.CongestionNodo;
import pe.evacua.escenario.ConexionDatos;
import pe.evacua.escenario.Escenario;
import pe.evacua.escenario.EscenarioInvalidoException;
import pe.evacua.escenario.NodoDatos;
import pe.evacua.escenario.PersonaDatos;
import pe.evacua.escenario.ResultadoSimulacion;
import pe.evacua.escenario.TipoElemento;
import pe.evacua.escenario.TipoNodo;
import pe.evacua.escenario.TipoPersona;
import pe.evacua.escenario.ValidadorEscenario;
import pe.evacua.estructuras.ListaSimple;

/**
 * Persistencia de escenarios en SQLite mediante JDBC.
 * Cada operación abre su propia conexión; el guardado es una transacción única,
 * así que un escenario nunca queda a medias.
 */
public class EscenarioSqliteDao implements RepositorioEscenarios {

    private static final String SQL_EXISTE = "SELECT 1 FROM escenario WHERE nombre = ?";
    private static final String SQL_BORRAR = "DELETE FROM escenario WHERE nombre = ?";
    private static final String SQL_INSERT_ESCENARIO = "INSERT INTO escenario (nombre, edificio, creado) VALUES (?, ?, ?)";
    private static final String SQL_ID_ESCENARIO = "SELECT id FROM escenario WHERE nombre = ?";
    private static final String SQL_INSERT_NODO = "INSERT INTO nodo (escenario_id, id, nombre, tipo, x, y, capacidad, orden) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_INSERT_CONEXION = "INSERT INTO conexion (escenario_id, id, origen, destino, tiempo, capacidad, orden) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_INSERT_PERSONA = "INSERT INTO persona (escenario_id, id, tipo, nodo_inicial, orden) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_INSERT_BLOQUEO = "INSERT INTO bloqueo (escenario_id, elemento, elemento_id, orden) VALUES (?, ?, ?, ?)";
    private static final String SQL_INSERT_RESULTADO = "INSERT INTO resultado (escenario_id, evacuadas, no_evacuadas, tiempo_total, tiempo_promedio) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_INSERT_CONGESTION = "INSERT INTO congestion (escenario_id, nodo_id, ocupacion, orden) VALUES (?, ?, ?, ?)";
    private static final String SQL_SELECT_ESCENARIO = "SELECT id, edificio FROM escenario WHERE nombre = ?";
    private static final String SQL_SELECT_NODOS = "SELECT id, nombre, tipo, x, y, capacidad FROM nodo WHERE escenario_id = ? ORDER BY orden";
    private static final String SQL_SELECT_CONEXIONES = "SELECT id, origen, destino, tiempo, capacidad FROM conexion WHERE escenario_id = ? ORDER BY orden";
    private static final String SQL_SELECT_PERSONAS = "SELECT id, tipo, nodo_inicial FROM persona WHERE escenario_id = ? ORDER BY orden";
    private static final String SQL_SELECT_BLOQUEOS = "SELECT elemento, elemento_id FROM bloqueo WHERE escenario_id = ? ORDER BY orden";
    private static final String SQL_SELECT_RESULTADO = "SELECT evacuadas, no_evacuadas, tiempo_total, tiempo_promedio FROM resultado WHERE escenario_id = ?";
    private static final String SQL_SELECT_CONGESTION = "SELECT nodo_id, ocupacion FROM congestion WHERE escenario_id = ? ORDER BY orden";
    private static final String SQL_LISTAR = "SELECT e.nombre, e.edificio, (SELECT COUNT(*) FROM resultado r WHERE r.escenario_id = e.id) FROM escenario e ORDER BY e.id";

    private final String url;

    /**
     * @param url URL JDBC, por ejemplo {@code jdbc:sqlite:evacua.db} o {@code jdbc:sqlite::memory:}
     *            (esta última no conserva datos entre conexiones; úsese solo para archivos).
     */
    public EscenarioSqliteDao(String url) throws RepositorioException {
        this.url = url;
        inicializarEsquema();
    }

    private Connection abrir() throws SQLException {
        Connection conexion = DriverManager.getConnection(url);
        try (Statement st = conexion.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return conexion;
    }

    private void inicializarEsquema() throws RepositorioException {
        String script;
        try (InputStream in = EscenarioSqliteDao.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new RepositorioException("No se encontró schema.sql en los recursos");
            }
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RepositorioException("No se pudo leer schema.sql", e);
        }
        try (Connection c = abrir(); Statement st = c.createStatement()) {
            for (String sentencia : quitarComentarios(script).split(";")) {
                if (!sentencia.isBlank()) {
                    st.execute(sentencia);
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("No se pudo inicializar la base de datos (" + url + ")", e);
        }
    }

    private static String quitarComentarios(String script) {
        StringBuilder sb = new StringBuilder();
        for (String linea : script.split("\n")) {
            if (!linea.trim().startsWith("--")) {
                sb.append(linea).append('\n');
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ guardar

    @Override
    public void guardar(Escenario e, boolean reemplazar) throws EscenarioInvalidoException, RepositorioException {
        ValidadorEscenario.exigirValido(e);
        try (Connection c = abrir()) {
            c.setAutoCommit(false);
            try {
                if (existe(c, e.nombre())) {
                    if (!reemplazar) {
                        throw new RepositorioException("Ya existe un escenario con el nombre \"" + e.nombre() + "\"");
                    }
                    try (PreparedStatement ps = c.prepareStatement(SQL_BORRAR)) {
                        ps.setString(1, e.nombre());
                        ps.executeUpdate();
                    }
                }
                long id = insertarEscenario(c, e);
                insertarNodos(c, id, e);
                insertarConexiones(c, id, e);
                insertarPersonas(c, id, e);
                insertarBloqueos(c, id, e);
                insertarResultado(c, id, e.resultado());
                c.commit();
            } catch (SQLException | RepositorioException ex) {
                c.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new RepositorioException("Error al guardar el escenario \"" + e.nombre() + "\"", ex);
        }
    }

    private boolean existe(Connection c, String nombre) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SQL_EXISTE)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private long insertarEscenario(Connection c, Escenario e) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SQL_INSERT_ESCENARIO)) {
            ps.setString(1, e.nombre());
            ps.setString(2, e.edificio());
            ps.setString(3, Instant.now().toString());
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement(SQL_ID_ESCENARIO)) {
            ps.setString(1, e.nombre());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private void insertarNodos(Connection c, long id, Escenario e) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SQL_INSERT_NODO)) {
            int orden = 0;
            for (NodoDatos n : e.nodos()) {
                ps.setLong(1, id);
                ps.setString(2, n.id());
                ps.setString(3, n.nombre());
                ps.setString(4, n.tipo().name());
                ps.setDouble(5, n.x());
                ps.setDouble(6, n.y());
                ps.setInt(7, n.capacidad());
                ps.setInt(8, orden++);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertarConexiones(Connection c, long id, Escenario e) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SQL_INSERT_CONEXION)) {
            int orden = 0;
            for (ConexionDatos k : e.conexiones()) {
                ps.setLong(1, id);
                ps.setString(2, k.id());
                ps.setString(3, k.origen());
                ps.setString(4, k.destino());
                ps.setDouble(5, k.tiempoRecorrido());
                ps.setInt(6, k.capacidad());
                ps.setInt(7, orden++);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertarPersonas(Connection c, long id, Escenario e) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SQL_INSERT_PERSONA)) {
            int orden = 0;
            for (PersonaDatos p : e.personas()) {
                ps.setLong(1, id);
                ps.setString(2, p.id());
                ps.setString(3, p.tipo().name());
                ps.setString(4, p.nodoInicial());
                ps.setInt(5, orden++);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertarBloqueos(Connection c, long id, Escenario e) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SQL_INSERT_BLOQUEO)) {
            int orden = 0;
            for (BloqueoDatos b : e.bloqueos()) {
                ps.setLong(1, id);
                ps.setString(2, b.elemento().name());
                ps.setString(3, b.elementoId());
                ps.setInt(4, orden++);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertarResultado(Connection c, long id, ResultadoSimulacion r) throws SQLException {
        if (r == null) {
            return;
        }
        try (PreparedStatement ps = c.prepareStatement(SQL_INSERT_RESULTADO)) {
            ps.setLong(1, id);
            ps.setInt(2, r.evacuadas());
            ps.setInt(3, r.noEvacuadas());
            ps.setDouble(4, r.tiempoTotal());
            ps.setDouble(5, r.tiempoPromedio());
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement(SQL_INSERT_CONGESTION)) {
            int orden = 0;
            for (CongestionNodo n : r.nodosMasCongestionados()) {
                ps.setLong(1, id);
                ps.setString(2, n.nodoId());
                ps.setLong(3, n.ocupacionAcumulada());
                ps.setInt(4, orden++);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ------------------------------------------------------------------ cargar

    @Override
    public Escenario cargar(String nombre) throws RepositorioException {
        try (Connection c = abrir()) {
            long id;
            String edificio;
            try (PreparedStatement ps = c.prepareStatement(SQL_SELECT_ESCENARIO)) {
                ps.setString(1, nombre);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new EscenarioNoEncontradoException(nombre);
                    }
                    id = rs.getLong(1);
                    edificio = rs.getString(2);
                }
            }
            return new Escenario(
                    nombre,
                    edificio,
                    leerNodos(c, id),
                    leerConexiones(c, id),
                    leerPersonas(c, id),
                    leerBloqueos(c, id),
                    leerResultado(c, id));
        } catch (SQLException ex) {
            throw new RepositorioException("Error al cargar el escenario \"" + nombre + "\"", ex);
        }
    }

    private ListaSimple<NodoDatos> leerNodos(Connection c, long id) throws SQLException {
        ListaSimple<NodoDatos> lista = new ListaSimple<>();
        try (PreparedStatement ps = c.prepareStatement(SQL_SELECT_NODOS)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.agregarFinal(new NodoDatos(
                            rs.getString(1), rs.getString(2), TipoNodo.valueOf(rs.getString(3)),
                            rs.getDouble(4), rs.getDouble(5), rs.getInt(6)));
                }
            }
        }
        return lista;
    }

    private ListaSimple<ConexionDatos> leerConexiones(Connection c, long id) throws SQLException {
        ListaSimple<ConexionDatos> lista = new ListaSimple<>();
        try (PreparedStatement ps = c.prepareStatement(SQL_SELECT_CONEXIONES)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.agregarFinal(new ConexionDatos(
                            rs.getString(1), rs.getString(2), rs.getString(3), rs.getDouble(4), rs.getInt(5)));
                }
            }
        }
        return lista;
    }

    private ListaSimple<PersonaDatos> leerPersonas(Connection c, long id) throws SQLException {
        ListaSimple<PersonaDatos> lista = new ListaSimple<>();
        try (PreparedStatement ps = c.prepareStatement(SQL_SELECT_PERSONAS)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.agregarFinal(new PersonaDatos(
                            rs.getString(1), TipoPersona.valueOf(rs.getString(2)), rs.getString(3)));
                }
            }
        }
        return lista;
    }

    private ListaSimple<BloqueoDatos> leerBloqueos(Connection c, long id) throws SQLException {
        ListaSimple<BloqueoDatos> lista = new ListaSimple<>();
        try (PreparedStatement ps = c.prepareStatement(SQL_SELECT_BLOQUEOS)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.agregarFinal(new BloqueoDatos(TipoElemento.valueOf(rs.getString(1)), rs.getString(2)));
                }
            }
        }
        return lista;
    }

    private ResultadoSimulacion leerResultado(Connection c, long id) throws SQLException {
        int evacuadas;
        int noEvacuadas;
        double total;
        double promedio;
        try (PreparedStatement ps = c.prepareStatement(SQL_SELECT_RESULTADO)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                evacuadas = rs.getInt(1);
                noEvacuadas = rs.getInt(2);
                total = rs.getDouble(3);
                promedio = rs.getDouble(4);
            }
        }
        ListaSimple<CongestionNodo> congestion = new ListaSimple<>();
        try (PreparedStatement ps = c.prepareStatement(SQL_SELECT_CONGESTION)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    congestion.agregarFinal(new CongestionNodo(rs.getString(1), rs.getLong(2)));
                }
            }
        }
        return new ResultadoSimulacion(evacuadas, noEvacuadas, total, promedio, congestion);
    }

    // ------------------------------------------------------------------ resto

    @Override
    public boolean existe(String nombre) throws RepositorioException {
        try (Connection c = abrir()) {
            return existe(c, nombre);
        } catch (SQLException ex) {
            throw new RepositorioException("Error al consultar el escenario \"" + nombre + "\"", ex);
        }
    }

    @Override
    public ListaSimple<ResumenEscenario> listar() throws RepositorioException {
        ListaSimple<ResumenEscenario> lista = new ListaSimple<>();
        try (Connection c = abrir(); PreparedStatement ps = c.prepareStatement(SQL_LISTAR); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.agregarFinal(new ResumenEscenario(rs.getString(1), rs.getString(2), rs.getInt(3) > 0));
            }
        } catch (SQLException ex) {
            throw new RepositorioException("Error al listar los escenarios", ex);
        }
        return lista;
    }

    @Override
    public void eliminar(String nombre) throws RepositorioException {
        try (Connection c = abrir(); PreparedStatement ps = c.prepareStatement(SQL_BORRAR)) {
            ps.setString(1, nombre);
            if (ps.executeUpdate() == 0) {
                throw new EscenarioNoEncontradoException(nombre);
            }
        } catch (SQLException ex) {
            throw new RepositorioException("Error al eliminar el escenario \"" + nombre + "\"", ex);
        }
    }
}
