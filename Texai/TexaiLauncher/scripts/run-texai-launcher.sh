#!/bin/sh
# ****************************************************************************
# * run the Texai Launcher
# ****************************************************************************

export REPOSITORIES=/home/reed/repositories
export ARCHIVE_REPOSITORIES=/home/reed/archiveRepositories
export ARCHIVE_DATA=/home/reed/archiveData
export SECURITY_DIR=/home/reed

CLASSPATH=$CLASSPATH:lib/aduna-commons-collections-2.7.0.jar
CLASSPATH=$CLASSPATH:lib/aduna-commons-concurrent-2.6.0.jar
CLASSPATH=$CLASSPATH:lib/aduna-commons-i18n-1.3.0.jar
CLASSPATH=$CLASSPATH:lib/aduna-commons-io-2.8.0.jar
CLASSPATH=$CLASSPATH:lib/aduna-commons-iteration-2.8.0.jar
CLASSPATH=$CLASSPATH:lib/aduna-commons-lang-2.8.0.jar
CLASSPATH=$CLASSPATH:lib/aduna-commons-net-2.6.0.jar
CLASSPATH=$CLASSPATH:lib/aduna-commons-text-2.6.0.jar
CLASSPATH=$CLASSPATH:lib/aduna-commons-xml-2.6.0.jar
CLASSPATH=$CLASSPATH:lib/AlbusHCN-1.0.jar
CLASSPATH=$CLASSPATH:lib/AlbusHCNSupport-1.0.jar
CLASSPATH=$CLASSPATH:lib/ant-optional-1.5.1.jar
CLASSPATH=$CLASSPATH:lib/asm-3.1.jar
CLASSPATH=$CLASSPATH:lib/bcprov-jdk16-140.jar
CLASSPATH=$CLASSPATH:lib/BehaviorLanguage-1.0.jar
CLASSPATH=$CLASSPATH:lib/BehaviorLanguageSupport-1.0.jar
CLASSPATH=$CLASSPATH:lib/BitTorrentSupport-1.0.jar
CLASSPATH=$CLASSPATH:lib/cglib-2.2.jar
CLASSPATH=$CLASSPATH:lib/commons-beanutils-1.4.jar
CLASSPATH=$CLASSPATH:lib/commons-codec-1.3.jar
CLASSPATH=$CLASSPATH:lib/commons-collections-3.2.1.jar
CLASSPATH=$CLASSPATH:lib/commons-dbcp-1.2.2.jar
CLASSPATH=$CLASSPATH:lib/commons-httpclient-3.1.jar
CLASSPATH=$CLASSPATH:lib/commons-io-1.4.jar
CLASSPATH=$CLASSPATH:lib/commons-jxpath-1.1.jar
CLASSPATH=$CLASSPATH:lib/commons-logging-1.1.1.jar
CLASSPATH=$CLASSPATH:lib/commons-pool-1.5.3.jar
CLASSPATH=$CLASSPATH:lib/ehcache-core-2.2.0.jar
CLASSPATH=$CLASSPATH:lib/IncrementalFCG-1.0.jar
CLASSPATH=$CLASSPATH:lib/Inference-1.0.jar
CLASSPATH=$CLASSPATH:lib/jalopy-1.5rc3.jar
CLASSPATH=$CLASSPATH:lib/JavaComposition-1.0.jar
CLASSPATH=$CLASSPATH:lib/Javasysmon-1.0.jar
CLASSPATH=$CLASSPATH:lib/JAWS-1.0.jar
CLASSPATH=$CLASSPATH:lib/jaxme2-0.5.2.jar
CLASSPATH=$CLASSPATH:lib/jaxmeapi-0.5.2.jar
CLASSPATH=$CLASSPATH:lib/jaxmejs-0.5.2.jar
CLASSPATH=$CLASSPATH:lib/jaxmexs-0.5.2.jar
CLASSPATH=$CLASSPATH:lib/jdom-b8.jar
CLASSPATH=$CLASSPATH:lib/joda-time-1.6.jar
CLASSPATH=$CLASSPATH:lib/junit-4.7.jar
CLASSPATH=$CLASSPATH:lib/log4j-1.2.15.jar
CLASSPATH=$CLASSPATH:lib/log4j-over-slf4j-1.5.6.jar
CLASSPATH=$CLASSPATH:lib/netty-3.3.1.Final.jar
CLASSPATH=$CLASSPATH:lib/Network-1.0.jar
CLASSPATH=$CLASSPATH:lib/OpenChord-1.0.jar
CLASSPATH=$CLASSPATH:lib/persistence-api-1.0.jar
CLASSPATH=$CLASSPATH:lib/RDFEntityManager-1.0.jar
CLASSPATH=$CLASSPATH:lib/servlet-api-2.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-http-client-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-http-protocol-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-model-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-query-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-queryalgebra-evaluation-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-queryalgebra-model-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-queryparser-api-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-queryparser-serql-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-queryparser-sparql-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-queryresultio-api-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-queryresultio-binary-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-queryresultio-sparqljson-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-queryresultio-sparqlxml-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-queryresultio-text-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-repository-api-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-repository-contextaware-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-repository-dataset-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-repository-event-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-repository-http-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-repository-manager-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-repository-sail-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-rio-api-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-rio-n3-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-rio-ntriples-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-rio-rdfxml-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-rio-trig-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-rio-trix-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-rio-turtle-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-runtime-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-sail-api-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-sail-inferencer-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-sail-memory-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-sail-nativerdf-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/sesame-sail-rdbms-2.3.2.jar
CLASSPATH=$CLASSPATH:lib/Skills-1.0.jar
CLASSPATH=$CLASSPATH:lib/slf4j-api-1.5.6.jar
CLASSPATH=$CLASSPATH:lib/slf4j-log4j12-1.5.6.jar
CLASSPATH=$CLASSPATH:lib/TexaiLauncher-1.0.jar
CLASSPATH=$CLASSPATH:lib/UPNPLib-1.0.jar
CLASSPATH=$CLASSPATH:lib/Utilities-1.0.jar
CLASSPATH=$CLASSPATH:lib/X509Security-1.0.jar
CLASSPATH=$CLASSPATH:lib/xerces-1.2.3.jar
CLASSPATH=$CLASSPATH:lib/xml-apis-1.0.b2.jar

rm screenlog.0

#SSL handshake debugging
#java -ea -Dlog4j.configuration=file://$PWD/log4j.properties -classpath $CLASSPATH -Djavax.net.debug=all org.texai.texaiLauncher.TexaiLauncher

java -ea -Dlog4j.configuration=file://$PWD/log4j.properties -classpath $CLASSPATH org.texai.texaiLauncher.TexaiLauncher
