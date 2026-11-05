#!/usr/bin/env bash
# Сборка OpenCreative++: ./build.sh  ->  build/libs/OpenCreativePP-<версия>.jar
# Нужна Java 21 (JAVA_HOME или java в PATH). Gradle скачается сам (wrapper).
#
# Windows: если Gradle падает с "Unable to establish loopback connection" (TEMP с коротким путём вида ADMINI~1),
# временная папка задаётся явно — по умолчанию .tmp внутри проекта (переопределить: CP_TMP=путь ./build.sh).
set -e
cd "$(dirname "$0")"
if [ -z "$JAVA_HOME" ] && ! command -v java >/dev/null 2>&1; then
    for candidate in "$HOME"/.jdks/jdk-21*; do [ -d "$candidate" ] && export JAVA_HOME="$candidate" && break; done
fi
ROOT="$(cygpath -m "$PWD" 2>/dev/null || pwd)"
T="${CP_TMP:-$ROOT/.tmp}"; mkdir -p "$T"
export TMP="$T" TEMP="$T"
export GRADLE_OPTS="-Djdk.net.unixdomain.tmpdir=$T -Djava.io.tmpdir=$T"
./gradlew build -x javadoc --console=plain -Dorg.gradle.jvmargs="-Xmx3g -Djdk.net.unixdomain.tmpdir=$T -Djava.io.tmpdir=$T" "$@"
