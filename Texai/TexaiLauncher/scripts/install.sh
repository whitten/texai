#!/bin/sh
# ****************************************************************************
# * updates the production installation
# ****************************************************************************

if [ $(hostname) = "mccarthy" ]; then
  echo "migrating to production ..."
  cd /home/reed
  unzip -ouK svn/Texai/TexaiLauncher/target/TexaiLauncher-1.0.zip
  rsync -v --executability --perms --recursive --times --exclude data/javacv --exclude data/*.uber --exclude data/certificate-serial-nbr.txt --exclude data/secure-random.ser --exclude data/truststore.p12 /home/reed/TexaiLauncher-1.0/* minsky::texai-launcher
fi
