/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author reed
 */
public class ZipUtilsTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(ZipUtilsTest.class);

  public ZipUtilsTest() {
  }

  @BeforeClass
  public static void setUpClass() {
  }

  @AfterClass
  public static void tearDownClass() {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of class ZipUtils.
   */
  @Test
  public void testArchiveFilesToByteArray() {
    LOGGER.info("archiveFilesToByteArray");
    final File directory = new File("data/test-directory");
    final File[] inputFiles = directory.listFiles();
    final List<String> inputFileNames = new ArrayList<>();
    for (final File inputFile : inputFiles) {
      inputFileNames.add(inputFile.getName());
    }
    Collections.sort(inputFileNames);
    assertEquals("[file1.txt, file2.txt, file3.txt]", inputFileNames.toString());
    
    final byte[] zippedBytes = ZipUtils.archiveFilesToByteArray(directory);
    assertEquals(394, zippedBytes.length);

    final File testDirectoryUnzipped = new File("data/test-directory-unzipped");
    if (testDirectoryUnzipped.exists()) {
      assertTrue(testDirectoryUnzipped.isDirectory());
      FileUtils.deleteQuietly(testDirectoryUnzipped);
    }
    assertFalse(testDirectoryUnzipped.exists());
    boolean isOK = testDirectoryUnzipped.mkdirs();
    assertTrue(isOK);
    assertTrue(testDirectoryUnzipped.exists());
    assertTrue(testDirectoryUnzipped.isDirectory());

    final ZipFile zipFile = ZipUtils.temporaryZipFile(zippedBytes);
    LOGGER.info("zipFile: " + zipFile.getName());
    assertTrue(zipFile.getName().startsWith("/tmp/archive"));
    assertTrue(zipFile.getName().endsWith(".zip"));
    final Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
    final int bufferSize = 4096;
    while (zipEntries.hasMoreElements()) {
      final ZipEntry zipEntry = zipEntries.nextElement();
      final String name = zipEntry.getName();
      LOGGER.info("  extracting " + name);
      try {
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(zipFile.getInputStream(zipEntry))) {
          int count;
          byte data[] = new byte[bufferSize];
          final FileOutputStream fileOutputStream = new FileOutputStream("data/test-directory-unzipped/" + name);
          try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream, bufferSize)) {
            while ((count = bufferedInputStream.read(data, 0, bufferSize))
                    != -1) {
              bufferedOutputStream.write(data, 0, count);
            }
            bufferedOutputStream.flush();
          }
        }
      } catch (IOException ex) {
        throw new TexaiException(ex);
      }
    }

    final File[] extractedFiles = testDirectoryUnzipped.listFiles();
    final List<String> extractedFileNames = new ArrayList<>();
    for (final File extractedFile : extractedFiles) {
      extractedFileNames.add(extractedFile.getName());
    }
    Collections.sort(extractedFileNames);
    assertEquals("[file1.txt, file2.txt, file3.txt]", extractedFileNames.toString());



  }

}
