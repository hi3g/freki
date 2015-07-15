package se.tre.freki.storage.cassandra.functions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

public class IsEmptyFunctionTest {
  private IsEmptyFunction isEmptyFunction;

  @Before
  public void setUp() throws Exception {
    isEmptyFunction = new IsEmptyFunction();
  }

  @Test
  public void testApplyEmptyCollection() throws Exception {
    assertTrue(isEmptyFunction.apply(ImmutableList.of()));
  }

  @Test
  public void testApplyNonEmptyCollection() throws Exception {
    assertFalse(isEmptyFunction.apply(ImmutableList.of("item")));
  }
}
