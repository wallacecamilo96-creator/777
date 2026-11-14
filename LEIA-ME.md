# Controle Projetor 2

Controle remoto por infravermelho para o projetor Bettdow AC1042, para celulares
Xiaomi/Poco com emissor infravermelho. Tem FOCO + e FOCO −.

## Instalar

1. Baixe `release/ControleProjetor2.apk` no celular e toque nele.
2. Permita instalar apps desta fonte e confirme "Instalar mesmo assim".
3. Abra "Controle Projetor 2" e aponte o topo do celular para o projetor.

## Se os botões não responderem

Toque em **Ajustes**, troque o **código do projetor** com as setas e teste o
VOL + em cada um até o projetor responder. São 9 códigos.

## Se o controle funciona mas o FOCO não

Em **Ajustes > Busca do foco**, toque em Iniciar e olhe a imagem. Quando o foco
mexer, toque em Pausar, confira com Repetir e salve como FOCO + ou FOCO −.
Se a lente do projetor tem um anel de foco manual, nenhum botão vai mexer no foco.

## Compilar

```
apt-get install aapt apksigner zipalign dalvik-exchange libandroid-23-java
./build.sh
```

Os códigos ficam em `ir/` (arquivos do Flipper-IRDB, licença CC0) e viram
`Perfis.java` pelo `tools/gerar_perfis.py`. `assinatura.jks` (senha `controle`)
é a chave de assinatura: mantê-la permite instalar versões novas por cima da antiga.
