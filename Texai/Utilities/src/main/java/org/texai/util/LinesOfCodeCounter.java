/*
 * LinesOfCodeCounter.java
 *
 * Created on March 29, 2007, 11:22 AM
 *
 * Description:
 *
 * Copyright (C) 2007 Stephen L. Reed.
 */
package org.texai.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 *
 * @author reed
 */
public final class LinesOfCodeCounter {

  // paths to count lines of code
  private static final String[] PATHS = {
    "/home/reed/git/texai/Texai/AlbusHCN/src/main",
    "/home/reed/git/texai/Texai/AlbusHCNSupport/src/main",
    "/home/reed/git/texai/Texai/CoinSkills/src/main",
    "/home/reed/git/texai/Texai/BitTorrentSupport/src/main",
    "/home/reed/git/texai/Texai/Deployment/src/main",
    "/home/reed/git/texai/Texai/Inference/src/main",
    "/home/reed/git/texai/Texai/Main/src/main",
    "/home/reed/git/texai/Texai/Network/src/main",
    "/home/reed/git/texai/Texai/RDFEntityManager/src/main",
    "/home/reed/git/texai/Texai/Security/src/main",
    "/home/reed/git/texai/Texai/Skills/src/main",
    "/home/reed/git/texai/Texai/SSLBitTorrent/src/main",
    "/home/reed/git/texai/Texai/TamperEvidentLog/src/main",
    "/home/reed/git/texai/Texai/UPNPLib/src/main",
    "/home/reed/git/texai/Texai/Utilities/src/main",
    "/home/reed/git/texai/Texai/WebServer/src/main",
    "/home/reed/git/texai/Texai/X509Security/src/main"
  };

  /**
   * Creates a new instance of LinesOfCodeCounter.
   */
  private LinesOfCodeCounter() {
    super();
  }

  /**
   * Counts the lines of code.
   */
  private void count() {
    try {
      int numberOfLines;
      int totalNumberOfLines = 0;
      for (final String path : PATHS) {
        System.out.println("directory name: " + path);
        numberOfLines = getNumberOfLinesInDir(path);
        totalNumberOfLines = totalNumberOfLines + numberOfLines;
        System.out.println("There are " + numberOfLines + " lines of Java code in the specified directory tree");
      }
      System.out.println("There are " + totalNumberOfLines + " total lines of Java code");
    } catch (final IOException ex) {
      System.out.println(ex);
      System.out.println(StringUtils.getStackTraceAsString(ex));
    }
  }

  /**
   * Executes this application.
   *
   * @param args the command line arguments (unused)
   */
  public static void main(final String[] args) {
    final LinesOfCodeCounter linesOfCodeCounter = new LinesOfCodeCounter();
    linesOfCodeCounter.count();
  }

  /**
   * Returns the number of lines in the specified file.
   *
   * @param filePath the specified file
   *
   * @return the number of lines in the specified file
   * @throws java.io.IOException when an input/output error occurs
   */
  private int getNumberOfLines(final String filePath) throws IOException {
    int numberOfLines;
    final BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(filePath)));
    numberOfLines = 0;
    try {
      String line = bufferedReader.readLine();
      boolean inComment = false;
      while (line != null) {
        if (inComment) {
          if (line.contains("*/")) {
            inComment = false;
          }
        } else if (!line.trim().startsWith("//")) {
          if (line.contains("/*")) {
            if (!line.contains("*/")) {
              inComment = true;
            }
          } else if (!inComment && !line.trim().isEmpty()) {
            numberOfLines++;
          }
        }
        line = bufferedReader.readLine();
      }
    } catch (final IOException ex) {
      throw new IOException(ex);
    } finally {
      bufferedReader.close();
    }
    System.out.println(filePath + ": " + numberOfLines);
    return numberOfLines;
  }

  /**
   * Returns the total number of lines of Java code in the specified directory and its subdirectories.
   *
   * @param directoryPath the specified directory
   *
   * @return the total number of lines of Java code in the specified directory
   * @throws java.io.IOException when an input/output error occurs
   */
  private int getNumberOfLinesInDir(final String directoryPath) throws IOException {
    int numberOfLines;
    final File directory = new File(directoryPath);
    final File[] files = directory.listFiles();
    if (files == null) {
      return 0;
    }
    numberOfLines = 0;
    for (final File file : files) {
      if (file.isFile()) {
        if (file.getName().endsWith(".java")) {
          numberOfLines = numberOfLines + getNumberOfLines(file.getPath());
        }
      } else if (file.isDirectory()) {
        numberOfLines = numberOfLines + getNumberOfLinesInDir(file.getPath());
      }
    }
    if (numberOfLines > 0 && directoryPath.endsWith("src")) {
      System.out.println(directoryPath + ": " + numberOfLines);
    }
    return numberOfLines;
  }
}
