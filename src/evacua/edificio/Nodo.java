package evacua.edificio;


/** Un nodo del edificio (un cuadro de la cuadricula). */
public class Nodo {
    public TipoNodo tipo = TipoNodo.VACIO;
    public boolean bloqueado;
    public int ocupacion;   // personas en este instante
    public int visitas;     // acumulado para el mapa de calor
    public int esperas;     // acumulado de esperas (cuello de botella)
}
