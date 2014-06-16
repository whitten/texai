/*
 * LinesOfCodeCounter.java
 *
 * Created on March 29, 2007, 11:22 AM
 *
 * Description:
 *
 * Copyright (C) 2007 Stephen L. Reed.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
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

  /** paths to count lines of code */
  private static final String[] PATHS = {
    "/home/reed/svn/JSAPI_Player/src",
    "/home/reed/svn/SpeechRecognition/src",
    "/home/reed/svn/ProfilingSecurityManager/src",
    "/home/reed/svn/X509CertificateServer/src",
    "/home/reed/svn/Texai/AlbusHCN/src/main",
    "/home/reed/svn/Texai/AlbusHCNSupport/src/main",
    "/home/reed/svn/Texai/BehaviorLanguage/src/main",
    "/home/reed/svn/Texai/BehaviorLanguageSupport/src/main",
    "/home/reed/svn/Texai/BitcoinTrader/src/main",
    "/home/reed/svn/Texai/BitTorrentSupport/src/main",
    "/home/reed/svn/Texai/Dialog/src/main",
    "/home/reed/svn/Texai/DialogSupport/src/main",
    "/home/reed/svn/Texai/DiscoursePlanner/src/main",
    "/home/reed/svn/Texai/ExactEnglish/src/main",
    "/home/reed/svn/Texai/GraphWriter/src/main",
    "/home/reed/svn/Texai/IncrementalFCG/src/main",
    "/home/reed/svn/Texai/Inference/src/main",
    "/home/reed/svn/Texai/JabberDialogAdapter/src/main",
    "/home/reed/svn/Texai/JavaComposition/src/main",
    "/home/reed/svn/Texai/Main/src/main",
    "/home/reed/svn/Texai/Network/src/main",
    "/home/reed/svn/Texai/RDFEntityManager/src/main",
    "/home/reed/svn/Texai/ScriptedBehaviorLanguage/src/main",
    "/home/reed/svn/Texai/Security/src/main",
    "/home/reed/svn/Texai/ShiftKeyPresser/src/main",
    "/home/reed/svn/Texai/Skills/src/main",
    "/home/reed/svn/Texai/SpreadingActivation/src/main",
    "/home/reed/svn/Texai/SSLBitTorrent/src/main",
    "/home/reed/svn/Texai/TexaiLauncher/src/main",
    "/home/reed/svn/Texai/Utilities/src/main",
    "/home/reed/svn/Texai/VNCRobot/src/main",
    "/home/reed/svn/Texai/VNCServer/src/main",
    "/home/reed/svn/Texai/WebServer/src/main",
    "/home/reed/svn/Texai/WebServerSupport/src/main",
    "/home/reed/svn/Texai/WorkFlow/src/main",
    "/home/reed/svn/Texai/X509CertificateServerTest/src/main",
    "/home/reed/svn/Texai/X509Security/src/main"
  };

  /** Creates a new instance of LinesOfCodeCounter. */
  private LinesOfCodeCounter() {
    super();
  }

  /** Counts the lines of code. */
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


  /** Executes this application.
   *
   * @param args the command line arguments (unused)
   */
  public static void main(final String[] args) {
    final LinesOfCodeCounter linesOfCodeCounter = new LinesOfCodeCounter();
    linesOfCodeCounter.count();
  }

  /** Returns the number of lines in the specified file.
   *
   * @param filePath the specified file
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

  /** Returns the total number of lines of Java code in the specified directory and its subdirectories.
   *
   * @param directoryPath the specified directory
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

