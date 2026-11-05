package evacua.importacion;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Lee un plano .xml:
 * &lt;edificio&gt;&lt;nodos&gt;&lt;nodo id="A" tipo="HABITACION" piso="1" x="1" y="2" bloqueado="false"/&gt;&lt;/nodos&gt;
 * &lt;conexiones&gt;&lt;conexion origen="A" destino="B" tiempo="2" capacidad="3"/&gt;&lt;/conexiones&gt;
 * &lt;personas&gt;&lt;persona nodo="A" tipo="ADULTO" salud="SANO"/&gt;&lt;/personas&gt;&lt;/edificio&gt;
 */
public final class ParserXml {
    private ParserXml() {
    }

    public static PlanoDatos parse(String texto) {
        org.w3c.dom.Document doc;
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            doc = f.newDocumentBuilder().parse(new ByteArrayInputStream(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalArgumentException("XML invalido: " + ex.getMessage());
        }
        PlanoDatos d = new PlanoDatos();
        d.cuadricula = "cuadricula".equals(doc.getDocumentElement().getAttribute("modo"));
        d.filas = entero(attr(doc.getDocumentElement(), "filas"));
        d.columnas = entero(attr(doc.getDocumentElement(), "columnas"));
        d.pisos = entero(attr(doc.getDocumentElement(), "pisos"));
        NodeList nodos = doc.getElementsByTagName("nodo");
        for (int i = 0; i < nodos.getLength(); i++) {
            Element e = (Element) nodos.item(i);
            PlanoDatos.NodoDef n = new PlanoDatos.NodoDef();
            n.id = attr(e, "id");
            n.tipo = attr(e, "tipo");
            n.x = entero(attr(e, "x"));
            n.y = entero(attr(e, "y"));
            n.piso = attr(e, "piso") == null ? Integer.valueOf(1) : entero(attr(e, "piso"));
            n.bloqueado = "true".equalsIgnoreCase(attr(e, "bloqueado"));
            d.nodos.add(n);
        }
        NodeList cs = doc.getElementsByTagName("conexion");
        for (int i = 0; i < cs.getLength(); i++) {
            Element e = (Element) cs.item(i);
            PlanoDatos.ConexionDef c = new PlanoDatos.ConexionDef();
            c.origen = attr(e, "origen");
            c.destino = attr(e, "destino");
            try {
                c.tiempo = attr(e, "tiempo") == null ? null : Double.valueOf(attr(e, "tiempo"));
            } catch (NumberFormatException ex) {
                c.tiempo = null;
            }
            c.capacidad = entero(attr(e, "capacidad"));
            c.bloqueada = "true".equalsIgnoreCase(attr(e, "bloqueada"));
            d.conexiones.add(c);
        }
        NodeList ps = doc.getElementsByTagName("persona");
        for (int i = 0; i < ps.getLength(); i++) {
            Element e = (Element) ps.item(i);
            PlanoDatos.PersonaDef p = new PlanoDatos.PersonaDef();
            p.nodo = attr(e, "nodo");
            if (attr(e, "tipo") != null) p.tipo = attr(e, "tipo");
            if (attr(e, "salud") != null) p.salud = attr(e, "salud");
            d.personas.add(p);
        }
        return d;
    }

    private static String attr(Element e, String k) {
        return e.hasAttribute(k) ? e.getAttribute(k).trim() : null;
    }

    private static Integer entero(String s) {
        try {
            return s == null ? null : Integer.valueOf(s);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
