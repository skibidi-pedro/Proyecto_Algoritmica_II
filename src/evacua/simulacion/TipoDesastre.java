package evacua.simulacion;

public enum TipoDesastre {
    NINGUNO, TERREMOTO, INCENDIO, FUGA_DE_GAS;

    @Override
    public String toString() {
        return this == NINGUNO ? "Simulacro" : name();
    }

    public boolean requiereOrigen() {
        return this == INCENDIO || this == FUGA_DE_GAS;
    }
}
