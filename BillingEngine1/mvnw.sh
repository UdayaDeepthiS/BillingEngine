#!/bin/sh
# Maven Wrapper script for Unix/macOS/Linux
set -e

# Determine the directory containing this script
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_JAR="$SCRIPT_DIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_PROPERTIES="$SCRIPT_DIR/.mvn/wrapper/maven-wrapper.properties"



# Use JAVA_HOME if set
if [ -n "$JAVA_HOME" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
else
    JAVA_CMD="java"
fi

# Download wrapper jar if missing
if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Downloading Maven Wrapper..."
    WRAPPER_URL=$(grep wrapperUrl "$WRAPPER_PROPERTIES" | cut -d= -f2-)
    if command -v curl >/dev/null 2>&1; then
        curl -o "$WRAPPER_JAR" "$WRAPPER_URL" --silent
    elif command -v wget >/dev/null 2>&1; then
        wget -q -O "$WRAPPER_JAR" "$WRAPPER_URL"
    else
        echo "Cannot download Maven Wrapper: curl or wget required" >&2
        exit 1
    fi
fi

exec "$JAVA_CMD" \
    -classpath "$WRAPPER_JAR" \
    "-Dmaven.multiModuleProjectDirectory=$SCRIPT_DIR" \
    org.apache.maven.wrapper.MavenWrapperMain \
    "$@"
