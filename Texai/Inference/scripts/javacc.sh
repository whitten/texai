#!/bin/sh
# ****************************************************************************
# * execute the java compiler-compiler
# ****************************************************************************

JAVACC=~/more/javacc-4.0/bin/javacc

cd ~/svn/Inference/src/org/texai/inference/ruleParser 
$JAVACC RuleParser.jj

