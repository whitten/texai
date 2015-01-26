#!/bin/sh

# Launch AIMain - an instance in the A.I. Coin cyptocurrency network.
# Each instance should execute in a separate Docker container.

cd Main-1.0

# start the openbox window manager for the VNC session
openbox &> /tmp/openbox.log &

# ensure that the JCE security policy files are installed
cp ../UnlimitedJCEPolicyJDK8/*.jar /usr/lib/jvm/java-8-oracle/jre/lib/security

CLASSPATH=lib/activation-1.1.jar
CLASSPATH=$CLASSPATH:lib/aduna-commons-collections-2.7.0.jar

CLASSPATH=lib/AlbusHCN-1.0.jar
CLASSPATH=$CLASSPATH:lib/AlbusHCNSupport-1.0.jar
CLASSPATH=$CLASSPATH:lib/ant-optional-1.5.1.jar
CLASSPATH=$CLASSPATH:lib/asm-3.1.jar
CLASSPATH=$CLASSPATH:lib/bcpkix-jdk14-1.50.jar
CLASSPATH=$CLASSPATH:lib/bcprov-jdk14-1.50.jar
CLASSPATH=$CLASSPATH:lib/bcprov-jdk15on-1.50.jar
CLASSPATH=$CLASSPATH:lib/bitcoinj-0.11.3.jar
CLASSPATH=$CLASSPATH:lib/BitTorrentSupport-1.0.jar
CLASSPATH=$CLASSPATH:lib/cglib-2.2.jar
CLASSPATH=$CLASSPATH:lib/CoinSkills-1.0.jar
CLASSPATH=$CLASSPATH:lib/commons-beanutils-1.4.jar
CLASSPATH=$CLASSPATH:lib/commons-codec-1.4.jar
CLASSPATH=$CLASSPATH:lib/commons-collections-3.2.1.jar
CLASSPATH=$CLASSPATH:lib/commons-dbcp-1.3.jar
CLASSPATH=$CLASSPATH:lib/commons-httpclient-3.1.jar
CLASSPATH=$CLASSPATH:lib/commons-io-2.4.jar
CLASSPATH=$CLASSPATH:lib/commons-jxpath-1.1.jar
CLASSPATH=$CLASSPATH:lib/commons-logging-1.1.1.jar
CLASSPATH=$CLASSPATH:lib/commons-pool-1.5.3.jar
CLASSPATH=$CLASSPATH:lib/ehcache-core-2.2.0.jar
CLASSPATH=$CLASSPATH:lib/guava-13.0.1.jar
CLASSPATH=$CLASSPATH:lib/Inference-1.0.jar
CLASSPATH=$CLASSPATH:lib/jackson-core-2.2.1.jar
CLASSPATH=$CLASSPATH:lib/javasysmon_2.10-0.3.4.jar
CLASSPATH=$CLASSPATH:lib/jaxme2-0.5.2.jar
CLASSPATH=$CLASSPATH:lib/jaxmeapi-0.5.2.jar
CLASSPATH=$CLASSPATH:lib/jaxmejs-0.5.2.jar
CLASSPATH=$CLASSPATH:lib/jaxmexs-0.5.2.jar
CLASSPATH=$CLASSPATH:lib/jcip-annotations-1.0.jar
CLASSPATH=$CLASSPATH:lib/jdom-b8.jar
CLASSPATH=$CLASSPATH:lib/joda-time-1.6.jar
CLASSPATH=$CLASSPATH:lib/json-simple-1.1.1.jar
CLASSPATH=$CLASSPATH:lib/jsr305-1.3.9.jar
CLASSPATH=$CLASSPATH:lib/junit-4.7.jar
CLASSPATH=$CLASSPATH:lib/log4j-1.2.15.jar
CLASSPATH=$CLASSPATH:lib/log4j-over-slf4j-1.5.6.jar
CLASSPATH=$CLASSPATH:lib/Main-1.0.jar
CLASSPATH=$CLASSPATH:lib/netty-3.3.1.Final.jar
CLASSPATH=$CLASSPATH:lib/Network-1.0.jar
CLASSPATH=$CLASSPATH:lib/opencsv-2.0.jar
CLASSPATH=$CLASSPATH:lib/persistence-api-1.0.jar
CLASSPATH=$CLASSPATH:lib/protobuf-java-2.5.0.jar
CLASSPATH=$CLASSPATH:lib/RDFEntityManager-1.0.jar
CLASSPATH=$CLASSPATH:lib/scala-library-2.10.3.jar
CLASSPATH=$CLASSPATH:lib/sc-light-jdk15on-1.47.0.2.jar
CLASSPATH=$CLASSPATH:lib/scrypt-1.3.3.jar
CLASSPATH=$CLASSPATH:lib/servlet-api-2.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-http-client-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-http-protocol-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-model-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-query-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-queryalgebra-evaluation-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-queryalgebra-model-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-queryparser-api-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-queryparser-serql-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-queryparser-sparql-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-queryresultio-api-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-queryresultio-binary-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-queryresultio-sparqljson-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-queryresultio-sparqlxml-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-queryresultio-text-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-repository-api-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-repository-contextaware-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-repository-dataset-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-repository-event-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-repository-http-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-repository-manager-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-repository-sail-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-repository-sparql-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-rio-api-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-rio-binary-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-rio-datatypes-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-rio-languages-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-rio-n3-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-rio-nquads-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-rio-ntriples-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-rio-rdfjson-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-rio-rdfxml-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-rio-trig-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-rio-trix-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-rio-turtle-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-runtime-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-sail-api-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-sail-federation-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-sail-inferencer-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-sail-memory-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-sail-nativerdf-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-sail-rdbms-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/sesame-util-2.7.12.jar
CLASSPATH=$CLASSPATH:lib/Skills-1.0.jar
CLASSPATH=$CLASSPATH:lib/slf4j-api-1.5.6.jar
CLASSPATH=$CLASSPATH:lib/slf4j-log4j12-1.5.6.jar
CLASSPATH=$CLASSPATH:lib/TamperEvidentLog-1.0.jar
CLASSPATH=$CLASSPATH:lib/temboosdk-1.0.jar
CLASSPATH=$CLASSPATH:lib/UPNPLib-1.0.jar
CLASSPATH=$CLASSPATH:lib/Utilities-1.0.jar
CLASSPATH=$CLASSPATH:lib/X509Security-1.0.jar
CLASSPATH=$CLASSPATH:lib/xerces-1.2.3.jar
CLASSPATH=$CLASSPATH:lib/xml-apis-1.0.b2.jar

# migrate new version of aicoin-cli
if [ -f bin/aicoin-cli-new ] ; then
  echo "migrating new version of aicoin-cli"
  rm bin/aicoin-cli
  mv bin/aicoin-cli-new bin/aicoin-cli
fi

# migrate new version of aicoin-qt
if [ -f bin/aicoin-qt-new ] ; then
  echo "migrating new version of aicoin-qt"
  rm bin/aicoin-qt
  mv bin/aicoin-qt-new bin/aicoin-qt
fi


# debug logger configuration
#java -ea -Dlog4j.configuration=file://$PWD/log4j.properties -Dlog4j.debug=true -classpath $CLASSPATH org.texai.main.AICoinMain

# debug SSL handshake
#java -ea -Dlog4j.configuration=file://$PWD/log4j.properties -classpath $CLASSPATH -Djavax.net.debug=all org.texai.main.AICoinMain

# restart automatically following an automatic software deployment
while java -ea -Dlog4j.configuration=file://$PWD/log4j.properties -classpath $CLASSPATH org.texai.main.AICoinMain 2>&1 | tee ../console.log ; do
  echo "Restarting AI Coin Peer"
  sleep 1
done
echo "AI Coin Peer terminated with an error"

