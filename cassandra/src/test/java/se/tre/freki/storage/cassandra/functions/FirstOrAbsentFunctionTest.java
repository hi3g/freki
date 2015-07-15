package se.tre.freki.storage.cassandra.functions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

public class FirstOrAbsentFunctionTest {
  private FirstOrAbsentFunction<String> objectFirstOrAbsentFunction;

  @Before
  public void setUp() throws Exception {
    objectFirstOrAbsentFunction = new FirstOrAbsentFunction<>();
  }

  @Test
  public void testApplyEmptyList() throws Exception {
    assertFalse(objectFirstOrAbsentFunction.apply(ImmutableList.<String>of()).isPresent());
  }

  @Test
  public void testApplyReturnsFirst() throws Exception {
    ImmutableList<String> list = ImmutableList.of("first", "second");
    assertEquals("first", objectFirstOrAbsentFunction.apply(list).get());
  }
}
