/*
 * SingleLineCommentRewriter.java
 *
 * Created on Sep 18, 2014, 2:36:15 PM
 *
 * Description: Rewrites block comments into single line comments in the declarations section of the Java program.
 *
 * Copyright 2014 by Stephen L. Reed
 */
package org.texai.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

/**
 *
 * @author reed
 */
public final class SingleLineCommentRewriter extends DirectoryWalker<String> {

  // the logger
  private final static Logger LOGGER = Logger.getLogger(SingleLineCommentRewriter.class);

  /**
   * Creates a new instance of LinesOfCodeCounter.
   */
  protected SingleLineCommentRewriter() {

  }

  /**
   * Handle the file received from the directory walker.
   *
   * @param file the current file being processed
   * @param depth the current directory level (starting directory = 0)
   * @param results the list of processed file names
   *
   * @throws java.io.IOException
   */
  @Override
  @edu.umd.cs.findbugs.annotations.SuppressWarnings({"DMI_HARDCODED_ABSOLUTE_FILENAME"})
  protected void handleFile(final File file, final int depth, final Collection<String> results) throws IOException {
    //Preconditions
    assert file != null : "file must not be null";
    assert file.isFile() : "file " + file + " must be a file";
    assert depth >= 0 : "depth must not be negative";
    assert results != null : "results must not be null";

    if (!file.getName().endsWith(".java")) {
      return;
    }
    if (!file.getAbsolutePath().contains("/src/main/java/org/texai/")) {
      return;
    }
    // make a temp file
    final File systemTemporaryDirectory = new File("/var/tmp");
    final File temporaryFile = new File(systemTemporaryDirectory, "java-file.tmp");
    // copy file to temp
    FileUtils.copyFile(file, temporaryFile);
    // rewrite the file from temp
    formatAndRewrite(temporaryFile, file);
    results.add(file.getCanonicalPath());
  }

  /**
   * Formats the given temporary file, replacing block comments in the declarative section with single line comments, writing the output to
   * the given file.
   *
   * @param temporaryFile the input temporary file
   * @param file the output file
   */
  protected void formatAndRewrite(final File temporaryFile, final File file) {
    //Preconditions
    assert temporaryFile != null : "temporaryFile must not be null";
    assert temporaryFile.isFile() : "temporaryFile " + temporaryFile + " must be a file";
    assert file != null : "file must not be null";

    LOGGER.debug("input temporary file: " + temporaryFile);
    LOGGER.debug("output file:          " + file);
    try (
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(temporaryFile), "UTF-8"));
            final BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {

      // copy input lines to output until the class header is found
      String line = bufferedReader.readLine();
      boolean isClass = false;
      boolean isInterface = false;
      int index;
      String className = null;
      while (line != null) {
        LOGGER.debug("line: " + line);
        if (!isInterface) {
          index = line.indexOf(" class ");
          if (index > -1) {
            className = line.substring(index + 7).split(" ")[0].trim();
            index = className.indexOf('<');
            if (index > -1) {
              className = className.substring(0, index);
            }
            if (StringUtils.isJavaClassName(className)) {
              isClass = true;
              LOGGER.debug("class name: '" + className + "'");
            }
          }
          if (!isClass) {
            index = line.indexOf(" interface ");
            if (index > -1) {
              final String interfaceName = line.substring(index + 11).split(" ")[0];
              if (index > -1) {
                LOGGER.debug("interface name: '" + interfaceName + "'");
                isInterface = true;
              }
            }
          }
        }
        bufferedWriter.write(line);
        bufferedWriter.newLine();
        if (isClass) {
          break;
        }
        line = bufferedReader.readLine();
      }
      if (line == null) {
        return;
      }

      // find block comments and rewrite them as single line comments, until the class constructor is found
      final String classHeader = " " + className + "(";
      boolean isClassHeaderFound = false;
      boolean isBeginBlockComment = false;
      boolean isEndBlockComment = false;
      String comment;
      final List<String> comments = new ArrayList<>();
      final List<String> blockComments = new ArrayList<>();
      while (true) {
        line = bufferedReader.readLine();
        if (line == null) {
          break;
        }
        LOGGER.debug("line: " + line);
        index = line.indexOf(classHeader);
        if (index > -1) {
          isClassHeaderFound = true;
          LOGGER.debug("class header found: " + line);
          assert !isBeginBlockComment;
        }
        if (isEndBlockComment) {
          if (isClassHeaderFound) {
            // preserve the block comments for the first constructor method
            blockComments.stream().forEach((blockComment) -> {
              try {
                bufferedWriter.write(blockComment);
                bufferedWriter.newLine();
              } catch (IOException ex) {
                throw new TexaiException(ex);
              }
            });
          } else {
            // emit the single line comments
            comments.stream().forEach((singleLineComment) -> {
              try {
                bufferedWriter.write("  // " + singleLineComment);
                bufferedWriter.newLine();
              } catch (IOException ex) {
                throw new TexaiException(ex);
              }
            });
          }
          isEndBlockComment = false;
          blockComments.clear();
          comments.clear();
        }

        if (line.startsWith("  /**")) {
          blockComments.add(line);
          isBeginBlockComment = true;
          comment = line.substring(index + 6).trim();
          if (!comment.isEmpty()) {
            if (comment.endsWith("*/")) {
              isBeginBlockComment = false;
              isEndBlockComment = true;
              comment = comment.substring(0, comment.length() - 2).trim();
              if (!comment.isEmpty()) {
                comments.add(comment);
                LOGGER.debug("comment: " + comment);
              }
            }
          }
        } else if (isBeginBlockComment) {
          blockComments.add(line);
          comment = line.trim();
          if (comment.endsWith("*/")) {
            isBeginBlockComment = false;
            isEndBlockComment = true;
            comment = comment.substring(0, comment.length() - 2).trim();
            if (comment.startsWith("*")) {
              comment = comment.substring(1).trim();
            }
            if (!comment.isEmpty()) {
              comments.add(comment);
              LOGGER.debug("comment: " + comment);
            }
          } else if (comment.startsWith("*")) {
            comment = comment.substring(1).trim();
            comments.add(comment);
            LOGGER.debug("comment: " + comment);
          }
        }
        if (isEndBlockComment || isBeginBlockComment) {
          continue;
        }

        bufferedWriter.write(line);
        bufferedWriter.newLine();
        if (isClassHeaderFound) {
          break;
        }
      }

      // copy the remainder of the input to the output
      while (true) {
        line = bufferedReader.readLine();
        if (line == null) {
          return;
        } else {
          bufferedWriter.write(line);
          bufferedWriter.newLine();
        }
      }
    } catch (final IOException ex) {
      throw new TexaiException(ex);
    }
  }

  /**
   * Executes this application.
   *
   * @param args the command line arguments (unused)
   */
  public static void main(final String[] args) {
//    if (args.length == 0) {
//      throw new TexaiException("the start directory argument is missing");
//    }
//    final String startDirectoryString = args[0];
    final String startDirectoryString = "../AlbusHCNSupport/src/main/java";
    final File startDirectory = new File(startDirectoryString);
    try {
      LOGGER.info("startDirectory: " + startDirectory.getCanonicalPath());
    } catch (IOException ex) {
      throw new TexaiException(ex);
    }
    if (!startDirectory.exists()) {
      throw new TexaiException("the start directory '" + startDirectory + "' does not exist");
    }
    if (!startDirectory.isDirectory()) {
      throw new TexaiException("the given start directory '" + startDirectory + "' must be a directory");
    }
    final SingleLineCommentRewriter singleLineCommentRewriter = new SingleLineCommentRewriter();
    final List<String> processedFiles = new ArrayList<>();
    try {
      singleLineCommentRewriter.walk(startDirectory, processedFiles);
    } catch (IOException ex) {
      throw new TexaiException(ex);
    }
    processedFiles.stream().sorted().forEach((processedFile) -> {
      LOGGER.info(processedFile);
    });
  }
}
