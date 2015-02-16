#!/bin/sh

# Migrates certain A.I. Coin files from the development workstation to the server 
# in which this script is executed - a production or demonstration server. 

# --------------- Alice

# copy the docker libraries from minsky
cd ~/docker/Alice/docker-context
scp -r reed@minsky:docker/Alice/docker-context/Dockerfile .
sudo docker build -t=aicoin-alice .

# copy the Java Main jar from minsky
cd ~/docker/Alice/Main-1.0
rm -fr lib
scp -r reed@minsky:docker/Alice/Main-1.0/lib lib

# copy certain data files
cd ~/docker/Alice/Main-1.0/data
scp reed@minsky:docker/Alice/Main-1.0/data/nodes.xml .
scp reed@minsky:docker/Alice/Main-1.0/data/SeedNodeInfos.ser .

# copy the shell script that launches the Alice container
cd
scp reed@minsky:~/run-alice-demo.sh .
chmod a+x run-alice-demo.sh

# copy the shell script that launches the Java application from within the container
cd ~/docker/Alice
scp reed@minsky:docker/Alice/openbox-run-aicoin-main.sh .
chmod a+x openbox-run-aicoin-main.sh

# copy Mint's singleton configuration role's X.509 certificate
cd ~/docker/Alice/Main-1.0/data
cp ~/docker/Mint/Main-1.0/data/ContainerSingletonConfiguration.crt .


# --------------- Bob

# copy the docker libraries from minsky
cd ~/docker/Bob/docker-context
scp -r reed@minsky:docker/Bob/docker-context/Dockerfile .
sudo docker build -t=aicoin-bob .

# copy the Java Main jar from Alice - all containers run the same code
cd ~/docker/Bob/Main-1.0
rm -fr lib
cp -r ~/docker/Alice/Main-1.0/lib lib

# copy certain data files
cd ~/docker/Bob/Main-1.0/data
cp ~/docker/Alice/Main-1.0/data/nodes.xml .
cp ~/docker/Alice/Main-1.0/data/SeedNodeInfos.ser .

# copy the shell script that launches the Bob container
cd
scp reed@minsky:~/run-bob-demo.sh .
chmod a+x run-bob-demo.sh

# copy the shell script that launches the Java application from within the container
cd ~/docker/Bob
cp ../Alice/openbox-run-aicoin-main.sh .

# copy Mint's singleton configuration role's X.509 certificate
cd ~/docker/Bob/Main-1.0/data
cp ~/docker/Mint/Main-1.0/data/ContainerSingletonConfiguration.crt .


# --------------- Mint

# copy the docker libraries from minsky
cd ~/docker/Mint/docker-context
scp -r reed@minsky:docker/Mint/docker-context/Dockerfile .
sudo docker build -t=aicoin-mint .

# copy the Java Main jar from Alice - all containers run the same code
cd ~/docker/Mint/Main-1.0
rm -fr lib
cp -r ~/docker/Alice/Main-1.0/lib lib

# copy certain data files
cd ~/docker/Mint/Main-1.0/data
cp ~/docker/Alice/Main-1.0/data/nodes.xml .
cp ~/docker/Alice/Main-1.0/data/SeedNodeInfos.ser .


# copy the shell script that launches the Mint container
cd
scp reed@minsky:~/run-mint-demo.sh .
chmod a+x run-mint-demo.sh

# copy the shell script that launches the Java application from within the container
cd ~/docker/Mint
cp ../Alice/openbox-run-aicoin-main.sh .

# --------------- Blockchain Explorer

# copy the docker libraries from minsky
cd ~/docker/BlockchainExplorer/docker-context
scp -r reed@minsky:docker/BlockchainExplorer/docker-context/Dockerfile .
sudo docker build -t=aicoin-blockchain-explorer .

# copy the Java Main jar from Alice - all containers run the same code
cd ~/docker/BlockchainExplorer/Main-1.0
rm -fr lib
cp -r ~/docker/Alice/Main-1.0/lib lib

# copy certain data files
cd ~/docker/BlockchainExplorer/Main-1.0/data
cp ~/docker/Alice/Main-1.0/data/nodes.xml .
cp ~/docker/Alice/Main-1.0/data/SeedNodeInfos.ser .

# copy the shell script that launches the Mint container
cd
scp reed@minsky:~/run-blockchain-explorer-demo.sh .
chmod a+x run-blockchain-explorer-demo.sh

# copy the shell script that launches the Java application from within the container
cd ~/docker/BlockchainExplorer
cp ../Alice/openbox-run-aicoin-main.sh .

# copy Mint's singleton configuration role's X.509 certificate
cd ~/docker/BlockchainExplorer/Main-1.0/data
cp ~/docker/Mint/Main-1.0/data/ContainerSingletonConfiguration.crt .




