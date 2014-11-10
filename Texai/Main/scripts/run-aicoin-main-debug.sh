#!/bin/sh

# Launch AIMain, with remote debugging - an instance in the A.I. Coin cyptocurrency network.
# Each instance should execute in a separate Docker container.

export REPOSITORIES=/home/reed/repositories
export SECURITY_DIR=/home/reed
export TEXAI_DIALOG_PROCESSING_MODE=development

export REPOSITORIES=/home/reed/repositories
export SECURITY_DIR=/home/reed
export TEXAI_DIALOG_PROCESSING_MODE=development

# change to a unique value of your choosing
export RPC_USER=rpctestuser

# change to a unique value of your choosing
export RPC_PASSWORD=rpctestpassword

CLASSPATH=lib/activation-1.1.jar
CLASSPATH=$CLASSPATH:lib/aduna-commons-collections-2.7.0.jar

java -ea -agentlib:jdwp=transport=dt_socket,server=y,address=10000 -Dlog4j.configuration=file://$PWD/log4j.properties -classpath $CLASSPATH org.texai.main.AICoinMain
