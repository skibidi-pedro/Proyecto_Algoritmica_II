package evacua.simulacion;

public enum TipoDesastre {
    NINGUNO, TERREMOTO, INCENDIO, FUGA_DE_GAS;

    public boolean requiereOrigen() {
        return this == INCENDIO || this == FUGA_DE_GAS;
    }
}
