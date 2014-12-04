#!/bin/sh
# ****************************************************************************
# * Runs the A.I.Coin Bob demo in a docker container.
# ****************************************************************************

# remove the previous container named Bob
sudo docker ps -a | grep 'Bob' | awk '{print $1}' | xargs --no-run-if-empty sudo docker rm

# port 10000 is for Java remote debugging, not included for normal production

sudo docker run --name Bob --link Mint:Mint -t -i \
  -p 127.0.1.1:25048:5048 \
  -p 127.0.1.1:5902:5900 \
  -p 127.0.1.1:10002:10000 \
  -v /home/reed/docker/Bob:/aicoin \
  -v /etc/localtime:/etc/localtime:ro \
  -e DISPLAY=$DISPLAY \
  -v /tmp/.X11-unix:/tmp/.X11-unix:ro \
  aicoin-bob
