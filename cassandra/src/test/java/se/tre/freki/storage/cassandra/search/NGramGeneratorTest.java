package se.tre.freki.storage.cassandra.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class NGramGeneratorTest {

  @Test
  public void testOneCharLabelName() throws Exception {
    final NGramGenerator ngrams = new NGramGenerator("i");
    assertEquals("i", ngrams.next());
    assertFalse(ngrams.hasNext());
  }

  @Test
  public void testShortLabelName() throws Exception {
    final NGramGenerator ngrams = new NGramGenerator("is");
    assertEquals("is", ngrams.next());
    assertFalse(ngrams.hasNext());
  }

  @Test
  public void testExactLabelName() throws Exception {
    final NGramGenerator ngrams = new NGramGenerator("the");
    assertEquals("the", ngrams.next());
    assertFalse(ngrams.hasNext());
  }

  @Test
  public void testMultipleLabelName() throws Exception {
    final NGramGenerator ngrams = new NGramGenerator("invoke");
    assertEquals("inv", ngrams.next());
    assertEquals("nvo", ngrams.next());
    assertEquals("vok", ngrams.next());
    assertEquals("oke", ngrams.next());
    assertFalse(ngrams.hasNext());
  }

  @Test
  public void testNonMultipleLabelName() throws Exception {
    final NGramGenerator ngrams = new NGramGenerator("hello");
    assertEquals("hel", ngrams.next());
    assertEquals("ell", ngrams.next());
    assertEquals("llo", ngrams.next());
    assertFalse(ngrams.hasNext());
  }
}
