#!/bin/sh
# ****************************************************************************
# * Runs the A.I.Coin Alice demo in a docker container.
# ****************************************************************************

sudo docker run -t -i -p 15048:5048 -v /home/reed/docker/Alice:/aicoin -v /etc/localtime:/etc/localtime:ro aicoin-alice