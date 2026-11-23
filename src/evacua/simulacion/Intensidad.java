package evacua.simulacion;

public enum Intensidad {
    BAJA(12, 10), MEDIA(14, 11), ALTA(15, 12);

    private final int terremoto, foco;
    Intensidad(int terremoto, int foco) {
        this.terremoto = terremoto;
        this.foco = foco;
    }

    int demora(TipoDesastre tipo) {
        return tipo == TipoDesastre.TERREMOTO ? terremoto : foco;
    }
}
