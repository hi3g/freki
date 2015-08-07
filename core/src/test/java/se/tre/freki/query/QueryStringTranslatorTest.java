package se.tre.freki.query;

import static se.tre.freki.labels.LabelType.METRIC;
import static se.tre.freki.labels.LabelType.TAGK;
import static se.tre.freki.labels.LabelType.TAGV;

import se.tre.freki.DaggerTestComponent;
import se.tre.freki.core.LabelClient;
import se.tre.freki.query.predicate.SimpleTimeSeriesIdPredicate;
import se.tre.freki.query.predicate.TimeSeriesTagPredicate;
import se.tre.freki.storage.Store;
import se.tre.freki.utils.DescriptiveErrorListener;
import se.tre.freki.utils.TestUtil;

import com.google.common.collect.UnmodifiableIterator;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import javax.inject.Inject;

public class QueryStringTranslatorTest {
  @Rule
  public final Timeout timeout = Timeout.millis(TestUtil.TIMEOUT);

  @Inject Store store;
  @Inject LabelClient labelClient;

  @Before
  public void setUp() throws Exception {
    DaggerTestComponent.create().inject(this);

    store.createLabel("sys.cpu.0", METRIC).get();
    store.createLabel("host", TAGK).get();
    store.createLabel("web01", TAGV).get();

  }

  private TimeSeriesQuery testHelper(String query) throws Exception {
    String queryString = query;

    final ANTLRInputStream input = new ANTLRInputStream(queryString);
    final se.tre.freki.query.SelectLexer lexer = new se.tre.freki.query.SelectLexer(input);
    final CommonTokenStream tokens = new CommonTokenStream(lexer);
    final se.tre.freki.query.SelectParser parser = new se.tre.freki.query.SelectParser(tokens);

    lexer.removeErrorListeners();
    lexer.addErrorListener(DescriptiveErrorListener.INSTANCE);
    parser.removeErrorListeners();
    parser.addErrorListener(DescriptiveErrorListener.INSTANCE);

    final se.tre.freki.query.SelectParser.QueryContext tree = parser.query();
    final ParseTreeWalker treeWalker = new ParseTreeWalker();
    final QueryStringTranslator translator = new QueryStringTranslator(labelClient);
    treeWalker.walk(translator, tree);

    return translator.query().get();
  }

  @Test
  public void testCompletedQuery() throws Exception {
    String queryString = "SELECT sys.cpu.0{host=web01} BETWEEN 1 AND 5000";
    TimeSeriesQuery timeSeriesQuery = testHelper(queryString);

    Assert.assertNotNull(timeSeriesQuery);
  }

  @Test
  public void testCompletedParts() throws Exception {

    String queryString = "SELECT sys.cpu.0{host=web01} BETWEEN 1 AND 5000";
    TimeSeriesQuery timeSeriesQuery = testHelper(queryString);

    Assert.assertEquals(timeSeriesQuery.endTime(), 5000L);
    Assert.assertEquals(timeSeriesQuery.startTime(), 1L);

    UnmodifiableIterator<TimeSeriesTagPredicate> list = timeSeriesQuery
        .predicate().tagPredicates().iterator();

    while (list.hasNext()) {
      TimeSeriesTagPredicate iterator = list.next();
      SimpleTimeSeriesIdPredicate key = (SimpleTimeSeriesIdPredicate) iterator.key();
      SimpleTimeSeriesIdPredicate value = (SimpleTimeSeriesIdPredicate) iterator.value();

      Assert.assertEquals(timeSeriesQuery.predicate().metric(),
          labelClient.lookupId("sys.cpu.0", METRIC).get());
      Assert.assertEquals(key.id(), labelClient.lookupId("host", TAGK).get());
      Assert.assertEquals(value.id(), labelClient.lookupId("web01", TAGV).get());
    }
  }

  @Test(expected = QueryException.class)
  public void testMissingTagField() throws Exception {

    String queryString = "SELECT sys.cpu.0{host=5, } BETWEEN 1 AND 5000";
    testHelper(queryString);
  }

  @Test(expected = QueryException.class)
  public void testMissingTagk() throws Exception {

    String queryString = "SELECT sys.cpu.0{=5} BETWEEN 1 AND 5000";
    testHelper(queryString);
  }

  @Test(expected = QueryException.class)
  public void testMissingTagv() throws Exception {

    String queryString = "SELECT sys.cpu.0{host=} BETWEEN 1 AND 5000";
    testHelper(queryString);
  }

}
