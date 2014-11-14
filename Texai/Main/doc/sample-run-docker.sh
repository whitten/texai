#!/bin/sh
# ****************************************************************************
# * Runs the A.I.Coin Alice demo in a docker container.
# ****************************************************************************

# remove the previous container named Alice
sudo docker ps -a | grep 'Alice' | awk '{print $1}' | xargs --no-run-if-empty sudo docker rm

sudo docker run --name Alice --link Mint:Mint -t -i -p 127.0.1.1:15048:5048 -v /home/reed/docker/Alice:/aicoin -v /etc/localtime:/etc/localtime:ro aicoin-alice