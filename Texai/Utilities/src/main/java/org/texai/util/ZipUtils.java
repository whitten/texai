package org.texai.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.apache.log4j.Logger;

/**
 * ZipUtils.java
 *
 * Description: Provides compression and decompression utility methods.
 *
 * Copyright (C) Jan 21, 2015, Stephen L. Reed.
 */
public class ZipUtils {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(ZipUtils.class);

  /**
   * Prevents instantiation of this utility class.
   */
  private ZipUtils() {
  }

  /**
   * Archives the files in the given directory into a zip-format byte array.
   *
   * @param directory the given directory
   *
   * @return a byte array in ZIP archive format
   */
  public static byte[] archiveFilesToByteArray(final File directory) {
    //Preconditions
    assert directory != null : "directory must not be null";
    assert directory.isDirectory() : "directory must be a directory";

    LOGGER.info("creating ziped byte array from files in directory " + directory);
    final int bufferSize = 4096;
    try {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      try (ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(byteArrayOutputStream))) {
        byte data[] = new byte[bufferSize];
        for (final File file : directory.listFiles()) {
          LOGGER.info("  adding: " + file.getName());
          try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file), bufferSize)) {
            ZipEntry entry = new ZipEntry(file.getName());
            zipOutputStream.putNextEntry(entry);
            int count;
            while ((count = bufferedInputStream.read(data, 0, bufferSize)) != -1) {
              zipOutputStream.write(data, 0, count);
            }
          }
        }
      }
      return byteArrayOutputStream.toByteArray();
    } catch (Exception ex) {
      throw new TexaiException(ex);
    }
  }

  /**
   * Returns a temporary ZipFile from the given zip-format byte array.
   *
   * @param bytes a byte array in ZIP archive format
   *
   * @return a temporary ZipFile
   */
  public static ZipFile temporaryZipFile(final byte[] bytes) {
    //Preconditions
    assert bytes != null : "bytes must not be null";

    try {
      final File zipFile = File.createTempFile("archive", ".zip");
      try (FileOutputStream fileOuputStream = new FileOutputStream(zipFile)) {
        fileOuputStream.write(bytes);
      }
      return new ZipFile(zipFile);
    } catch (IOException ex) {
      throw new TexaiException(ex);
    }
  }

}
