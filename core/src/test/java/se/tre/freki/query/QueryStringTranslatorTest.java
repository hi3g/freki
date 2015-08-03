package se.tre.freki.query;

import static se.tre.freki.labels.LabelType.METRIC;
import static se.tre.freki.labels.LabelType.TAGK;
import static se.tre.freki.labels.LabelType.TAGV;

import se.tre.freki.DaggerTestComponent;
import se.tre.freki.core.LabelClient;
import se.tre.freki.labels.LabelId;
import se.tre.freki.query.predicate.SimpleTimeSeriesIdPredicate;
import se.tre.freki.query.predicate.TimeSeriesTagPredicate;
import se.tre.freki.storage.Store;
import se.tre.freki.utils.TestUtil;

import com.google.common.collect.UnmodifiableIterator;

import junit.framework.TestCase;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.util.concurrent.ExecutionException;
import javax.inject.Inject;

public class QueryStringTranslatorTest extends TestCase {
  @Rule
  public final Timeout timeout = Timeout.millis(TestUtil.TIMEOUT);

  @Inject Store store;
  @Inject LabelClient labelClient;

  private LabelId sysCpu0;
  private LabelId web01;
  private LabelId host;

  @Before
  public void setUp() throws Exception {
    DaggerTestComponent.create().inject(this);

    sysCpu0 = store.createLabel("sys.cpu.0", METRIC).get();
    host = store.createLabel("host", TAGK).get();
    web01 = store.createLabel("web01", TAGV).get();

  }

  @Test
  public void testCompletedQuery() {
    String queryString = "SELECT sys.cpu.0{host=web01} BETWEEN 1 AND 5000";

    final ANTLRInputStream input = new ANTLRInputStream(queryString);
    final se.tre.freki.query.SelectLexer lexer = new se.tre.freki.query.SelectLexer(input);
    final CommonTokenStream tokens = new CommonTokenStream(lexer);
    final se.tre.freki.query.SelectParser parser = new se.tre.freki.query.SelectParser(tokens);

    final se.tre.freki.query.SelectParser.QueryContext tree = parser.query();
    final ParseTreeWalker treeWalker = new ParseTreeWalker();
    final QueryStringTranslator translator = new QueryStringTranslator(labelClient);
    treeWalker.walk(translator, tree);

    Assert.assertNotNull(translator.query());
  }

  @Test
  public void testCompletedParts() {
    String queryString = "SELECT sys.cpu.0{host=web01} BETWEEN 1 AND 5000";

    final ANTLRInputStream input = new ANTLRInputStream(queryString);
    final se.tre.freki.query.SelectLexer lexer = new se.tre.freki.query.SelectLexer(input);
    final CommonTokenStream tokens = new CommonTokenStream(lexer);
    final se.tre.freki.query.SelectParser parser = new se.tre.freki.query.SelectParser(tokens);

    final se.tre.freki.query.SelectParser.QueryContext tree = parser.query();
    final ParseTreeWalker treeWalker = new ParseTreeWalker();
    final QueryStringTranslator translator = new QueryStringTranslator(labelClient);
    treeWalker.walk(translator, tree);

    Assert.assertEquals(translator.query().endTime(), 5000L);
    Assert.assertEquals(translator.query().startTime(), 1L);

    UnmodifiableIterator<TimeSeriesTagPredicate> list = translator.query().predicate().tagPredicates().iterator();

    while (list.hasNext()) {
      TimeSeriesTagPredicate iterator = list.next();
      SimpleTimeSeriesIdPredicate key = (SimpleTimeSeriesIdPredicate) iterator.key();
      SimpleTimeSeriesIdPredicate value = (SimpleTimeSeriesIdPredicate) iterator.value();

      try {
        Assert.assertEquals(translator.query().predicate().metric(),
            labelClient.lookupId("sys.cpu.0", METRIC).get());
        Assert.assertEquals(key.id(), labelClient.lookupId("host", TAGK).get());
        Assert.assertEquals(value.id(), labelClient.lookupId("web01", TAGV).get());
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (ExecutionException e) {
        e.printStackTrace();
      }
    }
  }
}
