package se.tre.freki.query;

import static com.google.common.util.concurrent.Futures.addCallback;
import static com.google.common.util.concurrent.Futures.allAsList;
import static se.tre.freki.labels.LabelType.METRIC;
import static se.tre.freki.labels.LabelType.TAGK;
import static se.tre.freki.labels.LabelType.TAGV;

import se.tre.freki.core.LabelClient;
import se.tre.freki.labels.LabelId;
import se.tre.freki.query.predicate.AlternationTimeSeriesIdPredicate;
import se.tre.freki.query.predicate.SimpleTimeSeriesIdPredicate;
import se.tre.freki.query.predicate.TimeSeriesIdPredicate;
import se.tre.freki.query.predicate.TimeSeriesQueryPredicate;
import se.tre.freki.query.predicate.TimeSeriesTagPredicate;
import se.tre.freki.query.predicate.WildcardTimeSeriesIdPredicate;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

public class QueryStringTranslator extends se.tre.freki.query.QueryStatementsBaseListener {

  final LabelClient labelClient;

  public QueryStringTranslator(@Nonnull LabelClient labelClient) {
    this.labelClient = labelClient;
  }

  final TimeSeriesQueryPredicate.Builder builder = TimeSeriesQueryPredicate.builder();

  @Override
  public void enterMetric(se.tre.freki.query.QueryStatementsParser.MetricContext ctx) {
    String metric = ctx.getText();
    final ListenableFuture<LabelId> metric_id = labelClient.lookupId(metric, METRIC);
    addCallback(metric_id, new FutureCallback<LabelId>() {
      @Override
      public void onSuccess(final LabelId result) {
        builder.metric(result);
      }

      @Override
      public void onFailure(final Throwable throwable) {

      }
    });
  }

  @Override
  public void enterTagpairs(se.tre.freki.query.QueryStatementsParser.TagpairsContext ctx) {
    final List<TimeSeriesIdPredicate> idPredicates = new ArrayList<>();


    List<TerminalNode> keys = ctx.tagkey().Identifier();
    List<TerminalNode> values = ctx.tagvalue().Identifier();

    if( keys.size() == 1 && keys.get(0).getText().equals("*")) {
      idPredicates.add(0, WildcardTimeSeriesIdPredicate.wildcard());
    }
    else if (keys.size() == 1) {

      final ListenableFuture<LabelId> tagk_id = labelClient.lookupId(
          keys.get(0).getText(), TAGK);
     addCallback(tagk_id, new FutureCallback<LabelId>() {

       @Override
       public void onSuccess(final LabelId result) {
         idPredicates.add(0, SimpleTimeSeriesIdPredicate.id(result));
       }

       @Override
       public void onFailure(final Throwable throwable) {
       }
     });
    }
    else {
      final List<ListenableFuture<LabelId>> tagk_ids = new ArrayList<>();
      for (final TerminalNode key : keys) {
        tagk_ids.add(labelClient.lookupId(key.getText(), TAGK));
      }

      addCallback(allAsList(tagk_ids), new FutureCallback<List<LabelId>>() {
        @Override
        public void onSuccess(final List<LabelId> result) {

          idPredicates.add(0, AlternationTimeSeriesIdPredicate.ids(result));
        }

        @Override
        public void onFailure(final Throwable throwable) {

        }
      });
    }

    if( values.size() == 1 && values.get(0).getText().equals("*")) {
      idPredicates.add(1, WildcardTimeSeriesIdPredicate.wildcard());
    }
    else if (values.size() == 1) {

      final ListenableFuture<LabelId> tagv_id = labelClient.lookupId(
          values.get(0).getText(), TAGV);
      addCallback(tagv_id, new FutureCallback<LabelId>() {

        @Override
        public void onSuccess(final LabelId result) {
          idPredicates.add(1, SimpleTimeSeriesIdPredicate.id(result));
        }

        @Override
        public void onFailure(final Throwable throwable) {
        }
      });
    }
    else {
      final List<ListenableFuture<LabelId>> tagv_ids = new ArrayList<>();
      for (final TerminalNode value : values) {
        tagv_ids.add(labelClient.lookupId(value.getText(), TAGV));
      }

      addCallback(allAsList(tagv_ids), new FutureCallback<List<LabelId>>() {
        @Override
        public void onSuccess(final List<LabelId> result) {

          idPredicates.add(1, AlternationTimeSeriesIdPredicate.ids(result));
        }

        @Override
        public void onFailure(final Throwable throwable) {

        }
      });
    }

    if( ctx.operator().getText().equals("!="))
      builder.addTagPredicate(TimeSeriesTagPredicate.neq(
          idPredicates.get(0),idPredicates.get(1)));
    else{
      builder.addTagPredicate(TimeSeriesTagPredicate.eq(
          idPredicates.get(0),idPredicates.get(1)));
    }
  }

  @Override
  public void exitQuery(se.tre.freki.query.QueryStatementsParser.QueryContext ctx) {
    builder.build();
  }
}
