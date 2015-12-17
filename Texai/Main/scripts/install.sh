#!/bin/sh
# ****************************************************************************
# * updates the demonstration installation
# ****************************************************************************

if [ $(hostname) = "minsky" ]; then
  echo "installing Alice ..."
  cd /home/reed/docker/Alice
  cp /home/reed/git/texai/Texai/Main/data/keystore.uber .
  rm -fr Main-1.0
  unzip -ouK /home/reed/git/texai/Texai/Main/target/Main-1.0.zip
  cp keystore.uber Main-1.0/data
  cp Main-1.0/openbox-run-aicoin-main.sh .
  chmod a+x openbox-run-aicoin-main.sh

  echo "installing Bob ..."
  cd /home/reed/docker/Bob
  cp /home/reed/git/texai/Texai/Main/data/keystore.uber .
  rm -fr Main-1.0
  unzip -ouK /home/reed/git/texai/Texai/Main/target/Main-1.0.zip
  cp keystore.uber Main-1.0/data
  cp Main-1.0/openbox-run-aicoin-main.sh .
  chmod a+x openbox-run-aicoin-main.sh

  echo "installing Mint ..."
  cd /home/reed/docker/Mint
  cp /home/reed/git/texai/Texai/Main/data/keystore.uber .
  rm -fr Main-1.0
  unzip -ouK /home/reed/git/texai/Texai/Main/target/Main-1.0.zip
  cp keystore.uber Main-1.0/data
  cp Main-1.0/openbox-run-aicoin-main.sh .
  chmod a+x openbox-run-aicoin-main.sh

  echo "installing BlockchainExplorer ..."
  cd /home/reed/docker/BlockchainExplorer
  cp /home/reed/git/texai/Texai/Main/data/keystore.uber .
  rm -fr Main-1.0
  unzip -ouK /home/reed/git/texai/Texai/Main/target/Main-1.0.zip
  cp keystore.uber Main-1.0/data
  cp Main-1.0/openbox-run-aicoin-main.sh .
  chmod a+x openbox-run-aicoin-main.sh

  echo "installing SuperPeer ..."
  cd /home/reed/docker/SuperPeer
  cp /home/reed/git/texai/Texai/Main/data/keystore.uber .
  rm -fr Main-1.0
  unzip -ouK /home/reed/git/texai/Texai/Main/target/Main-1.0.zip
  cp keystore.uber Main-1.0/data
  cp Main-1.0/openbox-run-aicoin-main.sh .
  chmod a+x openbox-run-aicoin-main.sh

  echo "installing TestAlice ..."
  cd /home/reed/docker/TestAlice
  cp /home/reed/git/texai/Texai/Main/data/keystore.uber .
  rm -fr Main-1.0
  unzip -ouK /home/reed/git/texai/Texai/Main/target/Main-1.0.zip
  cp keystore.uber Main-1.0/data
  cp Main-1.0/openbox-run-aicoin-main.sh .
  chmod a+x openbox-run-aicoin-main.sh

  echo "installing TestBob ..."
  cd /home/reed/docker/TestBob
  cp /home/reed/git/texai/Texai/Main/data/keystore.uber .
  rm -fr Main-1.0
  unzip -ouK /home/reed/git/texai/Texai/Main/target/Main-1.0.zip
  cp keystore.uber Main-1.0/data
  cp Main-1.0/openbox-run-aicoin-main.sh .
  chmod a+x openbox-run-aicoin-main.sh

  echo "installing TestMint ..."
  cd /home/reed/docker/TestMint
  cp /home/reed/git/texai/Texai/Main/data/keystore.uber .
  rm -fr Main-1.0
  unzip -ouK /home/reed/git/texai/Texai/Main/target/Main-1.0.zip
  cp keystore.uber Main-1.0/data
  cp Main-1.0/openbox-run-aicoin-main.sh .
  chmod a+x openbox-run-aicoin-main.sh

  echo "installing TestBlockchainExplorer ..."
  cd /home/reed/docker/TestBlockchainExplorer
  cp /home/reed/git/texai/Texai/Main/data/keystore.uber .
  rm -fr Main-1.0
  unzip -ouK /home/reed/git/texai/Texai/Main/target/Main-1.0.zip
  cp keystore.uber Main-1.0/data
  cp Main-1.0/openbox-run-aicoin-main.sh .
  chmod a+x openbox-run-aicoin-main.sh

  echo "installing TestSuperPeer ..."
  cd /home/reed/docker/TestSuperPeer
  cp /home/reed/git/texai/Texai/Main/data/keystore.uber .
  rm -fr Main-1.0
  unzip -ouK /home/reed/git/texai/Texai/Main/target/Main-1.0.zip
  cp keystore.uber Main-1.0/data
  cp Main-1.0/openbox-run-aicoin-main.sh .
  chmod a+x openbox-run-aicoin-main.sh
fi
