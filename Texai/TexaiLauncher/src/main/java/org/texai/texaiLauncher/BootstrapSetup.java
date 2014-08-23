/*
 * BootstrapSetup.java
 *
 * Created on Oct 21, 2011, 1:40:24 PM
 *
 * Description: Creates the initial node runtime configuration, and clears node and role repositories.
 *
 * Copyright (C) Oct 21, 2011, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.texaiLauncher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.jcip.annotations.NotThreadSafe;
import net.sf.ehcache.CacheManager;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.texai.ahcs.impl.NodeRuntimeImpl;
import org.texai.ahcsSupport.NodeTypeInitializer;
import org.texai.ahcsSupport.RoleTypeInitializer;
import org.texai.ahcsSupport.domainEntity.NodeRuntimeConfiguration;
import org.texai.kb.CacheInitializer;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFEntityPersister;
import org.texai.texaiLauncher.domainEntity.NodeRuntimeInfo;
import org.texai.texaiLauncher.domainEntity.TexaiLauncherInfo;
import org.texai.util.NetworkUtils;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import org.texai.x509.X509Utils;

/** Creates the initial node runtime configuration, and clears node and role repositories.
 *
 * @author reed
 */
@NotThreadSafe
public class BootstrapSetup {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(BootstrapSetup.class);
  /** the indicator whether finalization has occurred */
  private AtomicBoolean isFinalized = new AtomicBoolean(false);
  /** the shutdown hook */
  private ShutdownHook shutdownHook;
  /** the RDF entity manager */
  private RDFEntityManager rdfEntityManager;
  /** the entry alias for the client X509 certificate */
  private static final String CLIENT_ENTRY_ALIAS = "client";
  /** the password for the  client keystore */
  private static final char[] CLIENT_KEYSTORE_PASSWORD = "client-keystore-password".toCharArray();

  /** Constructs a new BootstrapSetup instance. */
  public BootstrapSetup() {
  }

  /** Initializes this application. */
  private void initialization() {
    // configure a shutdown hook to run the finalization method in case the JVM is abnormally ended
    shutdownHook = new ShutdownHook();
    Runtime.getRuntime().addShutdownHook(shutdownHook);

    CacheInitializer.initializeCaches();
    rdfEntityManager = new RDFEntityManager();
  }

  /** Creates the initial node runtime configuration, and clears node and role repositories. */
  private void process() {
    if (!X509Utils.isTrustedDevelopmentSystem()) {
      throw new TexaiException("bootstrap setup must execute on the trusted developement system");
    }
    assert X509Utils.isJCEUnlimitedStrengthPolicy() : "the JCE unlimited strength jurisdiction policy files must be installed";

    // launcher bootstrap setup ...

    // delete the launcher keystore
    final String basePath;
    if (System.getProperty("org.texai.basepath") == null) {
      basePath = ".";
    } else {
      basePath = System.getProperty("org.texai.basepath");
    }
    LOGGER.info("basePath: " + basePath);
    final String launcherKeyStoreFilePath = basePath + "/data/launcher-keystore.uber";
    LOGGER.info("keyStoreFilePath " + launcherKeyStoreFilePath);
    final File launcherKeyStoreFile = new File(launcherKeyStoreFilePath);
    LOGGER.info("launcherKeyStoreFilePath " + launcherKeyStoreFilePath);
    if (launcherKeyStoreFile.exists()) {
      LOGGER.info("deleting existing launcher keystore " + launcherKeyStoreFile);
      boolean isDeleted = launcherKeyStoreFile.delete();
      if (!isDeleted) {
        throw new TexaiException("launcherKeyStoreFile not deleted");
      }
    }

    final TexaiLauncherAccess texaiLauncherAccess = new TexaiLauncherAccess(rdfEntityManager);
    // configure logging
    Logger.getLogger(RDFEntityPersister.class).setLevel(Level.WARN);

    // clear the launcher repository
    LOGGER.info("clearing launcher repository");
    DistributedRepositoryManager.clearNamedRepository("Launcher");

    // initialize the bootstrap node runtime
    final NodeRuntimeInfo nodeRuntimeInfo = new NodeRuntimeInfo();
    // There will be one launcher per LAN, and its external and internal ports will be 5048 - the Texai port. The node
    // runtimes, executing possibly multiple instances per host, will each have a dynamically assigned port used for both
    // internal and external mapping.
    final int port = NetworkUtils.getRandomDynamicServerPort();
    nodeRuntimeInfo.setInternalPort(port);
    nodeRuntimeInfo.setExternalPort(port);
    rdfEntityManager.persist(nodeRuntimeInfo);
    assert nodeRuntimeInfo.getId() != null;

    // initialize the persistent launcher information
    final TexaiLauncherInfo texaiLauncherInfo = new TexaiLauncherInfo();
    final List<NodeRuntimeInfo> nodeRuntimeInfos = new ArrayList<>();
    nodeRuntimeInfos.add(nodeRuntimeInfo);
    texaiLauncherInfo.setNodeRuntimeInfos(nodeRuntimeInfos);
    rdfEntityManager.persist(texaiLauncherInfo);

    // initialize the launcher keystore
    LOGGER.info("creating the Texai Launcher keystore");
    try {
      // the launcher keystore consists of the single client X.509 certificate, which is generated and signed by
      // the Texai root certificate on the developement system that has the root private key.
      final KeyPair launcherKeyPair = X509Utils.generateRSAKeyPair2048();
      final X509Certificate launcherX509Certificate = X509Utils.generateX509Certificate(
              launcherKeyPair.getPublic(), // myPublicKey
              X509Utils.getRootPrivateKey(), // issuerPrivateKey
              X509Utils.getRootX509Certificate(), // issuerCertificate
              texaiLauncherInfo.getUUID(), // uid
              "TexaiLauncher"); // domainComponent
      LOGGER.info("launcher certificate:\n" + launcherX509Certificate);
      texaiLauncherAccess.persistTexaiLauncherInfo(texaiLauncherInfo);

      LOGGER.info("creating launcher-keystore.uber");
      KeyStore launcherKeyStore = launcherKeyStore = X509Utils.findOrCreateKeyStore(
              launcherKeyStoreFilePath,
              TexaiLauncher.LAUNCHER_KEY_STORE_PASSWORD);
      launcherKeyStore.setKeyEntry(
              texaiLauncherInfo.getUUID().toString(), // alias
              launcherKeyPair.getPrivate(),
              TexaiLauncher.LAUNCHER_KEY_STORE_PASSWORD,
              new Certificate[]{launcherX509Certificate, X509Utils.getRootX509Certificate()});
      launcherKeyStore.store(new FileOutputStream(launcherKeyStoreFilePath), TexaiLauncher.LAUNCHER_KEY_STORE_PASSWORD);
      if (launcherKeyStoreFile.exists()) {
        LOGGER.info(launcherKeyStoreFile + " now exists");
      } else {
        assert false;
      }

    } catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | KeyStoreException | IOException | CertificateException ex) {
      LOGGER.error(StringUtils.getStackTraceAsString(ex));
      throw new TexaiException(ex);
    }

    // node runtime bootstrap setup ...

    // clear the node and role repositories
    LOGGER.info("clearing node and role repositories");
    DistributedRepositoryManager.clearNamedRepository("Nodes");
    DistributedRepositoryManager.clearNamedRepository("NodeRoleTypes");

    // load the repositories with the node and role types
    LOGGER.info("reloading node types and role types");
    final RoleTypeInitializer roleTypeInitializer = new RoleTypeInitializer();
    roleTypeInitializer.initialize(rdfEntityManager);
    if ((new File("data/role-types.xml")).exists()) {
      roleTypeInitializer.process("data/role-types.xml");
    } else {
      roleTypeInitializer.process("../Main/data/role-types.xml");
    }
    roleTypeInitializer.finalization();
    final NodeTypeInitializer nodeTypeInitializer = new NodeTypeInitializer();
    nodeTypeInitializer.initialize(rdfEntityManager);
    if ((new File("data/node-types.xml")).exists()) {
      nodeTypeInitializer.process("data/node-types.xml");
    } else {
      nodeTypeInitializer.process("../Main/data/node-types.xml");
    }
    nodeTypeInitializer.finalization();

    // delete the node runtime keystore
    final String nodeRuntimeKeyStoreFilePath = basePath + "/data/node-runtime-keystore.uber";
    LOGGER.info("keyStoreFilePath " + nodeRuntimeKeyStoreFilePath);
    final File nodeRuntimeKeyStoreFile = new File(nodeRuntimeKeyStoreFilePath);
    LOGGER.info("nodeRuntimeKeyStoreFilePath " + nodeRuntimeKeyStoreFilePath);
    if (nodeRuntimeKeyStoreFile.exists()) {
      LOGGER.info("deleting existing node runtime keystore " + nodeRuntimeKeyStoreFile);
      boolean isDeleted = nodeRuntimeKeyStoreFile.delete();
      if (!isDeleted) {
        throw new TexaiException("nodeRuntimeKeyStoreFile not deleted");
      }
    }

    // delete the role keystore
    final String roleKeyStoreFilePath = basePath + "/data/role-keystore.uber";
    LOGGER.info("keyStoreFilePath " + roleKeyStoreFilePath);
    final File roleKeyStoreFile = new File(roleKeyStoreFilePath);
    LOGGER.info("roleKeyStoreFilePath " + roleKeyStoreFilePath);
    if (roleKeyStoreFile.exists()) {
      LOGGER.info("deleting existing node runtime keystore " + roleKeyStoreFile);
      boolean isDeleted = roleKeyStoreFile.delete();
      if (!isDeleted) {
        throw new TexaiException("roleKeyStoreFile not deleted");
      }
    }

    try {
      // create an empty role keystore, in which to store the intermediate X.509 certificate
      X509Utils.findOrCreateKeyStore(roleKeyStoreFilePath, NodeRuntimeImpl.KEY_STORE_PASSWORD);

      // generate an intermediate X.509 certificate for signing the remaining certificates
      LOGGER.info("generating public and private keys for the certificate-signing certificate");
      final X509Certificate rootX509Certificate = X509Utils.getRootX509Certificate();
      final KeyPair certificateSigningKeyPair = X509Utils.generateRSAKeyPair3072();
      final PublicKey certificateSigningPublicKey = certificateSigningKeyPair.getPublic();
      final PrivateKey certificateAuthorityPrivateKey = X509Utils.getRootPrivateKey();
      LOGGER.info("generating intermediate certificate-signing certificate");
      final X509Certificate certificateSigningX509Certificate = X509Utils.generateIntermediateX509Certificate(
              certificateSigningPublicKey,
              certificateAuthorityPrivateKey,
              rootX509Certificate,
              0); // pathLengthConstraint
      final String certificateSigningKeyStoreEntryAlias = X509Utils.getUUID(certificateSigningX509Certificate).toString();
      certificateSigningX509Certificate.verify(rootX509Certificate.getPublicKey());
      LOGGER.info("intermediate certificate-signing certificate:\n" + certificateSigningX509Certificate);
      final List<Certificate> certificateList = new ArrayList<>();
      certificateList.add(certificateSigningX509Certificate);
      final CertPath certificateSigningCertPath = X509Utils.generateCertPath(certificateList);
      X509Utils.validateCertificatePath(certificateSigningCertPath);
      // update loaded keystore
      final KeyStore roleKeyStore = X509Utils.addEntryToKeyStore(
              roleKeyStoreFilePath,
              NodeRuntimeImpl.KEY_STORE_PASSWORD,
              certificateSigningKeyStoreEntryAlias, // alias
              certificateSigningCertPath,
              certificateSigningKeyPair.getPrivate()); // privateKey

      // generate an X.509 certificate for the node runtime, which is used to sign the message that starts up the bootstrap role
      LOGGER.info("generating public and private keys for the node runtime certificate");
      final KeyPair nodeRuntimeKeyPair = X509Utils.generateRSAKeyPair2048();
      LOGGER.info("creating the node runtime's certificate");
      final CertPath nodeRuntimeCertPath = X509Utils.generateX509CertificatePath(
              nodeRuntimeKeyPair.getPublic(),
              certificateSigningKeyPair.getPrivate(),
              certificateSigningX509Certificate,
              certificateSigningCertPath,
              "NodeRuntime"); // domainComponent
      assert !nodeRuntimeCertPath.getCertificates().isEmpty();
      final X509Certificate nodeRuntimeX509Certificate = (X509Certificate) nodeRuntimeCertPath.getCertificates().get(0);
      nodeRuntimeX509Certificate.verify(certificateSigningX509Certificate.getPublicKey());
      LOGGER.info("node runtime certificate:\n" + nodeRuntimeX509Certificate);
      final String nodeRuntimeKeyStoreEntryAlias = X509Utils.getUUID(nodeRuntimeX509Certificate).toString();
      LOGGER.info("  nodeRuntimeKeyStoreEntryAlias: " + nodeRuntimeKeyStoreEntryAlias);
        X509Utils.validateCertificatePath(nodeRuntimeCertPath);
      // update loaded keystore
      final KeyStore nodeRuntimeKeyStore = X509Utils.addEntryToKeyStore(
              nodeRuntimeKeyStoreFilePath,
              NodeRuntimeImpl.KEY_STORE_PASSWORD,
              nodeRuntimeKeyStoreEntryAlias, // alias
              nodeRuntimeCertPath,
              nodeRuntimeKeyPair.getPrivate()); // privateKey

      // create and persist the NodeRuntimeConfiguration used by the node runtime
      LOGGER.info("creating node runtime configuration for " + nodeRuntimeInfo.getId());
      final NodeRuntimeConfiguration nodeRuntimeConfiguration = new NodeRuntimeConfiguration(nodeRuntimeInfo.getId());
      nodeRuntimeConfiguration.setCertificateSigningKeyStoreEntryAlias(certificateSigningKeyStoreEntryAlias);
      nodeRuntimeConfiguration.setNodeRuntimeKeyStoreEntryAlias(nodeRuntimeKeyStoreEntryAlias);
      rdfEntityManager.persist(nodeRuntimeConfiguration);
      LOGGER.info("bootstrap node runtime configurations are ...");
      final Iterator<NodeRuntimeConfiguration> nodeRuntimeConfiguration_iter = rdfEntityManager.rdfEntityIterator(NodeRuntimeConfiguration.class);
      while (nodeRuntimeConfiguration_iter.hasNext()) {
        LOGGER.info("  " + nodeRuntimeConfiguration_iter.next());
      }

      final Enumeration<String> aliases = nodeRuntimeKeyStore.aliases();
      while (aliases.hasMoreElements()) {
        final String existingAlias = aliases.nextElement();
        LOGGER.info("  node runtime X509 certificate alias: " + existingAlias);
      }

    // create an X509 certificate for importing into the client's web browser
      LOGGER.info("creating the client certificate");
      final KeyPair clientKeyPair = X509Utils.generateRSAKeyPair2048();
      final X509Certificate clientX509Certificate = X509Utils.generateX509Certificate(
              clientKeyPair.getPublic(), // myPublicKey
              X509Utils.getRootPrivateKey(), // issuerPrivateKey
              X509Utils.getRootX509Certificate(), // issuerCertificate
              texaiLauncherInfo.getUUID(), // uid
              "client"); // domainComponent
      LOGGER.info("client certificate:\n" + clientX509Certificate);

      final String pkcs12FilePath = "data/texai-client.p12";
      final KeyStore pkcs12Truststore = X509Utils.findOrCreatePKCS12KeyStore(pkcs12FilePath, CLIENT_KEYSTORE_PASSWORD);
      pkcs12Truststore.setCertificateEntry(
              CLIENT_ENTRY_ALIAS,
              clientX509Certificate);
      pkcs12Truststore.store(new FileOutputStream(pkcs12FilePath), CLIENT_KEYSTORE_PASSWORD);
    } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | CertPathValidatorException | NoSuchProviderException | SignatureException | InvalidKeyException | IOException | KeyStoreException | CertificateException ex) {
      throw new TexaiException(ex);
    }

    // write the root X509 certificate for importing into the client's web browser as a trusted authority
    try {
      LOGGER.info("root certificate ...\n" + X509Utils.getRootX509Certificate());
      X509Utils.writeX509Certificate(X509Utils.getRootX509Certificate(), "data/texai-root-certificate.crt");
    } catch (CertificateEncodingException | IOException ex) {
      throw new TexaiException(ex);
    }

  }

  /** Finalizes this application and releases its resources. */
  private void finalization() {
    isFinalized.getAndSet(true);
    LOGGER.info("finalization");
    if (rdfEntityManager != null) {
      rdfEntityManager.close();
    }

    CacheManager.getInstance().shutdown();
    DistributedRepositoryManager.shutDown();
    LOGGER.info("BootstrapSetup is completed");
  }

  /** Provides a shutdown hook to finalize resources when the JVM is unexpectedly shutdown. */
  class ShutdownHook extends Thread {

    @Override
    public void run() {
      Thread.currentThread().setName("shutdown");
      if (!isFinalized.get()) {
        LOGGER.warn("***** shutdown, finalizing resources *****");
        finalization();
      }
    }
  }

  /** Installs the repositories. */
  public static void installRepositories() {
    if (!NetworkUtils.isDistributedTexaiInstance()) {
      return;
    }
    // TODO incomplete for the distributed version of Texai ...
    // assume root
    //    copy repositories
    final String repositoriesSourcePath = System.getenv("REPOSITORIES");
    LOGGER.info("repositoriesSourcePath: " + repositoriesSourcePath);
    final String repositoriesDestinationPath = "data/repositories";
    LOGGER.info("repositoriesDestinationPath: " + repositoriesDestinationPath);
    try {
      File sourceDirectory = new File(repositoriesSourcePath + "/AmericanEnglishConstructionAndLexicalCategoryRules");
      File destinationDirectory = new File(repositoriesDestinationPath + "/AmericanEnglishConstructionAndLexicalCategoryRules");
      LOGGER.info("copying AmericanEnglishConstructionAndLexicalCategoryRules repository");
      FileUtils.copyDirectory(sourceDirectory, destinationDirectory);

      sourceDirectory = new File(repositoriesSourcePath + "/AmericanEnglishGrammarUnitTestSpecifications");
      destinationDirectory = new File(repositoriesDestinationPath + "/AmericanEnglishGrammarUnitTestSpecifications");
      LOGGER.info("copying AmericanEnglishGrammarUnitTestSpecifications repository");
      FileUtils.copyDirectory(sourceDirectory, destinationDirectory);

      sourceDirectory = new File(repositoriesSourcePath + "/AmericanEnglishLexicalStemRules");
      destinationDirectory = new File(repositoriesDestinationPath + "/AmericanEnglishLexicalStemRules");
      LOGGER.info("copying AmericanEnglishLexicalStemRules repository");
      FileUtils.copyDirectory(sourceDirectory, destinationDirectory);

      sourceDirectory = new File(repositoriesSourcePath + "/AmericanEnglishMorphologicalRules");
      destinationDirectory = new File(repositoriesDestinationPath + "/AmericanEnglishMorphologicalRules");
      LOGGER.info("copying AmericanEnglishMorphologicalRules repository");
      FileUtils.copyDirectory(sourceDirectory, destinationDirectory);

      sourceDirectory = new File(repositoriesSourcePath + "/AmericanEnglishWordFrames");
      destinationDirectory = new File(repositoriesDestinationPath + "/AmericanEnglishWordFrames");
      LOGGER.info("copying AmericanEnglishWordFrames repository");
      FileUtils.copyDirectory(sourceDirectory, destinationDirectory);

      sourceDirectory = new File(repositoriesSourcePath + "/BehaviorLanguageCapabilities");
      destinationDirectory = new File(repositoriesDestinationPath + "/BehaviorLanguageCapabilities");
      LOGGER.info("copying BehaviorLanguageCapabilities repository");
      FileUtils.copyDirectory(sourceDirectory, destinationDirectory);

      sourceDirectory = new File(repositoriesSourcePath + "/BehaviorLanguageJavaCompositions");
      destinationDirectory = new File(repositoriesDestinationPath + "/BehaviorLanguageJavaCompositions");
      LOGGER.info("copying BehaviorLanguageJavaCompositions repository");
      FileUtils.copyDirectory(sourceDirectory, destinationDirectory);

      sourceDirectory = new File(repositoriesSourcePath + "/ConceptuallyRelatedTerms");
      destinationDirectory = new File(repositoriesDestinationPath + "/ConceptuallyRelatedTerms");
      LOGGER.info("copying ConceptuallyRelatedTerms repository");
      FileUtils.copyDirectory(sourceDirectory, destinationDirectory);

      sourceDirectory = new File(repositoriesSourcePath + "/DialogWordStemUsage");
      destinationDirectory = new File(repositoriesDestinationPath + "/DialogWordStemUsage");
      LOGGER.info("copying DialogWordStemUsage repository");
      FileUtils.copyDirectory(sourceDirectory, destinationDirectory);

      sourceDirectory = new File(repositoriesSourcePath + "/InferenceRules");
      destinationDirectory = new File(repositoriesDestinationPath + "/InferenceRules");
      LOGGER.info("copying InferenceRules repository");
      FileUtils.copyDirectory(sourceDirectory, destinationDirectory);

      sourceDirectory = new File(repositoriesSourcePath + "/ScriptedBehaviorLanguage");
      destinationDirectory = new File(repositoriesDestinationPath + "/ScriptedBehaviorLanguage");
      LOGGER.info("copying ScriptedBehaviorLanguage repository");
      FileUtils.copyDirectory(sourceDirectory, destinationDirectory);

      sourceDirectory = new File(repositoriesSourcePath + "/NodeRoleTypes");
      destinationDirectory = new File(repositoriesDestinationPath + "/NodeRoleTypes");
      LOGGER.info("copying NodeRoleTypes repository");
      FileUtils.copyDirectory(sourceDirectory, destinationDirectory);

      sourceDirectory = new File(repositoriesSourcePath + "/OpenCyc");
      destinationDirectory = new File(repositoriesDestinationPath + "/OpenCyc");
      LOGGER.info("copying OpenCyc repository");
      FileUtils.copyDirectory(sourceDirectory, destinationDirectory);

      sourceDirectory = new File(repositoriesSourcePath + "/TinyACT-R");
      destinationDirectory = new File(repositoriesDestinationPath + "/TinyACT-R");
      LOGGER.info("copying TinyACT-R repository");
      FileUtils.copyDirectory(sourceDirectory, destinationDirectory);

      sourceDirectory = new File(repositoriesSourcePath + "/RegisteredUsers");
      destinationDirectory = new File(repositoriesDestinationPath + "/RegisteredUsers");
      LOGGER.info("copying RegisteredUsers repository");
      FileUtils.copyDirectory(sourceDirectory, destinationDirectory);

      sourceDirectory = new File(repositoriesSourcePath + "/TexaiEnglishLexicon");
      destinationDirectory = new File(repositoriesDestinationPath + "/TexaiEnglishLexicon");
      LOGGER.info("copying TexaiEnglishLexicon repository");
      FileUtils.copyDirectory(sourceDirectory, destinationDirectory);

    } catch (IOException ex) {
      LOGGER.warn(StringUtils.getStackTraceAsString(ex));
      throw new TexaiException(ex);
    }
  }

  /** Executes this application.
   *
   * @param args the command line arguments - unused
   */
  public static void main(final String[] args) {
    BootstrapSetup bootstrapSetup = new BootstrapSetup();
    bootstrapSetup.initialization();
    bootstrapSetup.process();
    bootstrapSetup.finalization();
  }
}
