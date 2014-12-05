#!/bin/sh

# Launch a VNC server so that aicoin-qt can be remotely accessed using a VNC viewer on the docker host.

# start framebuffer display server
Xvfb :1 -extension GLX -screen 0 1024x768x24 &

# export DISPLAY or you have to use "DISPLAY=:1 /usr/bin/cmd"
export DISPLAY=:1

# Wait a bit until the X server is ready
sleep 1

x11vnc -display :1 -forever -usepw -create -v -o x11vnc.log

# To make sure we can restart this container properly
# Fatal server error: Server is already active for display 1
# If this server is no longer running, remove /tmp/.X1-lock
rm /tmp/.X1-lock


#   Connect to a remote desktop using Remote Desktop Viewer
#   Server(:port)  127.0.1.1:5903 (mint)
#   Password:      1234

#   In the VNC client terminal window enter 'openbox &'.

