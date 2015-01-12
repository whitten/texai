#!/bin/sh
# ****************************************************************************
# * updates the demonstration installation
# ****************************************************************************

if [ $(hostname) = "minsky" ]; then
  echo "installing in the home directory ..."
  cd /home/reed
  rm -fr Deployment-1.0
  unzip -ouK /home/reed/git/texai/Texai/Deployment/target/Deployment-1.0.zip
  chmod a+x /home/reed/Deployment-1.0/run-create-software-deployment-manifest.sh
  mkdir -p /home/reed/Deployment-1.0/data/deployment-manifests
fi
