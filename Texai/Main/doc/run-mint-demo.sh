#!/bin/sh
# ****************************************************************************
# * Runs the A.I.Coin Mint demo in a docker container.
# ****************************************************************************

# remove the previous container named Mint
sudo docker ps -a | grep 'Mint' | awk '{print $1}' | xargs --no-run-if-empty sudo docker rm

sudo docker run --name Mint -t -i -p 127.0.1.1:35048:5048 -v /home/reed/docker/Mint:/aicoin -v /etc/localtime:/etc/localtime:ro aicoin-mint