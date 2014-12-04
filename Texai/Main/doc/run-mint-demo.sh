#!/bin/sh
# ****************************************************************************
# * Runs the A.I.Coin Mint demo in a docker container.
# ****************************************************************************

# remove the previous container named Mint
sudo docker ps -a | grep 'Mint' | awk '{print $1}' | xargs --no-run-if-empty sudo docker rm

# port 10000 is for Java remote debugging, not included for normal production

sudo docker run --name Mint -t -i \
  -p 127.0.1.1:35048:5048 \
  -p 127.0.1.1:5903:5900 \
  -p 127.0.1.1:10003:10000 \
  -v /home/reed/docker/Mint:/aicoin \
  -v /etc/localtime:/etc/localtime:ro \
  -e DISPLAY=$DISPLAY \
  -v /tmp/.X11-unix:/tmp/.X11-unix:ro \
  aicoin-mint
