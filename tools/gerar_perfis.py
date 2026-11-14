#!/usr/bin/env python3
"""Le os arquivos .ir (formato Flipper) da pasta ir/ e gera Perfis.java."""
import re
import sys

TECLAS = ["POWER", "UP", "DOWN", "LEFT", "RIGHT", "OK", "BACK", "HOME", "MENU",
          "SOURCE", "VOL_UP", "VOL_DOWN", "MUTE", "FOCUS_PLUS", "FOCUS_MINUS", "PLAY"]

APELIDOS = {
    "POWER": ["power", "start"],
    "UP": ["up"],
    "DOWN": ["down", "dwn"],
    "LEFT": ["left"],
    "RIGHT": ["right"],
    "OK": ["ok", "enter"],
    "BACK": ["back", "return"],
    "HOME": ["home"],
    "MENU": ["menu", "options", "settings"],
    "SOURCE": ["source", "src", "souce", "input"],
    "VOL_UP": ["volup", "vol+", "vup", "volumeup"],
    "VOL_DOWN": ["voldn", "voldown", "vol-", "vdwn", "volumedown"],
    "MUTE": ["mute"],
    "FOCUS_PLUS": ["focusup", "focusplus", "fright"],
    "FOCUS_MINUS": ["focusdn", "focusminus", "fleft"],
    "PLAY": ["playpause", "play/pause", "play"],
}

# (arquivo, nome mostrado no app) - na ordem em que aparecem no app
PERFIS = [
    ("Thunrlge_X1BQ.ir", "Android A"),
    ("Generic_Projector.ir", "Generico A"),
    ("WiMiUS_s25.ir", "Android B"),
    ("Rayfoto_Projector.ir", "Generico B"),
    ("WeWatch_v10_Projector.ir", "Generico C"),
    ("NexiGo-PJ20.ir", "Com foco A"),
    ("Wanbo_T6_Max.ir", "Com foco B"),
    ("TOPTRO_TR25.ir", "Com foco C"),
    ("LED_Smart_Home_Theater_Projector.ir", "Generico D"),
]


def normaliza(nome):
    return re.sub(r"[\s_]", "", nome.lower())


def le_ir(caminho):
    botoes = []
    atual = {}
    for linha in open(caminho, encoding="utf-8", errors="replace"):
        linha = linha.strip()
        if linha.startswith("#"):
            if atual:
                botoes.append(atual)
            atual = {}
            continue
        if ":" in linha:
            chave, valor = linha.split(":", 1)
            atual[chave.strip()] = valor.strip()
    if atual:
        botoes.append(atual)
    return [b for b in botoes if b.get("type") == "parsed"]


def main(pasta_ir, saida):
    linhas = []
    for arquivo, nome in PERFIS:
        botoes = le_ir(pasta_ir + "/" + arquivo)
        protocolos = {b["protocol"] for b in botoes}
        enderecos = {b["address"] for b in botoes}
        if len(protocolos) != 1 or len(enderecos) != 1:
            sys.exit("perfil com mais de um protocolo/endereco: " + arquivo)
        protocolo = protocolos.pop()
        if protocolo not in ("NEC", "NECext"):
            sys.exit("protocolo nao suportado: " + protocolo)
        end = [int(x, 16) for x in enderecos.pop().split()]
        ext = protocolo == "NECext"
        end_lo, end_hi = end[0], (end[1] if ext else end[0] ^ 0xFF)
        codigos = []
        for tecla in TECLAS:
            codigo = -1
            for b in botoes:
                if normaliza(b["name"]) in APELIDOS[tecla]:
                    c = [int(x, 16) for x in b["command"].split()]
                    codigo = c[0] | ((c[1] if ext else c[0] ^ 0xFF) << 8)
                    break
            codigos.append(codigo)
        if codigos[0] < 0 or codigos[1] < 0:
            sys.exit("perfil sem POWER/UP: " + arquivo)
        linhas.append('        new Perfil("%s", 0x%02X, 0x%02X, %s, new int[]{%s}),' % (
            nome, end_lo, end_hi, "true" if ext else "false",
            ", ".join("-1" if c < 0 else "0x%04X" % c for c in codigos)))

    with open(saida, "w") as f:
        f.write("// GERADO por tools/gerar_perfis.py - nao edite a mao.\n")
        f.write("package br.controle.projetor2;\n\n")
        f.write("final class Perfis {\n")
        for i, t in enumerate(TECLAS):
            f.write("    static final int %s = %d;\n" % (t, i))
        f.write("    static final int TOTAL_TECLAS = %d;\n\n" % len(TECLAS))
        f.write("    static final class Perfil {\n")
        f.write("        final String nome;\n")
        f.write("        final int endLo, endHi;\n")
        f.write("        final boolean ext;\n")
        f.write("        /** byte baixo = comando, byte alto = 4o byte do quadro; -1 = sem botao. */\n")
        f.write("        final int[] codigos;\n")
        f.write("        Perfil(String nome, int endLo, int endHi, boolean ext, int[] codigos) {\n")
        f.write("            this.nome = nome; this.endLo = endLo; this.endHi = endHi;\n")
        f.write("            this.ext = ext; this.codigos = codigos;\n")
        f.write("        }\n    }\n\n")
        f.write("    static final Perfil[] LISTA = {\n")
        f.write("\n".join(linhas) + "\n    };\n\n")
        f.write("    private Perfis() {}\n}\n")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
