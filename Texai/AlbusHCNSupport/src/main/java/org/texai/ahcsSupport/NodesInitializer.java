/*
 * NodesInitializer.java
 *
 * Created on Sep 17, 2014, 8:29:22 PM
 *
 * Description: Initializes the Nodes repository from an XML file.
 *
 */
package org.texai.ahcsSupport;

import org.texai.ahcsSupport.skill.BasicNodeRuntime;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.ahcsSupport.domainEntity.Role;
import org.texai.ahcsSupport.domainEntity.SkillClass;
import org.texai.kb.CacheInitializer;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.util.ArraySet;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import org.texai.x509.MessageDigestUtils;
import org.texai.x509.X509SecurityInfo;
import org.texai.x509.X509Utils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Initializes node types from an XML file.
 *
 * @author reed
 */
@NotThreadSafe
public final class NodesInitializer {

  // the log4j logger
  private static final Logger LOGGER = Logger.getLogger(NodesInitializer.class);
  // the node access
  private final NodeAccess nodeAccess;
  // the node field holder dictionary, qualifiedName --> NodeFieldsHolder
  private final Map<String, NodeFieldsHolder> nodeFieldsHolderDictionary = new HashMap<>();
  // the role field holder dictionary, qualified qualifiedName --> RoleFieldsHolder
  private final Map<String, RoleFieldsHolder> roleFieldsHolderDictionary = new HashMap<>();
  // the current node field holder
  private NodeFieldsHolder nodeFieldsHolder;
  // the current role field holder
  private RoleFieldsHolder roleFieldsHolder;
  // indicates whether the given skill class is tested for existence in the classpath - false
  // for some unit tests
  private final boolean isClassExistsTested;
  // the container name, which is unique in the network
  private final String containerName;
  // the nodes file SHA-512 hash encoded as a base 64 string, used to detect tampering
  private String nodesFileHashString;
  // the keystore password, which is not persisted
  private final char[] keyStorePassword;
  // the node runtime
  private final BasicNodeRuntime nodeRuntime;
  // the loaded nodes
  private final List<Node> nodes = new ArrayList<>();
  // the count of </node> tags
  private int nbrNodeTags = 0;
  // the keystore path, which is different for test vs production
  private final String keyStoreFilePath;
  // the output path for the configuration role's certificate, which is different for test vs production
  private final String configurationCertificateFilePath;
  // the X.509 certificate for the SingletonConfiguration role
  private X509Certificate singletonConfigurationCertificate;
  // the key store that contains the X.509 certificates
  private KeyStore keyStore;

  /**
   * Constructs a new NodeInitializer instance.
   *
   * @param isClassExistsTested indicates whether the given skill class is tested for existence in the classpath - false for some unit
   * tests.
   * @param keyStorePassword the keystore password, which is not persisted
   * @param nodeRuntime the node runtime
   * @param keyStoreFilePath the keystore path, which is different for test vs production
   * @param configurationCertificateFilePath the output path for the configuration role's certificate, which is different for test vs
   * production
   */
  public NodesInitializer(
          final boolean isClassExistsTested,
          final char[] keyStorePassword,
          final BasicNodeRuntime nodeRuntime,
          final String keyStoreFilePath,
          final String configurationCertificateFilePath) {
    //Preconditions
    assert keyStorePassword != null : "keyStorePassword must not be null";
    assert keyStorePassword.length > 0 : "keyStorePassword must not be empty";
    assert nodeRuntime != null : "nodeRuntime must not be null";
    assert StringUtils.isNonEmptyString(keyStoreFilePath) : "keyStoreFilePath must be a non-empty string";
    assert StringUtils.isNonEmptyString(configurationCertificateFilePath) : "configurationCertificateFilePath must be a non-empty string";

    this.isClassExistsTested = isClassExistsTested;
    this.keyStorePassword = keyStorePassword.clone();
    this.nodeRuntime = nodeRuntime;
    this.keyStoreFilePath = keyStoreFilePath;
    this.configurationCertificateFilePath = configurationCertificateFilePath;
    containerName = nodeRuntime.getContainerName();
    nodeAccess = nodeRuntime.getNodeAccess();
  }

  /**
   * Reads the nodes XML file and records the node and role fields.
   *
   * @param nodesPath the nodes XML file path
   * @param nodesFileHashString the nodes file SHA-512 hash encoded as a base 64 string, used to detect tampering
   */
  public void process(
          final String nodesPath,
          final String nodesFileHashString) {
    //Preconditions
    assert StringUtils.isNonEmptyString(nodesPath) : "nodesPath must not be empty";
    assert StringUtils.isNonEmptyString(nodesFileHashString) : "nodesFileHashString must not be empty";
    if (!X509Utils.isJCEUnlimitedStrengthPolicy()) {
      throw new TexaiException("JCE Unlimited Strength Policy files are not installed");
    }

    MessageDigestUtils.verifyFileHash(
            nodesPath, // filePath
            nodesFileHashString); // fileHashString

    CacheInitializer.initializeCaches();
    DistributedRepositoryManager.clearNamedRepository("Nodes");
    final BufferedInputStream bufferedInputStream;
    try {
      final File nodesFile = new File(nodesPath);
      LOGGER.debug("parsing the nodes file: " + nodesFile.toString());
      bufferedInputStream = new BufferedInputStream(new FileInputStream(nodesFile));
    } catch (final FileNotFoundException ex) {
      throw new TexaiException(ex);
    }
    try {
      final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
      final SAXParser saxParser = saxParserFactory.newSAXParser();
      final SAXHandler myHandler = new SAXHandler();
      saxParser.parse(bufferedInputStream, myHandler);

    } catch (final ParserConfigurationException | SAXException | IOException ex) {
      LOGGER.fatal(StringUtils.getStackTraceAsString(ex));
      throw new TexaiException(ex);
    }

    assert nbrNodeTags == nodeFieldsHolderDictionary.size();
    displayNodes();
    populateNodeRolesFromPrototytpeNodes();
    removeAbstractAgents();
    displayNodes();
    verifyParentRoles();
    verifyChildRoles();
    generateX509CertificatesForRoles();
    emitConfigurationCertificate();
    persistNodes();
    loadNodesAndInjectDependencies();
    displayNetworkSingletonNodes();
    toGraphViz(
            "data", // graphPath
            "agents-graph"); // raphName

    try {
      final RepositoryConnection repositoryConnection
              = DistributedRepositoryManager.getInstance().getRepositoryConnectionForRepositoryName("Nodes");
      LOGGER.debug("repository size: " + repositoryConnection.size());
    } catch (final RepositoryException ex) {
      throw new TexaiException(ex);
    }
  }

  /**
   * Populates node roles from prototype nodes.
   */
  private void populateNodeRolesFromPrototytpeNodes() {
    LOGGER.debug("populating roles from prototypes ...");
    nodeFieldsHolderDictionary.values().stream().sorted().forEach((nodeFieldsHolder1) -> {
      LOGGER.debug("");
      LOGGER.debug("  targetNodeFieldsHolder:    " + nodeFieldsHolder1);
      nodeFieldsHolder1.prototypeNodeNames.stream().forEach(prototypeNodeName -> {
        final NodeFieldsHolder prototypeNodeFieldsHolder1 = nodeFieldsHolderDictionary.get(prototypeNodeName);
        if (prototypeNodeFieldsHolder1 == null) {
          throw new TexaiException("prototypeNodeName not found " + prototypeNodeName);
        }
        populateNodeRolesFromPrototypeNode(
                nodeFieldsHolder1,
                prototypeNodeFieldsHolder1);
      });
    });
  }

  /**
   * Recursively ascend the tree of prototype nodes, starting with the given prototype node holder, adding its roles to the given target
   * node holder.
   *
   * @param targetNodeFieldsHolder the given target node holder
   * @param prototypeNodeFieldsHolder
   */
  private void populateNodeRolesFromPrototypeNode(
          final NodeFieldsHolder targetNodeFieldsHolder,
          final NodeFieldsHolder prototypeNodeFieldsHolder) {
    //Preconditions
    assert targetNodeFieldsHolder != null : "targetNodeFieldsHolder must not be null";
    assert prototypeNodeFieldsHolder != null : "prototypeNodeFieldsHolder must not be null";

    LOGGER.debug("");
    LOGGER.debug("    prototypeNodeFieldsHolder: " + prototypeNodeFieldsHolder);
    prototypeNodeFieldsHolder.roleFieldsHolders.stream().forEach(new Consumer<RoleFieldsHolder>() {

      @Override
      public void accept(final RoleFieldsHolder roleFieldsHolder1) {
        final RoleFieldsHolder clonedRoleFieldsHolder = new RoleFieldsHolder();
        LOGGER.debug("");
        LOGGER.debug("    role:        " + roleFieldsHolder1.qualifiedName);
        final String[] nameParts = roleFieldsHolder1.qualifiedName.split("\\.");
        if (nameParts.length != 3) {
          throw new TexaiException("malformed qualified role name: " + roleFieldsHolder1.qualifiedName);
        }
        // rename the role qualified name to prefix it with the name of the target node
        clonedRoleFieldsHolder.qualifiedName = targetNodeFieldsHolder.name + "." + nameParts[2];
        LOGGER.debug("    cloned role: " + clonedRoleFieldsHolder.qualifiedName);
        clonedRoleFieldsHolder.description = roleFieldsHolder1.description;
        clonedRoleFieldsHolder.parentQualifiedName = roleFieldsHolder1.parentQualifiedName;
        clonedRoleFieldsHolder.skillClasses.addAll(roleFieldsHolder1.skillClasses);
        clonedRoleFieldsHolder.variableNames.addAll(roleFieldsHolder1.variableNames);
        targetNodeFieldsHolder.roleFieldsHolders.add(clonedRoleFieldsHolder);
        roleFieldsHolderDictionary.put(clonedRoleFieldsHolder.qualifiedName, clonedRoleFieldsHolder);
      }
    });

    prototypeNodeFieldsHolder.prototypeNodeNames.stream().forEach(prototypeNodeName -> {
      final NodeFieldsHolder prototypeNodeFieldsHolder1 = nodeFieldsHolderDictionary.get(prototypeNodeName);
      if (prototypeNodeFieldsHolder1 == null) {
        throw new TexaiException("prototypeNodeName not found " + prototypeNodeName);
      }
      populateNodeRolesFromPrototypeNode(
              targetNodeFieldsHolder,
              prototypeNodeFieldsHolder1);
      if (targetNodeFieldsHolder.roleFieldsHolders.isEmpty()) {
        throw new TexaiException("node " + targetNodeFieldsHolder.name + " has no roles");
      }
    });
  }

  private void removeAbstractAgents() {
    final Set<String> nodeNamesToRemove = new HashSet<>();
    nodeFieldsHolderDictionary.entrySet().stream().forEach(entry -> {
      if (entry.getValue().isAbstract) {
        nodeNamesToRemove.add(entry.getKey());
      }
    });
    nodeNamesToRemove.stream().sorted().forEach(key -> {
      final NodeFieldsHolder nodeFieldsHolder1 = nodeFieldsHolderDictionary.get(key);
      LOGGER.debug("removing abstract agent: " + nodeFieldsHolder1);
      nodeFieldsHolderDictionary.remove(key);
      LOGGER.debug("removing abstract agent's roles...");
      final Set<String> roleNamesToRemove = new HashSet<>();
      nodeFieldsHolder1.roleFieldsHolders.stream().forEach(roleFieldsHolder1 -> {
        roleNamesToRemove.add(roleFieldsHolder1.qualifiedName);
      });
      roleNamesToRemove.stream().forEach(roleName -> {
        LOGGER.debug("  removed " + roleFieldsHolderDictionary.get(roleName));
        roleFieldsHolderDictionary.remove(roleName);
      });
    });
  }

  /**
   * Verifies that the specified parent role exists for each declaring role.
   */
  private void verifyParentRoles() {
    LOGGER.debug("verifying parent roles ...");
    roleFieldsHolderDictionary.values().stream().sorted().forEach(roleFieldsHolder1 -> {
      final String parentQualifiedName = roleFieldsHolder1.parentQualifiedName;
      if (StringUtils.isNonEmptyString(parentQualifiedName)) {
        final RoleFieldsHolder parentRoleFieldsHolder = roleFieldsHolderDictionary.get(parentQualifiedName);
        if (parentRoleFieldsHolder == null) {
          throw new TexaiException("cannot find parent role: " + parentQualifiedName + "\n for role "
                  + roleFieldsHolder1.qualifiedName);
        }
        LOGGER.debug("  " + roleFieldsHolder1.qualifiedName + " has parent " + parentQualifiedName);
        parentRoleFieldsHolder.childQualifiedNames.add(roleFieldsHolder1.qualifiedName);
      } else {
        LOGGER.debug("  no parent role for " + roleFieldsHolder1.qualifiedName);
      }
    });
  }

  /**
   * Verifies that the specified child role exists for each declaring parent role.
   */
  private void verifyChildRoles() {
    LOGGER.debug("verifying child roles ...");
    roleFieldsHolderDictionary.values().stream().sorted().forEach(roleFieldsHolder1 -> {
      roleFieldsHolder1.childQualifiedNames.stream().forEach(childQualifiedName -> {
        final RoleFieldsHolder childRoleFieldsHolder = roleFieldsHolderDictionary.get(childQualifiedName);
        if (childRoleFieldsHolder == null) {
          throw new TexaiException(" child role not found, parent: " + roleFieldsHolder1.qualifiedName
                  + ", child: " + childQualifiedName);
        }
        if (!childRoleFieldsHolder.parentQualifiedName.equals(roleFieldsHolder1.qualifiedName)) {
          throw new TexaiException(" child role mismatch, parent: " + roleFieldsHolder1.qualifiedName
                  + ", child: " + childQualifiedName);
        }
        LOGGER.debug("  " + roleFieldsHolder1.qualifiedName + " has child " + childQualifiedName);

      });
    });
  }

  /**
   * Displays the nodes and their roles.
   */
  private void displayNodes() {
    LOGGER.debug("");
    LOGGER.debug("nodes and their roles ...");
    nodeFieldsHolderDictionary.values().stream().sorted().forEach(nodeFieldsHolder1 -> {
      LOGGER.debug("");
      LOGGER.debug("  " + nodeFieldsHolder1);
      assert !nodeFieldsHolder1.roleFieldsHolders.isEmpty();
      nodeFieldsHolder1.roleFieldsHolders.stream().sorted().forEach(roleFieldsHolder1 -> {
        LOGGER.debug("    " + roleFieldsHolder1);
      });
    });
  }

  /**
   * Generates a self-signed X509 certificate for each role.
   */
  private void generateX509CertificatesForRoles() {

    //TODO create a write-only keystore
    try {
      LOGGER.debug("getting the keystore " + keyStoreFilePath);
      keyStore = X509Utils.findOrCreateUberKeyStore(keyStoreFilePath, keyStorePassword);
      try (final FileOutputStream keyStoreOutputStream = new FileOutputStream(new File(keyStoreFilePath))) {
        keyStore.store(keyStoreOutputStream, keyStorePassword);
      }
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | NoSuchProviderException ex) {
      LOGGER.error(StringUtils.getStackTraceAsString(ex));
      throw new TexaiException(ex);
    }

    LOGGER.debug("getting self-signed X.509 certificates for roles ...");
    roleFieldsHolderDictionary.values().stream().sorted().forEach(roleFieldsHolder1 -> {
      LOGGER.debug("  " + roleFieldsHolder1.qualifiedName);
      if (roleFieldsHolder1.areRemoteCommunicationsPermitted) {

        boolean isFound = false;
        if (X509Utils.keyStoreContains(keyStoreFilePath,
                keyStorePassword,
                roleFieldsHolder1.qualifiedName)) {  // alias
          LOGGER.debug("    getting the existing certificate");
          roleFieldsHolder1.x509SecurityInfo
                  = X509Utils.getX509SecurityInfo(
                          keyStore,
                          keyStorePassword,
                          roleFieldsHolder1.qualifiedName); // alias
          if (roleFieldsHolder1.x509SecurityInfo != null) {
            isFound = true;
          }
        }

        if (!isFound) {
          LOGGER.debug("    generating a new certificate");
          final KeyPair keyPair;
          try {
            keyPair = X509Utils.generateRSAKeyPair3072();
          } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException ex) {
            throw new TexaiException(ex);
          }
          roleFieldsHolder1.x509SecurityInfo = X509Utils.generateX509SecurityInfo(
                  keyStore,
                  keyStorePassword,
                  keyPair,
                  null, // uid
                  roleFieldsHolder1.qualifiedName, // domainComponent
                  roleFieldsHolder1.qualifiedName); // certificateAlias
        }
      } else if (X509Utils.keyStoreContains(keyStoreFilePath,
              keyStorePassword,
              roleFieldsHolder1.qualifiedName)) {  // alias
        try {
          LOGGER.debug("    deleting unwanted certificate from the keystore: " + roleFieldsHolder1.qualifiedName);
          keyStore.deleteEntry(roleFieldsHolder1.qualifiedName); // alias
        } catch (KeyStoreException ex) {
          throw new TexaiException(ex);
        }
      }
    });
    // persist the keystore and the updated certificates
    try (final FileOutputStream fileOutputStream = new FileOutputStream(keyStoreFilePath)) {
      keyStore.store(fileOutputStream, keyStorePassword);
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException ex) {
      throw new TexaiException(ex);
    }

    // Persist a copy of the keystore in outside of the installation directory tree
    final File saveKeyStoreFile = new File("../../keystore.uber");
    if (!saveKeyStoreFile.exists()) {
      try {
        FileUtils.copyFile(new File(keyStoreFilePath), saveKeyStoreFile);
      } catch (IOException ex) {
        throw new TexaiException(ex);
      }
    }

    nodeRuntime.setKeyStore(keyStore);
    nodeRuntime.setKeyStorePassword(keyStorePassword);
    LOGGER.debug("");
    LOGGER.debug("confirming that roles' certificates can be retrieved from storage ...");
    roleFieldsHolderDictionary.values().stream().sorted().forEach(roleFieldsHolder1 -> {
      LOGGER.debug("  " + roleFieldsHolder1.qualifiedName);
      if (roleFieldsHolder1.areRemoteCommunicationsPermitted) {
        if (X509Utils.keyStoreContains(keyStoreFilePath,
                keyStorePassword,
                roleFieldsHolder1.qualifiedName)) {  // alias
          LOGGER.debug("    certificate OK");
        } else {
          LOGGER.debug("    certificate not stored for " + roleFieldsHolder1.qualifiedName);
        }
        final X509SecurityInfo x509SecurityInfo = X509Utils.getX509SecurityInfo(
                nodeRuntime.getKeyStore(),
                nodeRuntime.getKeyStorePassword(),
                roleFieldsHolder1.qualifiedName); // alias
        LOGGER.debug("    X.509 subject: " + x509SecurityInfo.getX509Certificate().getSubjectDN());
      }
    });
  }

  /**
   * Populates node roles from prototype nodes.
   */
  private void emitConfigurationCertificate() {
    final String singletonConfigurationRoleName = "ContainerSingletonConfigurationRole";
    roleFieldsHolderDictionary.values().stream().forEach(roleFieldsHolder1 -> {
      // save the configuration role's certificate
      if (Role.extractRoleName(roleFieldsHolder1.qualifiedName).equals(singletonConfigurationRoleName)) {
        singletonConfigurationCertificate = roleFieldsHolder1.x509SecurityInfo.getCertificateChain()[0];
      }
    });
    if (singletonConfigurationCertificate == null) {
      throw new TexaiException("missing " + singletonConfigurationRoleName);
    }
    LOGGER.debug("emitting the configuration role's certificate to " + configurationCertificateFilePath);
    try {
      X509Utils.writeX509Certificate(singletonConfigurationCertificate,
              configurationCertificateFilePath); // filePath
    } catch (CertificateEncodingException | IOException ex) {
      throw new TexaiException(ex);
    }
  }

  /**
   * Persists the nodes and their roles.
   */
  private void persistNodes() {
    LOGGER.debug("");
    LOGGER.debug("persisting nodes and their roles ...");
    final RepositoryConnection repositoryConnection = nodeAccess.getRDFEntityManager().getConnectionToNamedRepository("Nodes");
    try {
      repositoryConnection.begin();
      nodeFieldsHolderDictionary.values().stream().sorted().forEach(nodeFieldsHolder1 -> {
        LOGGER.debug("");
        LOGGER.debug("  " + nodeFieldsHolder1);
        if (nodeFieldsHolder1.isAbstract) {
          LOGGER.debug("      abstract and not persisted");
        } else {
          final Set<Role> roles = new ArraySet<>();
          nodeFieldsHolder1.roleFieldsHolders.stream().sorted().forEach(roleFieldsHolder1 -> {
            LOGGER.debug("    " + roleFieldsHolder1);
            final Role role = new Role(
                    roleFieldsHolder1.qualifiedName,
                    roleFieldsHolder1.description,
                    roleFieldsHolder1.parentQualifiedName,
                    roleFieldsHolder1.childQualifiedNames,
                    roleFieldsHolder1.skillClasses,
                    roleFieldsHolder1.variableNames,
                    roleFieldsHolder1.areRemoteCommunicationsPermitted);
            nodeAccess.persistRole(role);
            roles.add(role);
            role.getChildQualifiedNames().stream().forEach(childQualifiedName -> {
              LOGGER.debug("      child: " + childQualifiedName);
            });
          });
          assert !roles.isEmpty();
          final Node node = new Node(
                  nodeFieldsHolder1.name,
                  nodeFieldsHolder1.missionDescription,
                  roles,
                  nodeFieldsHolder1.isNetworkSingleton);
          nodeAccess.persistNode(node);
          node.getRoles().stream().forEach(role -> {
            role.setNode(node);
            assert role.getNode() == node;
            nodeAccess.persistRole(role);
          });
        }
      });
      repositoryConnection.commit();
    } catch (RepositoryException ex) {
      throw new TexaiException(ex);
    }
  }

  /**
   * Loads the persisted nodes and injects the runtime dependencies.
   */
  private void loadNodesAndInjectDependencies() {
    LOGGER.debug("");
    LOGGER.debug("loading the persisted nodes and injecting dependencies ...");
    nodeAccess.getNodes().stream().sorted().forEach((Node node) -> {
      LOGGER.debug("");
      LOGGER.debug("  " + node);
      node.setNodeRuntime(nodeRuntime);
      assert node.getNodeRuntime() != null;
      assert !node.getRoles().isEmpty();
      for (final Role role : node.getRoles()) {
        LOGGER.debug("    " + role);
        role.instantiate();

        assert node.equals(role.getNode());
        assert node.getId().equals(role.getNode().getId());
        assert node.getNodeRuntime() != null;
        assert role.getNode().getNodeRuntime() != null;

        nodeRuntime.registerRole(role);
        X509SecurityInfo x509SecurityInfo = null;
        if (role.areRemoteCommunicationsPermitted()) {
          x509SecurityInfo = X509Utils.getX509SecurityInfo(
                  keyStore,
                  keyStorePassword, // keyStorePassword
                  role.getQualifiedName()); // alias
          assert x509SecurityInfo != null;
        }
        role.initialize(nodeRuntime, x509SecurityInfo);
      }
    });
  }

  /**
   * Displays the network singleton nodes.
   */
  private void displayNetworkSingletonNodes() {
    LOGGER.debug("");
    LOGGER.debug("the network singleton nodes (nomadic agents) and their  child roles  ...");
    nodeAccess.getNodes().stream().filter(Node.isNetworkSingletonNode()).sorted().forEach(node -> {
      LOGGER.debug("");
      LOGGER.debug("  " + node);
      node.getRoles().stream().sorted().forEach((Role role) -> {
        role.getChildQualifiedNames().stream().sorted().forEach((String childQualifiedName) -> {
          final Role childRole = nodeRuntime.getLocalRole(childQualifiedName);
          assert childRole != null;
          LOGGER.debug("    " + childQualifiedName);
        });
      });
    });

    LOGGER.debug("");
    LOGGER.debug("the network singleton nodes (nomadic agents) and their filtered non-singleton child roles  ...");
    nodeAccess.getNodes().stream().filter(Node.isNetworkSingletonNode()).sorted().forEach(node -> {
      LOGGER.debug("");
      LOGGER.debug("  " + node);
      node.getRoles().stream().sorted().forEach((Role role) -> {
        role.getChildQualifiedNames().stream().sorted().forEach((String childQualifiedName) -> {
          final Role childRole = nodeRuntime.getLocalRole(childQualifiedName);
          assert childRole != null;
          if (!childRole.getNode().isNetworkSingleton()) {
            LOGGER.debug("    " + childQualifiedName);
          }
        });
      });
    });
  }

  /**
   * Generates a GraphViz input file that depicts an agent graph.
   *
   * <code>
   * $ dot -Tpng agents-graph.dot -o agents-graph.png
   * </code>
   *
   * @param graphPath the graph directory
   * @param graphName the output graph name
   */
  public void toGraphViz(
          final String graphPath,
          final String graphName) {
    //Preconditions
    assert graphPath != null : "graphPath must not be null";
    assert !graphPath.isEmpty() : "graphPath must not be empty";
    assert graphName != null : "graphName must not be null";
    assert !graphName.isEmpty() : "graphName must not be empty";
    assert !graphName.contains(" ") : "graphName must not contain whitespace";

    final String graphDataPath = graphPath + "/" + graphName + ".dot";
    final String keyDataPath = graphPath + "/" + graphName + "-key.txt";
    BufferedWriter graphBufferedWriter = null;
    BufferedWriter keyBufferedWriter = null;
    LOGGER.debug("");
    LOGGER.debug("graphDataPath: " + graphDataPath);
    try {
      graphBufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(graphDataPath), "UTF-8"));
      keyBufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(keyDataPath), "UTF-8"));
      graphBufferedWriter.append("digraph \"");
      graphBufferedWriter.append(graphName);
      graphBufferedWriter.append("\" {\n");
      graphBufferedWriter.append("  ratio = \"auto\" ;\n");
      graphBufferedWriter.append("  mincross = 2.0 ;\n");

      graphNodes(
              graphBufferedWriter,
              keyBufferedWriter);

      graphBufferedWriter.append("}");
      graphBufferedWriter.close();
      keyBufferedWriter.close();
    } catch (final IOException ex) {
      try {
        if (graphBufferedWriter != null) {
          graphBufferedWriter.close();
        }

        if (keyBufferedWriter != null) {
          keyBufferedWriter.close();
        }
        throw new TexaiException(ex);
      } catch (final IOException ex1) {
        throw new TexaiException(ex1);    // NOPMD
      }
    }
  }

  /**
   * Graphs the agents by major role parent-child relationships.
   *
   * @param graphBufferedWriter the graph buffered writer
   * @param keyBufferedWriter the graph key buffered writer
   */
  private void graphNodes(
          final BufferedWriter graphBufferedWriter,
          final BufferedWriter keyBufferedWriter) {
    //Preconditions
    assert graphBufferedWriter != null : "graphBufferedWriter must not be null";
    assert keyBufferedWriter != null : "keyBufferedWriter must not be null";

    try {
      graphBufferedWriter.append("subgraph cluster_agents");
      graphBufferedWriter.append(" {\n");
      graphBufferedWriter.append("  label = \"nodes\"\n");

      // emit a graph node for each agent node.
      nodeAccess.getNodes().stream().sorted().forEach(node -> {
        final String nodeLabel = node.extractAgentName();
        final String shape;
        if (node.isNetworkSingleton()) {
          shape = "    shape = oval";
        } else {
          shape = "    shape = box";
        }

        try {
          // describe the node
          keyBufferedWriter.append(nodeLabel);
          keyBufferedWriter.append("\n  ");
          keyBufferedWriter.append(node.getMissionDescription());
          keyBufferedWriter.append("\n\n");

          // graph the node
          graphBufferedWriter.append("  ");
          graphBufferedWriter.append("N");
          graphBufferedWriter.append(nodeLabel);
          graphBufferedWriter.append(" [\n");
          graphBufferedWriter.append(shape);
          graphBufferedWriter.append("\n    label = \"");
          graphBufferedWriter.append(nodeLabel);
          graphBufferedWriter.append("\" ];\n");
        } catch (IOException ex) {
          throw new TexaiException(ex);
        }
      });
      graphBufferedWriter.append("  }");

      // emit a graph edge between agents having connecting parent and child roles
      // the dictionary of graphed parent-child edges: parent node --> child node
      final Set<String> parentChildEdges = new HashSet<>();
      nodeAccess.getNodes().stream().sorted().forEach(node -> {
        node.getRoles().stream().forEach(role -> {
          role.getChildQualifiedNames().stream().forEach(childQualifiedName -> {
            final String childAgentName = Node.extractAgentName(childQualifiedName);
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("  ");
            stringBuilder.append("N");
            stringBuilder.append(node.extractAgentName());
            stringBuilder.append(" -> ");
            stringBuilder.append("N");
            stringBuilder.append(childAgentName);
            stringBuilder.append(";\n");
            parentChildEdges.add(stringBuilder.toString());
          });
        });
      });
      parentChildEdges.stream().forEach(string -> {
        // link the node to its children
        try {
          graphBufferedWriter.append(string);
        } catch (IOException ex) {
          throw new TexaiException(ex);
        }
      });
      graphBufferedWriter.append("\n");
    } catch (IOException ex) {
      throw new TexaiException(ex);
    }
  }

  /**
   * Contains the fields required to construct a node instance.
   */
static  class NodeFieldsHolder implements Comparable<NodeFieldsHolder> {

    // the node qualifiedName which must end in "Agent"
    String name;
    // the node's mission described in English
    String missionDescription;
    // the indicator whether this is an abstract node, which is a non-instantiated prototype
    boolean isAbstract = false;
    // the prototype node names, if any, from which roles and uninitialized state variables are copied
    final Set<String> prototypeNodeNames = new ArraySet<>();
    // the role field holders
    final Set<RoleFieldsHolder> roleFieldsHolders = new ArraySet<>();
    // the indicator whether this node is a singleton nomadic agent, in which case only one container in the network hosts
    // the active node and all other nodes having the same name are inactive
    boolean isNetworkSingleton;

    /** Constructs a new NodeFieldsHolder instance. */
    NodeFieldsHolder() {
      name = null;
      missionDescription = null;
      isNetworkSingleton = false;
    }

    /**
     * Compares the given node field holder with this one. Collates by node qualifiedName.
     *
     * @param other the given node field holder
     *
     * @return -1 if less, 0 if equal, otherwise return 1
     */
    @Override
    public int compareTo(final NodeFieldsHolder other) {
      return this.name.compareTo(other.name);
    }

    /** Returns a hash code for this object.
     *
     * @return a hash code for this object
     */
    @Override
    public int hashCode() {
      int hash = 5;
      hash = 67 * hash + Objects.hashCode(this.name);
      return hash;
    }

    /** Returns whether some other object equals this one.
     *
     * @param obj the other object
     * @return whether some other object equals this one
     */
    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final NodeFieldsHolder other = (NodeFieldsHolder) obj;
      return Objects.equals(this.name, other.name);
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this objec
     */
    @Override
    public String toString() {
      final StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append('[');
      stringBuilder.append(name);
      stringBuilder.append(']');
      return stringBuilder.toString();
    }
  }

  /**
   * Finalizes this application.
   */
  public void finalization() {
    // erase the password
    for (int i = 0; i < keyStorePassword.length; i++) {
      keyStorePassword[i] = 0;
    }
    LOGGER.debug("NodesInitializer completed");
  }

  /**
   * Contains the fields required to construct a role instance.
   */
static  class RoleFieldsHolder implements Comparable<RoleFieldsHolder> {

    // the qualified role name, i.e. container.nodename.rolename
    String qualifiedName;
    // the role's description in English
    String description;
    // the parent qualified role name, i.e. container.nodename.rolename, which is null if this is a top level role
    String parentQualifiedName;
    // the qualified child role names, i.e. container.nodename.rolename, which are empty if this is a lowest level role.
    final Set<String> childQualifiedNames = new ArraySet<>();
    // the skill class names, which are objects that contain, verify and format the class names
    final Set<SkillClass> skillClasses = new ArraySet<>();
    // the state variable names
    final Set<String> variableNames = new ArraySet<>();
    // the indicator whether this role is permitted to send a message to a recipient in another container, which requires
    // an X.509 certificate
    boolean areRemoteCommunicationsPermitted = false;
    // the X.509 security information for this role, which includes the self-signed certificate identifying this role
    // during remote communication
    X509SecurityInfo x509SecurityInfo;

    /** Constructs a new RoleFieldsHolder instance. */
    RoleFieldsHolder() {
      qualifiedName = null;
      description = null;
      parentQualifiedName = null;
      x509SecurityInfo = null;
    }

    /**
     * Compares the given role field holder with this one. Collates by qualified role name.
     *
     * @param other the given role field holder
     *
     * @return -1 if less, 0 if equal, otherwise return 1
     */
    @Override
    public int compareTo(RoleFieldsHolder other) {
      //Preconditions
      assert other != null : "other must not be null";
      assert StringUtils.isNonEmptyString(other.qualifiedName) : "other.qualifiedName must be a non empty string";
      assert StringUtils.isNonEmptyString(this.qualifiedName) : "qualifiedName must be a non empty string";

      return this.qualifiedName.compareTo(other.qualifiedName);
    }

    /** Returns a hash code for this object.
     *
     * @return a hash code for this object
     */
    @Override
    public int hashCode() {
      int hash = 7;
      hash = 79 * hash + Objects.hashCode(this.qualifiedName);
      return hash;
    }

    /** Returns whether some other object equals this one.
     *
     * @param obj the other object
     * @return whether some other object equals this one
     */
    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final RoleFieldsHolder other = (RoleFieldsHolder) obj;
      return Objects.equals(this.qualifiedName, other.qualifiedName);
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this objec
     */
    @Override
    public String toString() {
      final StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append('[');
      stringBuilder.append(qualifiedName);
      stringBuilder.append(']');
      return stringBuilder.toString();
    }
  }

  /**
   * Provides a SAX parsing handler.
   */
  final class SAXHandler extends DefaultHandler {

    /**
     * the string builder
     */
    private final StringBuilder stringBuilder = new StringBuilder();

    /**
     * Constructs a new SAXHandler instance.
     */
    public SAXHandler() {
    }

    /**
     * Receives notification of the start of an element.
     *
     * @param uri the element tag
     * @param localName the local qualifiedName
     * @param qName the qualified qualifiedName
     * @param attributes the attributes
     */
    @Override
    public void startElement(
            final String uri,
            final String localName,
            final String qName,
            final Attributes attributes) {
      //Preconditions
      assert StringUtils.isNonEmptyString(qName) : "qName must be a non-empty string";

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("startElement qName: " + qName);
      }
      stringBuilder.setLength(0);
      switch (qName) {
        case "node":
          nodeFieldsHolder = new NodeFieldsHolder();
          break;
        case "role":
          roleFieldsHolder = new RoleFieldsHolder();
          break;
        default:
      }
    }

    /**
     * Receive notification of character data inside an element.
     *
     * @param characters the characters
     * @param start the start position in the character array
     * @param length the length of the character string
     */
    @Override
    public void characters(final char[] characters,
            final int start,
            final int length) {

      //LOGGER.debug("characters, start: " + start + ", length: " + length);
      final int end = start + length;
      for (int i = start; i < end; i++) {
        stringBuilder.append(characters[i]);
      }
    }

    /**
     * Receives notification of the end of an element.
     *
     * @param uri the element tag
     * @param localName the local qualifiedName
     * @param qName the qualified qualifiedName
     */
    @Override
    public void endElement(
            final String uri,
            final String localName,
            final String qName) {
      //Preconditions
      assert StringUtils.isNonEmptyString(qName) : "qName must be a non-empty string";
      assert nodeAccess != null : "nodeAccess must not be null";

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("endElement qName: " + qName);
        LOGGER.debug("stringBuilder: " + stringBuilder.toString());
      }

      switch (qName) {
        case "name":
          nodeFieldsHolder.name = containerName + "." + stringBuilder.toString();
          break;
        case "qualifiedName":
          roleFieldsHolder.qualifiedName = containerName + "." + stringBuilder.toString();
          break;
        case "missionDescription":
          nodeFieldsHolder.missionDescription = stringBuilder.toString();
          break;
        case "isAbstract":
          nodeFieldsHolder.isAbstract = Boolean.parseBoolean(stringBuilder.toString());
          break;
        case "isNetworkSingleton":
          nodeFieldsHolder.isNetworkSingleton = Boolean.parseBoolean(stringBuilder.toString());
          break;
        case "areRemoteCommunicationsPermitted":
          roleFieldsHolder.areRemoteCommunicationsPermitted = Boolean.parseBoolean(stringBuilder.toString());
          break;
        case "prototypeNodeName":
          nodeFieldsHolder.prototypeNodeNames.add(containerName + "." + stringBuilder.toString());
          break;
        case "description":
          roleFieldsHolder.description = stringBuilder.toString();
          break;
        case "parentQualifiedName":
          if (stringBuilder.length() > 0) {
            roleFieldsHolder.parentQualifiedName = containerName + "." + stringBuilder.toString();
          }
          break;
        case "skill-class-name":
          final String skillClassName = stringBuilder.toString();
          if (!SkillClass.isValidSkillClassName(skillClassName, isClassExistsTested)) {
            throw new TexaiException("invalid skill class name: " + skillClassName);
          }
          roleFieldsHolder.skillClasses.add(new SkillClass(skillClassName, isClassExistsTested));
          break;
        case "variableName":
          roleFieldsHolder.variableNames.add(stringBuilder.toString());
          break;
        case "role":
          roleFieldsHolderDictionary.put(roleFieldsHolder.qualifiedName, roleFieldsHolder);
          nodeFieldsHolder.roleFieldsHolders.add(roleFieldsHolder);
          break;
        case "node":
          nodeFieldsHolderDictionary.put(nodeFieldsHolder.name, nodeFieldsHolder);
          nbrNodeTags++;
          break;
        default:
      }
    }
  }
}
