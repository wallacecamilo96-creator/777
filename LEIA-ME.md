# Controle Projetor 2

Controle remoto por infravermelho para o projetor Bettdow AC1042, para celulares
Xiaomi/Poco com emissor infravermelho. Tem FOCO + e FOCO −.

## Instalar

1. Baixe `release/ControleProjetor2.apk` no celular e toque nele.
2. Permita instalar apps desta fonte e confirme "Instalar mesmo assim".
3. Abra "Controle Projetor 2" e aponte o topo do celular para o projetor.

## Códigos

O código 1 (Bettdow AC1042) já vem com FOCO + e FOCO −, confirmados no aparelho
(NEC, endereço 0x00, comandos 0x8C e 0x9C). Os outros botões ainda não têm código.

## Descobrir os botões que faltam

Em **Ajustes > Busca de botões**, deixe o projetor na tela inicial, toque em
Iniciar e fique olhando. Quando o projetor reagir, toque em Pausar, confira com
Repetir e as setas, escolha o botão e toque em Salvar. O que for salvo fica
guardado no celular.

## Compilar

```
apt-get install aapt apksigner zipalign dalvik-exchange libandroid-23-java
./build.sh
```

Os códigos ficam em `ir/` (arquivos do Flipper-IRDB, licença CC0) e viram
`Perfis.java` pelo `tools/gerar_perfis.py`. `assinatura.jks` (senha `controle`)
é a chave de assinatura: mantê-la permite instalar versões novas por cima da antiga.
