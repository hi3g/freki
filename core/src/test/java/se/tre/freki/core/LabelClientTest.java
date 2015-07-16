package se.tre.freki.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static se.tre.freki.labels.LabelType.METRIC;
import static se.tre.freki.labels.LabelType.TAGK;
import static se.tre.freki.labels.LabelType.TAGV;

import se.tre.freki.DaggerTestComponent;
import se.tre.freki.labels.LabelId;
import se.tre.freki.storage.Store;
import se.tre.freki.utils.TestUtil;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.util.concurrent.ExecutionException;
import javax.inject.Inject;

public class LabelClientTest {
  @Rule
  public final Timeout timeout = Timeout.millis(TestUtil.TIMEOUT);

  @Inject Store store;
  @Inject LabelClient labelClient;

  private LabelId sysCpu0;
  private LabelId host;
  private LabelId web01;

  @Before
  public void setUp() throws Exception {
    DaggerTestComponent.create().inject(this);

    sysCpu0 = store.createLabel("sys.cpu.0", METRIC).get();
    host = store.createLabel("host", TAGK).get();
    web01 = store.createLabel("web01", TAGV).get();
  }

  @Test(expected = IllegalArgumentException.class)
  public void createUidInvalidCharacter() {
    labelClient.createId(METRIC, "Not!A:Valid@Name");
  }

  @Test(expected = NullPointerException.class)
  public void createUidNullName() {
    labelClient.createId(METRIC, null);
  }

  @Test(expected = NullPointerException.class)
  public void createUidNullType() {
    labelClient.createId(null, "localhost");
  }

  @Test
  public void createUidTagKey() {
    assertNotNull(labelClient.createId(TAGK, "region"));
  }

  @Test
  public void createUidTagKeyExists() throws Exception {
    try {
      labelClient.createId(TAGK, "host").get();
      fail("The id should have existed and therefore an exception should have been thrown");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof IllegalArgumentException);
    }
  }

  @Test
  public void getLabelId() throws Exception {
    assertEquals(sysCpu0, labelClient.getLabelId(METRIC, "sys.cpu.0").get().get());
  }

  @Test(expected = IllegalArgumentException.class)
  public void getLabelIdEmptyName() {
    labelClient.getLabelId(TAGV, "");
  }

  @Test
  public void getLabelIdNoSuchName() throws Exception {
    assertFalse(labelClient.getLabelId(METRIC, "sys.cpu.2").get().isPresent());
  }

  @Test(expected = IllegalArgumentException.class)
  public void getLabelIdNullName() {
    labelClient.getLabelId(TAGV, null);
  }

  @Test(expected = NullPointerException.class)
  public void getLabelIdNullType() {
    labelClient.getLabelId(null, "sys.cpu.1");
  }

  @Test
  public void getUidName() throws Exception {
    assertEquals("web01", labelClient.getLabelName(TAGV, web01).get().get());
  }

  @Test
  public void getLabelNameNoSuchId() throws Exception {
    assertFalse(labelClient.getLabelName(TAGV, mock(LabelId.class)).get().isPresent());
  }

  @Test(expected = NullPointerException.class)
  public void getUidNameNullType() throws Exception {
    labelClient.getLabelName(null, mock(LabelId.class));
  }

  @Test(expected = NullPointerException.class)
  public void getLabelNameNullId() throws Exception {
    labelClient.getLabelName(TAGV, null);
  }

  @Test
  public void validateLabelName() {
    final String validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUWVXYZ0123456789-_./";

    for (char c = 0; c < 255; ++c) {
      final String input = String.valueOf(c);
      try {
        LabelClient.validateLabelName("test", input);
        assertTrue("character " + input.charAt(0) + " with code " + ((int) input.charAt(
                0)) + " is not in the valid chars",
            validChars.contains(input));
      } catch (IllegalArgumentException e) {
        assertFalse(validChars.contains(input));
      }
    }
  }

  @Test(expected = NullPointerException.class)
  public void validateLabelNameNullString() {
    LabelClient.validateLabelName("test", null);
  }
}
