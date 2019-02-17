#!/bin/bash

M2_REPO="/home/jonas/.m2/repository"

CLASSPATH="$M2_REPO/com/fazecast/jSerialComm/2.4.0/jSerialComm-2.4.0.jar"
CLASSPATH="$CLASSPATH:$M2_REPO/io/snice/snice-commons/0.1.0-SNAPSHOT/snice-commons-0.1.0-SNAPSHOT.jar"
CLASSPATH="$CLASSPATH:$M2_REPO/io/snice/snice-buffers/0.1.0-SNAPSHOT/snice-buffers-0.1.0-SNAPSHOT.jar"
CLASSPATH="$CLASSPATH:$M2_REPO/com/fasterxml/jackson/dataformat/jackson-dataformat-yaml/2.9.8/jackson-dataformat-yaml-2.9.8.jar"
CLASSPATH="$CLASSPATH:$M2_REPO/com/fasterxml/jackson/core/jackson-core/2.9.8/jackson-core-2.9.8.jar"
CLASSPATH="$CLASSPATH:$M2_REPO/com/fasterxml/jackson/core/jackson-databind/2.9.8/jackson-databind-2.9.8.jar"
CLASSPATH="$CLASSPATH:$M2_REPO/com/fasterxml/jackson/core/jackson-annotations/2.9.8/jackson-annotations-2.9.8.jar"
CLASSPATH="$CLASSPATH:modem-core/target/classes"
CLASSPATH="$CLASSPATH:modem-shell/target/classes"

PROG="/home/jonas/tools/java/current/bin/java"

$PROG -cp $CLASSPATH io.snice.MShell "$*"
