#!/bin/sh
# ****************************************************************************
# * updates the demonstration installation
# ****************************************************************************

if [ $(hostname) = "minsky" ]; then
  echo "migrating to three demo docker container data volumes ..."
  cd /home/reed/docker/Alice
  unzip -ouK /home/reed/git/texai/Texai/Main/target/Main-1.0.zip
  cd /home/reed/docker/Bob
  unzip -ouK /home/reed/git/texai/Texai/Main/target/Main-1.0.zip
  cd /home/reed/docker/Mint
  unzip -ouK /home/reed/git/texai/Texai/Main/target/Main-1.0.zip
  #rsync -v --executability --perms --recursive --times --exclude data/javacv --exclude data/*.uber --exclude data/certificate-serial-nbr.txt --exclude data/secure-random.ser --exclude data/truststore.p12 /home/reed/TexaiLauncher-1.0/* minsky::texai-launcher
fi