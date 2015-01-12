#!/bin/sh

# Create a software deployment manifest.

CLASSPATH=lib/commons-io-2.4.jar
CLASSPATH=$CLASSPATH:lib/joda-time-1.6.jar
CLASSPATH=$CLASSPATH:lib/log4j-1.2.15.jar
CLASSPATH=$CLASSPATH:lib/Deployment-1.0.jar
CLASSPATH=$CLASSPATH:lib/Utilities-1.0.jar

java -ea -Dlog4j.configuration=file://$PWD/log4j.properties -classpath $CLASSPATH org.texai.deployment.CreateSoftwareDeploymentManifest \
/home/reed/docker/Bob /home/reed/Bob-old data/deployment-manifests

