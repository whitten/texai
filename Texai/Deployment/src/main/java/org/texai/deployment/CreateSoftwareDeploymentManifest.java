package org.texai.deployment;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.texai.util.FileSystemUtils;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import org.texai.x509.X509Utils;

/**
 * CreateSoftwareDeploymentManifest.java
 *
 * Description: Compares the new A.I. Coin peer directory with the production peer directory and prepares a manifest of changed files.
 *
 * Copyright (C) Jan 11, 2015, Stephen L. Reed.
 */
public class CreateSoftwareDeploymentManifest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(CreateSoftwareDeploymentManifest.class);

  // the location of a copy of the production directory
  private final String oldDirectoryPath;

  // the location of a copy of the proposed production directory
  private final String newDirectoryPath;

  // the location of the directory into which the manifest is output
  private final String manifestDirectoryPath;

  // the directory into which the manifest is output.
  final File manifestDirectory;

  // the ignored directories
  private final List<String> ignoredDirectoryPaths = new ArrayList<>();

  // the ignored files
  private final List<String> ignoredFilePaths = new ArrayList<>();

  /**
   * Creates a new instance of CreateSoftwareDeploymentManifest.
   *
   * @param oldDirectoryPath the location of a copy of the production directory
   * @param newDirectoryPath the location of a copy of the proposed production directory
   * @param manifestDirectoryPath the location of the parent directory into which the manifest directory is output, files-2015-01-11
   */
  public CreateSoftwareDeploymentManifest(
          final String oldDirectoryPath,
          final String newDirectoryPath,
          final String manifestDirectoryPath) {
    //Preconditions
    assert StringUtils.isNonEmptyString(oldDirectoryPath) : "oldDirectoryPath must be a non-empty string";
    assert StringUtils.isNonEmptyString(newDirectoryPath) : "newDirectoryPath must be a non-empty string";
    assert StringUtils.isNonEmptyString(manifestDirectoryPath) : "manifestDirectoryPath must be a non-empty string";

    this.oldDirectoryPath = oldDirectoryPath;
    this.newDirectoryPath = newDirectoryPath;
    this.manifestDirectoryPath = manifestDirectoryPath;

    manifestDirectory = new File(manifestDirectoryPath);
    if (!manifestDirectory.exists()) {
      throw new TexaiException(manifestDirectory + " must exist");
    }
    if (!manifestDirectory.isDirectory()) {
      throw new TexaiException(manifestDirectory + " must be a directory");
    }

    ignoredDirectoryPaths.add(".aicoin");
    ignoredDirectoryPaths.add("journals");
    ignoredDirectoryPaths.add("repositories");

    ignoredFilePaths.add("agents-graph.dot");
    ignoredFilePaths.add("agents-graph-key.txt");
    ignoredFilePaths.add("certificate-serial-nbr.txt");
    ignoredFilePaths.add("console.log");
    ignoredFilePaths.add("keystore.uber");
    ignoredFilePaths.add("secure-random.ser");
    ignoredFilePaths.add("wallet.dat"); // redundant because .aicoin is ignored
    ignoredFilePaths.add("x11vnc.log");
  }

  /**
   * Initializes this application.
   */
  private void initialization() {
    LOGGER.info("Comparing old and new software/data directories to create a deployment manifest ...");
    LOGGER.info("  old directory path      " + oldDirectoryPath);
    LOGGER.info("  new directory path      " + newDirectoryPath);
    LOGGER.info("  manifest directory path " + manifestDirectory);
  }

  /**
   * Compares the new A.I. Coin peer directory with the production peer directory and prepares a manifest of changed files.
   */
  private void process() {
    final File oldDirectoryFile = new File(oldDirectoryPath);
    if (!oldDirectoryFile.exists()) {
      throw new TexaiException(oldDirectoryFile + " does not exist");
    }
    if (!oldDirectoryFile.isDirectory()) {
      throw new TexaiException(oldDirectoryFile + " is not a directory");
    }
    final File newDirectoryFile = new File(newDirectoryPath);
    if (!newDirectoryFile.exists()) {
      throw new TexaiException(newDirectoryFile + " does not exist");
    }
    if (!newDirectoryFile.isDirectory()) {
      throw new TexaiException(newDirectoryFile + " is not a directory");
    }
    final File manifestDirectoryFile = new File(manifestDirectoryPath);
    if (!manifestDirectoryFile.exists()) {
      throw new TexaiException(manifestDirectoryFile + " does not exist");
    }
    if (!manifestDirectoryFile.isDirectory()) {
      throw new TexaiException(manifestDirectoryFile + " is not a directory");
    }

    // recursively visit and compare the old and new directory contents
    final StringBuilder manifestStringBuilder = new StringBuilder();

    compareDirectories(oldDirectoryFile, newDirectoryFile, manifestStringBuilder);
    final String manifestString = manifestStringBuilder.toString();
    LOGGER.info("manifest ...\n" + manifestString);

    final String manifestFilePath = manifestDirectory.toString() + '/' + FileSystemUtils.formDatedFileName("manifest");
    LOGGER.info("Writing manifest " + manifestFilePath);
    try {
      try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(manifestFilePath))) {
        bufferedOutputStream.write(manifestString.getBytes());
      }
    } catch (Exception ex) {
      throw new TexaiException(ex);
    }
  }

  /**
   * Recursively visits and compares the old and new directory contents, creating an upgrade manifest.
   *
   * @param oldDirectoryFile the old directory
   * @param newDirectoryFile the corresponding new directory
   * @param manifestStringBuilder the upgrade manifest text
   */
  private void compareDirectories(
          final File oldDirectoryFile,
          final File newDirectoryFile,
          final StringBuilder manifestStringBuilder) {
    //Preconditions
    assert manifestStringBuilder != null : "manifestStringBuilder must not be null";

    final List<File> oldFiles = new ArrayList<>();
    for (final File file : oldDirectoryFile.listFiles()) {
      oldFiles.add(file);
    }
    Collections.sort(oldFiles);
    final Iterator<File> oldFiles_iter = oldFiles.iterator();

    final List<File> newFiles = new ArrayList<>();
    for (final File file : newDirectoryFile.listFiles()) {
      newFiles.add(file);
    }
    Collections.sort(newFiles);
    final Iterator<File> newFiles_iter = newFiles.iterator();

    File oldFile = advanceIterator(oldFiles_iter);
    File newFile = advanceIterator(newFiles_iter);

    while (true) {
      LOGGER.debug("");
      if (oldFile == null && newFile == null) {
        break;
      }
      if (oldFile == null) {
        LOGGER.debug("files in the old directory exhausted");
      } else if (isIgnored(oldFile)) {
        LOGGER.info("ignoring " + oldFile);
        oldFile = advanceIterator(oldFiles_iter);
        continue;
      } else {
        LOGGER.debug("comparing old file " + oldFile);
      }
      if (newFile == null) {
        LOGGER.debug("files in the new directory exhausted");
      } else if (isIgnored(newFile)) {
        LOGGER.info("ignoring " + newFile);
        newFile = advanceIterator(newFiles_iter);
        continue;
      } else {
        LOGGER.debug("comparing new file " + newFile);
      }

      if (oldFile == null) {
        LOGGER.debug("old files exhausted, adding new file " + newFile);
        final String encodedFileHash = X509Utils.fileHashString(newFile);
        manifestStringBuilder.append(new StringBuilder().append("add     \"").append(newFile).append("\"\n").toString());
        addFileToManifest(newFile);
        newFile = advanceIterator(newFiles_iter);

      } else if (newFile == null) {
        LOGGER.debug("new files exhausted, removing old file " + oldFile);
        manifestStringBuilder.append(new StringBuilder().append("remove     \"").append(oldFile).append("\"\n").toString());
        oldFile = advanceIterator(oldFiles_iter);

      } else if (compareRelativeFilePaths(oldFile, newFile) < 0) {
        if (oldFile.isDirectory()) {
          LOGGER.debug("removing old directory " + oldFile);
          manifestStringBuilder.append(new StringBuilder().append("remove-dir \"").append(oldFile).append("\"\n").toString());
        } else {
          LOGGER.debug("removing old file " + oldFile);
          manifestStringBuilder.append(new StringBuilder().append("remove     \"").append(oldFile).append("\"\n").toString());
        }
        oldFile = advanceIterator(oldFiles_iter);

      } else if (compareRelativeFilePaths(oldFile, newFile) > 0) {
        if (newFile.isDirectory()) {
          addDirectory(newFile, manifestStringBuilder);
        } else {
          LOGGER.debug("adding new file " + newFile);
          manifestStringBuilder.append(new StringBuilder().append("add        \"").append(newFile).append("\"\n").toString());
          addFileToManifest(newFile);
        }
        newFile = advanceIterator(newFiles_iter);

      } else {
        if (oldFile.isDirectory() && newFile.isDirectory()) {
          LOGGER.debug("");
          LOGGER.debug("recursing into old " + oldFile + ", new " + newFile);
          compareDirectories(oldFile, newFile, manifestStringBuilder);
        } else {
          if (oldFile.isDirectory() || newFile.isDirectory()) {
            throw new TexaiException("cannot compare a directory with a same-named file, old " + oldFile + ", new " + newFile);
          }
          // compare contents
          LOGGER.debug("comparing equal files, old " + oldFile + ", new " + newFile);
          try {
            if (!FileUtils.contentEquals(oldFile, newFile)) {
              LOGGER.debug("replacing old file " + oldFile + ", with new file " + newFile);
              manifestStringBuilder.append(new StringBuilder().append("replace    \"").append(oldFile).append("\"\n").toString());
              addFileToManifest(newFile);
            }
          } catch (IOException ex) {
            throw new TexaiException(ex);
          }
        }

        oldFile = advanceIterator(oldFiles_iter);
        newFile = advanceIterator(newFiles_iter);
      }
    }
  }

  /**
   * Returns the next file from the given files iterator, or null if exhausted.
   *
   * @param iterator the given files iterator
   *
   * @return the next file from the given files iterator, or null if exhausted
   */
  protected static File advanceIterator(final Iterator<File> iterator) {
    //Preconditions
    assert iterator != null : "iterator must not be null";

    if (iterator.hasNext()) {
      return iterator.next();
    } else {
      return null;
    }
  }

  /**
   * Returns whether the given file or directory is ignored.
   *
   * @param file the given file or directory
   *
   * @return whether the given file or directory is ignored
   */
  protected boolean isIgnored(final File file) {
    //Preconditions
    assert file != null : "file must not be null";

    final String filePath = file.toString();
    for (final String ignoredDirectoryPath : ignoredDirectoryPaths) {
      if (ignoredDirectoryPath.equals(filePath) || filePath.endsWith('/' + ignoredDirectoryPath)) {
        return true;
      }
    }
    for (final String ignoredFilePath : ignoredFilePaths) {
      if (ignoredFilePath.equals(filePath) || filePath.endsWith('/' + ignoredFilePath)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Recursively adds the files in the given directory to the manifest.
   *
   * @param directory the given directory
   * @param manifestStringBuilder the file update mainifest
   */
  private void addDirectory(final File directory, final StringBuilder manifestStringBuilder) {
    //Preconditions
    assert directory != null : "directory must not be null";
    assert directory.isDirectory() : "directory must be a directory";
    assert manifestStringBuilder != null : "manifestStringBuilder must not be null";

    LOGGER.debug("adding new directory " + directory);
    manifestStringBuilder.append(new StringBuilder().append("add-dir    \"").append(directory).append("\"\n").toString());
    final List<File> oldFiles = new ArrayList<>();
    for (final File file : directory.listFiles()) {
      oldFiles.add(file);
    }
    Collections.sort(oldFiles);
    for (final File file : oldFiles) {
      if (file.isDirectory()) {
        addDirectory(file, manifestStringBuilder);
      } else {
        LOGGER.debug("adding new file " + file);
        manifestStringBuilder.append(new StringBuilder().append("add        \"").append(file).append("\"\n").toString());
        addFileToManifest(file);
      }
    }
  }

  /**
   * Compares the relative file paths of the old file and the new file, with respect to their given directories.
   *
   * @param oldFile the old file
   * @param newFile the new file
   *
   * @return -1 if the old file is less than the new file, 0 if equal, otherwise return +1
   */
  private int compareRelativeFilePaths(
          final File oldFile,
          final File newFile) {
    //Preconditions
    assert oldFile != null : "oldFile must not be null";
    assert newFile != null : "newFile must not be null";

    final String relativeOldFilePath = oldFile.toString().substring(oldDirectoryPath.length() + 1);
    final String relativeNewFilePath = newFile.toString().substring(newDirectoryPath.length() + 1);
    LOGGER.debug("  comparing " + relativeOldFilePath + " with " + relativeNewFilePath);
    return relativeOldFilePath.compareTo(relativeNewFilePath);
  }

  /**
   * Adds the given file to the manifest directory.
   *
   * @param file the given file
   */
  private void addFileToManifest(final File file) {
    //Preconditions
    assert file != null : "file must not be null";
    assert file.exists() : "file must exist " + file;
    assert file.isFile() : "file must be a file " + file;

    final File targetFile = new File(manifestDirectory.toString() + '/' + file.getName());
    LOGGER.debug("copying " + file + " to " + targetFile);
    try {
      FileUtils.copyFile(file, targetFile);
    } catch (IOException ex) {
      throw new TexaiException(ex);
    }
  }

  /**
   * Finalizes this application and releases its resources.
   */
  private void finalization() {
    LOGGER.info("Completed preparing the software and data deployment manifest.");

  }

  /**
   * Executes this application.
   *
   * @param args the command line arguments. arg1 = old directory, arg2 = new directory, arg3 = manifest directory
   */
  public static void main(final String[] args) {
    //Preconditions
    if (args == null) {
      throw new TexaiException("command line arguments must be present, old directory, new directory, manifest directory");
    }
    if (!StringUtils.isNonEmptyString(args[0])) {
      throw new TexaiException("old directory must be a non empty string");
    }
    if (!StringUtils.isNonEmptyString(args[1])) {
      throw new TexaiException("new directory must be a non empty string");
    }
    if (!StringUtils.isNonEmptyString(args[2])) {
      throw new TexaiException("manifest directory must be a non empty string");
    }

    final CreateSoftwareDeploymentManifest createSoftwareDeploymentManifest = new CreateSoftwareDeploymentManifest(
            args[0], // oldDirectoryPath
            args[1], // newDirectory
            args[2]); // mainifestDirectory
    createSoftwareDeploymentManifest.initialization();
    createSoftwareDeploymentManifest.process();
    createSoftwareDeploymentManifest.finalization();
  }
}
