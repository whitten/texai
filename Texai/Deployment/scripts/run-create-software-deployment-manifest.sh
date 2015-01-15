#!/bin/sh

# Create a software deployment manifest.

CLASSPATH=lib/commons-io-2.4.jar
CLASSPATH=$CLASSPATH:lib/bcprov-jdk14-1.50.jar
CLASSPATH=$CLASSPATH:lib/bcprov-jdk15on-1.50.jar
CLASSPATH=$CLASSPATH:lib/bcpkix-jdk14-1.50.jar
CLASSPATH=$CLASSPATH:lib/json-simple-1.1.1.jar
CLASSPATH=$CLASSPATH:lib/joda-time-1.6.jar
CLASSPATH=$CLASSPATH:lib/log4j-1.2.15.jar
CLASSPATH=$CLASSPATH:lib/Deployment-1.0.jar
CLASSPATH=$CLASSPATH:lib/Utilities-1.0.jar
CLASSPATH=$CLASSPATH:lib/X509Security-1.0.jar

java -ea -Dlog4j.configuration=file://$PWD/log4j.properties -classpath $CLASSPATH org.texai.deployment.CreateSoftwareDeploymentManifest \
/home/reed/docker/Bob /home/reed/Bob-old data/deployment-manifests

