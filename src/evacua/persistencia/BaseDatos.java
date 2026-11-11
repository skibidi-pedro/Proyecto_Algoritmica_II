package evacua.persistencia;

import evacua.edificio.Edificio;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class BaseDatos {
    public static final String CABECERA = "escenario,algoritmo,semilla,personas,evacuadas,ticks,tiempo_promedio,estres_promedio";
    private final Path carpeta;

    public BaseDatos(String carpeta) {
        this.carpeta = Paths.get(carpeta);
    }

    private Path archivo(String nombre) {
        if (nombre == null || !nombre.matches("[\\w .-]{1,60}")) throw new IllegalArgumentException("Nombre de escenario invalido (use letras, numeros, espacio, punto, guion).");
        return carpeta.resolve("escenarios").resolve(nombre + ".txt");
    }

    public void guardar(String nombre, Edificio e) throws IOException {
        List<String> err = GestorEscenarios.validarParaGuardar(e);
        if (!err.isEmpty()) throw new IllegalArgumentException("No se puede guardar el escenario: " + String.join(" ", err));
        Path f = archivo(nombre);
        Files.createDirectories(f.getParent());
        Files.write(f, GestorEscenarios.aTexto(e, true).getBytes(StandardCharsets.UTF_8));
    }

    public Edificio cargar(String nombre) throws IOException {
        Path f = archivo(nombre);
        if (!Files.exists(f)) throw new IllegalArgumentException("No existe el escenario '" + nombre + "'.");
        return GestorEscenarios.deTexto(new String(Files.readAllBytes(f), StandardCharsets.UTF_8));
    }

    public List<String> listar() throws IOException {
        List<String> r = new ArrayList<>();
        Path dir = carpeta.resolve("escenarios");
        if (!Files.isDirectory(dir)) return r;
        try (Stream<Path> s = Files.list(dir)) {
            s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".txt"))
                    .map(n -> n.substring(0, n.length() - 4)).sorted().forEach(r::add);
        }
        return r;
    }

    public boolean eliminar(String nombre) throws IOException {
        return Files.deleteIfExists(archivo(nombre));
    }

    public void registrarResultado(String linea) throws IOException {
        Files.createDirectories(carpeta);
        Path f = carpeta.resolve("resultados.csv");
        if (!Files.exists(f)) Files.write(f, (CABECERA + "\n").getBytes(StandardCharsets.UTF_8));
        Files.write(f, (linea + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
    }

    public List<String> resultados() throws IOException {
        Path f = carpeta.resolve("resultados.csv");
        return Files.exists(f) ? Files.readAllLines(f, StandardCharsets.UTF_8) : new ArrayList<>();
    }
}
