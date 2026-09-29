#!/bin/sh
# Gradle start-up script (POSIX) for Cofre Hero.
app_path=$0
while [ -h "$app_path" ]; do
  ls=$(ls -ld -- "$app_path")
  link=${ls#*' -> '}
  case $link in
    /*) app_path=$link ;;
    *) app_path=${app_path%/*}/$link ;;
  esac
done
APP_HOME=$(cd "${app_path%/*}" >/dev/null && pwd -P) || exit

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
if [ ! -f "$CLASSPATH" ]; then
  echo "ERROR: falta $CLASSPATH. Ejecuta: bash scripts/setup-codespace.sh" >&2
  exit 1
fi

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVACMD=$JAVA_HOME/bin/java
else
  JAVACMD=java
  command -v java >/dev/null 2>&1 || { echo "ERROR: java no encontrado." >&2; exit 1; }
fi

exec "$JAVACMD" -Xmx64m -Xms64m -Dfile.encoding=UTF-8 \
  -Dorg.gradle.appname=gradlew \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain "$@"
