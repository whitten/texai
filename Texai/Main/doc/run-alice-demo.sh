#!/bin/sh
# ****************************************************************************
# * Runs the A.I.Coin Alice demo in a docker container.
# ****************************************************************************

# initialize the aicoin-qt data directory
#cd docker/Alice/.aicoin
#sudo rm -fr *
#sudo rm .lock
#cd ~

# remove the previous container named Alice (TODO production use -rm)
sudo docker ps -a | grep 'aicoin-alice' | awk '{print $1}' | xargs --no-run-if-empty sudo docker rm

# port 10000 is for Java remote debugging, not included for normal production

sudo docker run --name Alice --link Mint:Mint -t -i \
  -p 127.0.1.1:15048:5048 \
  -p 127.0.1.1:5901:5900 \
  -p 127.0.0.1:8351:8332 \
  -p 8336:8333 \
  -p 127.0.1.1:10001:10000 \
  -v /home/reed/docker/Alice:/aicoin \
  -v /etc/localtime:/etc/localtime:ro \
  -e DISPLAY=$DISPLAY \
  -v /tmp/.X11-unix:/tmp/.X11-unix:ro \
  aicoin-alice ./run-x11vnc.sh

# dump env variables
# sudo docker run --name Alice --link Mint:Mint aicoin-alice env
