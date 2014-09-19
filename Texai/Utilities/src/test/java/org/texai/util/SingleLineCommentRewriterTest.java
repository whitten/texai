package org.texai.util;

import java.io.File;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author reed
 */
public class SingleLineCommentRewriterTest {
  
  // the logger
  private final static Logger LOGGER = Logger.getLogger(SingleLineCommentRewriterTest.class);
  
  public SingleLineCommentRewriterTest() {
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
   * Test of formatAndRewrite method, of class SingleLineCommentRewriter.
   */
  @Test
  public void testFormatAndRewrite() {
    LOGGER.info("formatAndRewrite");
    final File temporaryFile = new File("data/comment-rewrite-input-test.java");
    final File file = new File("data/comment-rewrite-output-test.java");
    SingleLineCommentRewriter instance = new SingleLineCommentRewriter();
    instance.formatAndRewrite(temporaryFile, file);
  }

  /**
   * Test of main method, of class SingleLineCommentRewriter.
   */
  @Test
  public void testMain() {
    LOGGER.info("main");
    final String[] args = {"data/comment-rewrite-test-directory1"};
    SingleLineCommentRewriter.main(args);
  }

}
