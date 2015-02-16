#!/bin/sh
# ****************************************************************************
# * Runs the A.I.Coin Blockchain Explorer demo in a docker container.
# ****************************************************************************

# initialize the aicoin-qt data directory
#cd docker/BlockchainExplorer/.aicoin
#sudo rm -fr *
#sudo rm .lock
#cd ~

# remove the previous container named Blockchain Explorer
sudo docker ps -a | grep 'aicoin-blockchain-explorer' | awk '{print $1}' | xargs --no-run-if-empty sudo docker rm

# port 10000 is for Java remote debugging, not included for normal production

sudo docker run --name BlockchainExplorer --link Mint:Mint -t -i \
  -p 127.0.1.1:45048:5048 \
  -p 127.0.1.1:5904:5900 \
  -p 3000:3000 \
  -p 127.0.1.1:10004:10000 \
  -v /home/reed/docker/BlockchainExplorer:/aicoin \
  -v /etc/localtime:/etc/localtime:ro \
  -e DISPLAY=$DISPLAY \
  -v /tmp/.X11-unix:/tmp/.X11-unix:ro \
  aicoin-blockchain-explorer ./run-x11vnc.sh

