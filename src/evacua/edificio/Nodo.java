package evacua.edificio;

public class Nodo {
    public TipoNodo tipo = TipoNodo.VACIO;
    public boolean bloqueado;
    public int ocupacion;
    public int visitas;
    public int esperas;
}
