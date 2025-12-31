#!/usr/bin/env sh
# -----------------------------------------------------------------------------
# Gradle start up script for UN*X
# -----------------------------------------------------------------------------
set -e

# Resolve links - $0 may be a symlink
PRG="$0"
while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`"/$link"
  fi
done

PRGDIR=`dirname "$PRG"`
# Make it absolute
[ -n "$PRGDIR" ] && PRGDIR=`cd "$PRGDIR" && pwd`

# Determine APP_HOME (project root)
APP_HOME="$PRGDIR"

# Default JVM
if [ -z "$JAVA_HOME" ]; then
  JAVA_CMD="java"
else
  JAVA_CMD="$JAVA_HOME/bin/java"
fi

# Wrapper jar and properties
GRADLE_WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
GRADLE_WRAPPER_PROPERTIES="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"

if [ ! -f "$GRADLE_WRAPPER_JAR" ]; then
  echo "ERROR: Gradle wrapper jar not found: $GRADLE_WRAPPER_JAR"
  echo "Run 'gradle wrapper' to generate the wrapper files."
  exit 1
fi

CLASSPATH="$GRADLE_WRAPPER_JAR"

# Assemble args
ARGS=""
for i in "$@"; do
  ARGS="$ARGS \"$i\""
done

# Exec wrapper main
exec "$JAVA_CMD" -cp "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
