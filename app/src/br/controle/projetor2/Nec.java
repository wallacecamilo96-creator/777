package br.controle.projetor2;

/** Monta o sinal infravermelho no protocolo NEC (38 kHz). */
final class Nec {
    static final int FREQUENCIA = 38000;

    private Nec() {}

    /** Quatro bytes do quadro, cada um enviado do bit menos significativo para o mais. */
    static int[] sinal(int b0, int b1, int b2, int b3) {
        int[] p = new int[68];
        int i = 0;
        p[i++] = 9000;
        p[i++] = 4500;
        int[] bytes = {b0, b1, b2, b3};
        for (int b = 0; b < 4; b++) {
            for (int bit = 0; bit < 8; bit++) {
                p[i++] = 560;
                p[i++] = ((bytes[b] >> bit) & 1) == 1 ? 1690 : 560;
            }
        }
        p[i++] = 560;
        p[i] = 40000;
        return p;
    }

    /** codigo: byte baixo = comando, byte alto = 4o byte (inverso do comando no NEC comum). */
    static int[] sinal(Perfis.Perfil perfil, int codigo) {
        return sinal(perfil.endLo, perfil.endHi, codigo & 0xFF, (codigo >> 8) & 0xFF);
    }

    /** Codigo de um comando simples (4o byte = inverso), usado na busca. */
    static int codigoSimples(int comando) {
        return (comando & 0xFF) | (((comando ^ 0xFF) & 0xFF) << 8);
    }
}
