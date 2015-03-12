#!/bin/sh
# ****************************************************************************
# * updates the demonstration installation
# ****************************************************************************

if [ $(hostname) = "minsky" ]; then
  echo "migrating to demo docker container data volumes ..."
  cd /home/reed/docker/Alice
  rm -fr Main-1.0
  unzip -ouK /home/reed/git/texai/Texai/Main/target/Main-1.0.zip
  cp keystore.uber Main-1.0/data
  cp Main-1.0/openbox-run-aicoin-main.sh .
  chmod a+x openbox-run-aicoin-main.sh

  cd /home/reed/docker/Bob
  rm -fr Main-1.0
  unzip -ouK /home/reed/git/texai/Texai/Main/target/Main-1.0.zip
  cp keystore.uber Main-1.0/data
  cp Main-1.0/openbox-run-aicoin-main.sh .
  chmod a+x openbox-run-aicoin-main.sh

  cd /home/reed/docker/Mint
  rm -fr Main-1.0
  unzip -ouK /home/reed/git/texai/Texai/Main/target/Main-1.0.zip
  cp keystore.uber Main-1.0/data
  cp Main-1.0/openbox-run-aicoin-main.sh .
  chmod a+x openbox-run-aicoin-main.sh

  cd /home/reed/docker/BlockchainExplorer
  rm -fr Main-1.0
  unzip -ouK /home/reed/git/texai/Texai/Main/target/Main-1.0.zip
  cp keystore.uber Main-1.0/data
  cp Main-1.0/openbox-run-aicoin-main.sh .
  chmod a+x openbox-run-aicoin-main.sh

  cd /home/reed/docker/SuperPeer
  rm -fr Main-1.0
  unzip -ouK /home/reed/git/texai/Texai/Main/target/Main-1.0.zip
  cp keystore.uber Main-1.0/data
  cp Main-1.0/openbox-run-aicoin-main.sh .
  chmod a+x openbox-run-aicoin-main.sh

  echo "migrating to test demo docker container data volumes ..."
  cd /home/reed/docker/TestAlice
  rm -fr Main-1.0
  unzip -ouK /home/reed/git/texai/Texai/Main/target/Main-1.0.zip
  cp keystore.uber Main-1.0/data
  cp Main-1.0/openbox-run-aicoin-main.sh .
  chmod a+x openbox-run-aicoin-main.sh

  cd /home/reed/docker/TestBob
  rm -fr Main-1.0
  unzip -ouK /home/reed/git/texai/Texai/Main/target/Main-1.0.zip
  cp keystore.uber Main-1.0/data
  cp Main-1.0/openbox-run-aicoin-main.sh .
  chmod a+x openbox-run-aicoin-main.sh

  cd /home/reed/docker/TestMint
  rm -fr Main-1.0
  unzip -ouK /home/reed/git/texai/Texai/Main/target/Main-1.0.zip
  cp keystore.uber Main-1.0/data
  cp Main-1.0/openbox-run-aicoin-main.sh .
  chmod a+x openbox-run-aicoin-main.sh

  cd /home/reed/docker/TestBlockchainExplorer
  rm -fr Main-1.0
  unzip -ouK /home/reed/git/texai/Texai/Main/target/Main-1.0.zip
  cp keystore.uber Main-1.0/data
  cp Main-1.0/openbox-run-aicoin-main.sh .
  chmod a+x openbox-run-aicoin-main.sh

  cd /home/reed/docker/TestSuperPeer
  rm -fr Main-1.0
  unzip -ouK /home/reed/git/texai/Texai/Main/target/Main-1.0.zip
  cp keystore.uber Main-1.0/data
  cp Main-1.0/openbox-run-aicoin-main.sh .
  chmod a+x openbox-run-aicoin-main.sh
fi
