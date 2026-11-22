package evacua.simulacion;

public enum TipoDesastre {
    CLASICO, TERREMOTO, INCENDIO, INUNDACION, FUGA_DE_GAS;

    Desastre crear() {
        switch (this) {
            case TERREMOTO: return new Terremoto();
            case INCENDIO: return new Incendio();
            case INUNDACION: return new Inundacion();
            case FUGA_DE_GAS: return new FugaDeGas();
            default: return new Desastre();
        }
    }
}
