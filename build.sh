#!/usr/bin/env bash
# Compila o APK sem Gradle. Requer (apt): aapt apksigner zipalign dalvik-exchange libandroid-23-java
set -euo pipefail
cd "$(dirname "$0")"
JAR=/usr/lib/android-sdk/platforms/android-23/android.jar
OUT=build
rm -rf "$OUT" && mkdir -p "$OUT/gen" "$OUT/obj" release

python3 -I tools/gerar_perfis.py ir "$OUT/gen/Perfis.java"
aapt package -f -m -J "$OUT/gen" -M app/AndroidManifest.xml -S app/res -I "$JAR"
javac -nowarn --release 8 -cp "$JAR" -d "$OUT/obj" \
    $(find app/src "$OUT/gen" -name '*.java')
dalvik-exchange --dex --output="$OUT/classes.dex" "$OUT/obj"
aapt package -f -M app/AndroidManifest.xml -S app/res -I "$JAR" -F "$OUT/app.unsigned.apk"
(cd "$OUT" && aapt add app.unsigned.apk classes.dex >/dev/null)
zipalign -f 4 "$OUT/app.unsigned.apk" "$OUT/app.aligned.apk"
apksigner sign --ks assinatura.jks --ks-pass pass:controle --key-pass pass:controle \
    --out release/ControleProjetor2.apk "$OUT/app.aligned.apk"
rm -f release/*.idsig
apksigner verify --print-certs release/ControleProjetor2.apk | head -1
ls -l release/ControleProjetor2.apk
