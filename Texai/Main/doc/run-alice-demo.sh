#!/bin/sh
# ****************************************************************************
# * Runs the A.I.Coin Alice demo in a docker container.
# ****************************************************************************

# remove the previous container named Alice (TODO production use -rm)
sudo docker ps -a | grep 'Alice' | awk '{print $1}' | xargs --no-run-if-empty sudo docker rm

# port 10000 is for Java remote debugging, not included for normal production

sudo docker run --name Alice --link Mint:Mint -t -i 
  -p 127.0.1.1:15048:5048 \
  -p 127.0.1.1:5901:5900 \
  -p 127.0.1.1:10001:10000 \
  -v /home/reed/docker/Alice:/aicoin \
  -v /etc/localtime:/etc/localtime:ro \
  -e DISPLAY=$DISPLAY \
  -v /tmp/.X11-unix:/tmp/.X11-unix:ro \
  aicoin-alice
