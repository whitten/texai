package org.texai.deployment;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
public class CreateSoftwareDeploymentManifestTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(CreateSoftwareDeploymentManifestTest.class);

  public CreateSoftwareDeploymentManifestTest() {
  }

  @BeforeClass
  public static void setUpClass() {
    final File testManifestsDirectory = new File("data/test-deployment-manifests");
    assertTrue(testManifestsDirectory.exists());
    final File[] files = testManifestsDirectory.listFiles();
    for (final File file : files) {
      if (!file.isHidden()) {
        LOGGER.info("deleting previous test file: " + file);
        final boolean isOK = file.delete();
        assertTrue(isOK);
      }
    }

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
   * Test of formatJSON method, of class CreateSoftwareDeploymentManifest.
   */
  @Test
  public void testformatJSON() {
    LOGGER.info("formatJSON");
    final String jsonString = "{\"path\":\"data\\/test-deployment-dir-new\\/file-b-change\","
            + "\"command\":\"replace\","
            + "\"hash\":\"vfeSHKnz8dO44jB3KKVWtIjfBRTBOUCbFd6UK8vEfZagZrsFaghsr3gBccz7IvqgZ7HyJ5p3\\/wy+i8S+29fDMg==\"},"
            + "{\"path\":\"data\\/test-deployment-dir-old\\/file-h-remove\","
            + "\"command\":\"remove\"},"
            + "{\"path\":\"data\\/test-deployment-dir-new\\/file-i-add\","
            + "\"command\":\"add\","
            + "\"hash\":\"z4PhNX7vuL3xVChQ1m2AB9Yg5AULVxXcg\\/SpIdNs6c5H0NE8XYXysP+DGNKHfuwvY7kxvUdBeoGlODJ6+SfaPg==\"},"
            + "{\"path\":\"data\\/test-deployment-dir-new\\/subdirectory\\/CoinSkills-1.0.jar\","
            + "\"command\":\"replace\","
            + "\"hash\":\"BSKS7ixZiAqIBQYet9yUZizefr2I8m7NyTLRIHNNKVuOhtwjkZi4L3lg27WLccVOHW+MIXiDRQRAoHG3mvVL2g==\"},"
            + "{\"path\":\"data\\/test-deployment-dir-new\\/subdirectory\\/file-f-add\","
            + "\"command\":\"add\","
            + "\"hash\":\"z4PhNX7vuL3xVChQ1m2AB9Yg5AULVxXcg\\/SpIdNs6c5H0NE8XYXysP+DGNKHfuwvY7kxvUdBeoGlODJ6+SfaPg==\"},"
            + "{\"path\":\"data\\/test-deployment-dir-old\\/subdirectory\\/file-x-remove\","
            + "\"command\":\"remove\"},"
            + "{\"path\":\"data\\/test-deployment-dir-new\\/subdirectory-add\","
            + "\"command\":\"add-dir\"},"
            + "{\"path\":\"data\\/test-deployment-dir-new\\/subdirectory-add\\/file-g-add\","
            + "\"command\":\"add\","
            + "\"hash\":\"n8hoSN8jX9CMGPphSvfX0FRpRqyPeveBCF7ynPAMSvPjcQiIju8w7f2\\/RjRpM0JQoVrFGtVH31xcMQ82Eb+I+g==\"},"
            + "{\"path\":\"data\\/test-deployment-dir-old\\/subdirectory-remove\","
            + "\"command\":\"remove\"}]}";

    final String formattedString = "{\"path\":\"data\\/test-deployment-dir-new\\/file-b-change\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"vfeSHKnz8dO44jB3KKVWtIjfBRTBOUCbFd6UK8vEfZagZrsFaghsr3gBccz7IvqgZ7HyJ5p3\\/wy+i8S+29fDMg==\"},\n"
            + "  \n"
            + "{\"path\":\"data\\/test-deployment-dir-old\\/file-h-remove\",\n"
            + "  \"command\":\"remove\"},\n"
            + "  \n"
            + "{\"path\":\"data\\/test-deployment-dir-new\\/file-i-add\",\n"
            + "  \"command\":\"add\",\n"
            + "  \"hash\":\"z4PhNX7vuL3xVChQ1m2AB9Yg5AULVxXcg\\/SpIdNs6c5H0NE8XYXysP+DGNKHfuwvY7kxvUdBeoGlODJ6+SfaPg==\"},\n"
            + "  \n"
            + "{\"path\":\"data\\/test-deployment-dir-new\\/subdirectory\\/CoinSkills-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"BSKS7ixZiAqIBQYet9yUZizefr2I8m7NyTLRIHNNKVuOhtwjkZi4L3lg27WLccVOHW+MIXiDRQRAoHG3mvVL2g==\"},\n"
            + "  \n"
            + "{\"path\":\"data\\/test-deployment-dir-new\\/subdirectory\\/file-f-add\",\n"
            + "  \"command\":\"add\",\n"
            + "  \"hash\":\"z4PhNX7vuL3xVChQ1m2AB9Yg5AULVxXcg\\/SpIdNs6c5H0NE8XYXysP+DGNKHfuwvY7kxvUdBeoGlODJ6+SfaPg==\"},\n"
            + "  \n"
            + "{\"path\":\"data\\/test-deployment-dir-old\\/subdirectory\\/file-x-remove\",\n"
            + "  \"command\":\"remove\"},\n"
            + "  \n"
            + "{\"path\":\"data\\/test-deployment-dir-new\\/subdirectory-add\",\n"
            + "  \"command\":\"add-dir\"},\n"
            + "  \n"
            + "{\"path\":\"data\\/test-deployment-dir-new\\/subdirectory-add\\/file-g-add\",\n"
            + "  \"command\":\"add\",\n"
            + "  \"hash\":\"n8hoSN8jX9CMGPphSvfX0FRpRqyPeveBCF7ynPAMSvPjcQiIju8w7f2\\/RjRpM0JQoVrFGtVH31xcMQ82Eb+I+g==\"},\n"
            + "  \n"
            + "{\"path\":\"data\\/test-deployment-dir-old\\/subdirectory-remove\",\n"
            + "  \"command\":\"remove\"}]}";
    assertEquals(formattedString, CreateSoftwareDeploymentManifest.formatJSON(jsonString));
  }

  /**
   * Test of advanceIterator method, of class CreateSoftwareDeploymentManifest.
   */
  @Test
  public void testAdvanceIterator() {
    LOGGER.info("advanceIterator");
    final List<File> files = new ArrayList<>();
    files.add(new File("a"));
    files.add(new File("b"));
    final Iterator<File> files_iter = files.iterator();
    File file = CreateSoftwareDeploymentManifest.advanceIterator(files_iter);
    assertEquals("a", file.toString());
    file = CreateSoftwareDeploymentManifest.advanceIterator(files_iter);
    assertEquals("b", file.toString());
    file = CreateSoftwareDeploymentManifest.advanceIterator(files_iter);
    assertNull(file);
    assertTrue(!files_iter.hasNext());
  }

  /**
   * Test of formRelativePath method, of class CreateSoftwareDeploymentManifest.
   */
  @Test
  @SuppressWarnings(value = "DMI_HARDCODED_ABSOLUTE_FILENAME", justification = "unit test")
  public void testFormRelativePath() {
    LOGGER.info("formRelativePath");

    final String oldDirectoryPath = "/a/b/c/";
    final String newDirectoryPath = "/d/e/f/";
    File file = new File("/a/b/c/myfile.txt");
    String relativePath = CreateSoftwareDeploymentManifest.formRelativePath(
            oldDirectoryPath,
            newDirectoryPath,
            file);
    assertEquals("myfile.txt", relativePath);
    file = new File("/d/e/f/myfile.txt");
    relativePath = CreateSoftwareDeploymentManifest.formRelativePath(
            oldDirectoryPath,
            newDirectoryPath,
            file);
    assertEquals("myfile.txt", relativePath);
    final File oldDirectory = new File(oldDirectoryPath);
    final File newDirectory = new File(newDirectoryPath);
    relativePath = CreateSoftwareDeploymentManifest.formRelativePath(
            oldDirectory,
            newDirectory,
            file);
    assertEquals("myfile.txt", relativePath);
    file = new File("/a/b/c/myfile.txt");
    relativePath = CreateSoftwareDeploymentManifest.formRelativePath(
            oldDirectory,
            newDirectory,
            file);
    assertEquals("myfile.txt", relativePath);
  }

  /**
   * Test of isIgnored method, of class CreateSoftwareDeploymentManifest.
   */
  @Test
  public void testIsIgnored() {
    LOGGER.info("isIgnored");

    CreateSoftwareDeploymentManifest createSoftwareDeploymentManifest = new CreateSoftwareDeploymentManifest(
            "data/test-deployment-dir-old", // oldDirectoryPath
            "data/test-deployment-dir-new", // newDirectoryPath
            "data/test-deployment-manifests"); // manifestDirectoryPath
    assertTrue(createSoftwareDeploymentManifest.isIgnored(new File(".aicoin")));
    assertTrue(createSoftwareDeploymentManifest.isIgnored(new File("keystore.uber")));
    assertFalse(createSoftwareDeploymentManifest.isIgnored(new File("nodes.xml")));
  }

  /**
   * Test of main method, of class CreateSoftwareDeploymentManifest.
   */
  @Test
  public void testMain() {
    LOGGER.info("main");
    String[] args = {
      "data/test-deployment-dir-old",
      "data/test-deployment-dir-new",
      "data/test-deployment-manifests"
    };
    LOGGER.info("working directory: " + System.getProperty("user.dir"));
    CreateSoftwareDeploymentManifest.main(args);
  }

}
