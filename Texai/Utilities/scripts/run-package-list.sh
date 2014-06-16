#!/bin/sh
# ****************************************************************************
# * runs the package-list merge
# ****************************************************************************

CLASSPATH=../target/Utilities-1.0.jar
java -enableassertions -classpath $CLASSPATH org.texai.util.PackageList
