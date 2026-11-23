package evacua.simulacion;

public enum Intensidad {
    BAJA(12, 10, 3, 4, 2, 0.5, 3, 2),
    MEDIA(14, 11, 5, 8, 4, 1.5, 5, 4),
    ALTA(15, 12, 7, 12, 7, 3, 8, 7);

    private final int terremoto, foco;
    final int duracion, estres, refuerzos;
    final double dano, peligro, derrumbe;

    Intensidad(int terremoto, int foco, int duracion, double dano, double peligro, double derrumbe, int estres, int refuerzos) {
        this.duracion = duracion;
        this.dano = dano;
        this.peligro = peligro;
        this.derrumbe = derrumbe;
        this.estres = estres;
        this.refuerzos = refuerzos;
        this.terremoto = terremoto;
        this.foco = foco;
    }

    int demora(TipoDesastre tipo) {
        return tipo == TipoDesastre.TERREMOTO ? terremoto : foco;
    }
}
