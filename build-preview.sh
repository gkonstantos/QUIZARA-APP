#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT to an Android SDK with platform 36 and build-tools 36.0.0}"
: "${JAVA_HOME:?Set JAVA_HOME to a JDK}"
compiler="$ANDROID_SDK_ROOT/build-tools/36.0.0"
platform="$ANDROID_SDK_ROOT/platforms/android-36/android.jar"
output="build/manual-preview"
rm -rf "$output"
mkdir -p "$output/generated" "$output/classes" "$output/dex"
python3 - "$output" <<'PY'
from pathlib import Path
import sys
s=Path('app/src/main/AndroidManifest.xml').read_text()
s=s.replace('<manifest xmlns:android="http://schemas.android.com/apk/res/android">','<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.monomaxia.gnoseon">')
s=s.replace('android:name=".MainActivity"','android:name="com.monomaxia.gnoseon.MainActivity"')
Path(sys.argv[1]+'/AndroidManifest.xml').write_text(s)
PY
"$compiler/aapt2" compile --dir app/src/main/res -o "$output/resources.zip"
"$compiler/aapt2" link -o "$output/base.apk" -I "$platform" --manifest "$output/AndroidManifest.xml" --java "$output/generated" --min-sdk-version 23 --target-sdk-version 36 --version-code 33 --version-name 0.11.9 --rename-manifest-package com.monomaxia.gnoseon.preview -A app/src/main/assets "$output/resources.zip"
"$JAVA_HOME/bin/javac" -encoding UTF-8 -source 8 -target 8 -classpath "$platform" -d "$output/classes" "$output/generated/com/monomaxia/gnoseon/R.java" app/src/main/java/com/monomaxia/gnoseon/*.java
"$JAVA_HOME/bin/jar" cf "$output/classes.jar" -C "$output/classes" .
"$compiler/d8" --min-api 23 --lib "$platform" --output "$output/dex" "$output/classes.jar"
python3 - "$output" <<'PY'
from pathlib import Path
from zipfile import ZipFile
import sys
p=Path(sys.argv[1])
with ZipFile(p/'base.apk','a') as z:
 for d in (p/'dex').glob('*.dex'):z.write(d,d.name)
PY
"$compiler/zipalign" -f -p 4 "$output/base.apk" "$output/aligned.apk"
"$compiler/apksigner" sign --ks development/preview.keystore --ks-pass pass:android --key-pass pass:android --out "$output/quizara.apk" "$output/aligned.apk"
"$compiler/apksigner" verify "$output/quizara.apk"
printf '%s\n' "$output/quizara.apk"
