package se.tre.freki.query;

import static com.google.common.util.concurrent.Futures.allAsList;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static se.tre.freki.labels.LabelType.TAGK;
import static se.tre.freki.labels.LabelType.TAGV;

import se.tre.freki.core.LabelClient;
import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.LabelType;
import se.tre.freki.query.predicate.AlternationTimeSeriesIdPredicate;
import se.tre.freki.query.predicate.SimpleTimeSeriesIdPredicate;
import se.tre.freki.query.predicate.TimeSeriesIdPredicate;
import se.tre.freki.query.predicate.TimeSeriesQueryPredicate;
import se.tre.freki.query.predicate.TimeSeriesTagPredicate;
import se.tre.freki.query.predicate.WildcardTimeSeriesIdPredicate;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class QueryStringTranslator extends se.tre.freki.query.SelectParserBaseListener {
  private final LabelClient labelClient;

  private final TimeSeriesQuery.Builder queryBuilder;
  private final TimeSeriesQueryPredicate.Builder predicateBuilder;

  private TimeSeriesQuery query;

  private ListenableFuture<LabelId> metric;

  private final List<ListenableFuture<TimeSeriesIdPredicate>> futurePredicateList;
  private boolean isKey;

  private List<Boolean> operatorList;

  private TimeSeriesIdPredicate key;
  private TimeSeriesIdPredicate value;

  /**
   * Create a new instance that will resolve label names against the provided {@link LabelClient}.
   *
   * @param labelClient The label client to use for resolving label names
   */
  public QueryStringTranslator(final LabelClient labelClient) {
    this.labelClient = labelClient;
    queryBuilder = TimeSeriesQuery.builder();
    predicateBuilder = TimeSeriesQueryPredicate.builder();
    futurePredicateList = new ArrayList<>();
    operatorList = new ArrayList<>();
  }

  @Override
  public void enterQuery(@NotNull final se.tre.freki.query.SelectParser.QueryContext ctx) {
    super.enterQuery(ctx);

    final long startTime = Long.parseLong(ctx.startTime.getText());
    final long endTime = Long.parseLong(ctx.endTime.getText());
    isKey = true;

    queryBuilder.startTime(startTime)
        .endTime(endTime);
  }

  @Override
  public void exitQuery(@NotNull final se.tre.freki.query.SelectParser.QueryContext ctx) {
    super.exitQuery(ctx);

    try {
      predicateBuilder.metric(metric.get());
    } catch (ExecutionException e) {
      throw new QueryException("Error while fetching metric", e);
    } catch (InterruptedException e) {
      throw new QueryException("Interrupted while waiting for metric", e);
    }
    List<TimeSeriesIdPredicate> predicateList;
    try {
      predicateList = allAsList(futurePredicateList).get();
    } catch (InterruptedException e) {
      throw new QueryException("Interrupted while waiting for tags", e);
    } catch (ExecutionException e) {
      throw new QueryException("Error while fetching tags", e);
    }
    for (int i = 0; i < predicateList.size() / 2; i++) {
      key = predicateList.get(i * 2);
      value = predicateList.get(i * 2 + 1);

      if (operatorList.get(i)) {
        predicateBuilder.addTagPredicate(TimeSeriesTagPredicate.eq(key, value));
      } else {
        predicateBuilder.addTagPredicate(TimeSeriesTagPredicate.neq(key, value));
      }
    }

    final TimeSeriesQueryPredicate predicate = predicateBuilder.build();
    query = queryBuilder.predicate(predicate).build();
  }

  @Override
  public void enterQualifier(@NotNull final se.tre.freki.query.SelectParser.QualifierContext ctx) {
    super.enterQualifier(ctx);

    metric = labelClient.lookupId(
        ctx.metric.getText(), LabelType.METRIC);
  }

  @Override
  public void enterWildcardTag(
      @NotNull final se.tre.freki.query.SelectParser.WildcardTagContext ctx) {
    super.enterWildcardTag(ctx);
    if (isKey) {
      isKey = false;
      futurePredicateList.add(immediateFuture(WildcardTimeSeriesIdPredicate.wildcard()));
    } else {
      isKey = true;
      futurePredicateList.add(immediateFuture(WildcardTimeSeriesIdPredicate.wildcard()));
    }
  }

  @Override
  public void enterAlternatingTag(
      @NotNull final se.tre.freki.query.SelectParser.AlternatingTagContext ctx) {
    super.enterAlternatingTag(ctx);

    if (isKey) {
      isKey = false;
      final List<ListenableFuture<LabelId>> futureIds = new ArrayList<>();
      for (final TerminalNode terminalNode : ctx.LABEL_NAME()) {
        futureIds.add(labelClient.lookupId(terminalNode.getText(), TAGK));
      }
      futurePredicateList.add(Futures.transform(allAsList(futureIds),
          new AsyncFunction<List<LabelId>, TimeSeriesIdPredicate>() {
            @Override
            public ListenableFuture<TimeSeriesIdPredicate> apply(final List<LabelId> labelIds)
                throws Exception {
              return immediateFuture(AlternationTimeSeriesIdPredicate.ids(labelIds));
            }
          }));
    } else {
      isKey = true;
      final List<ListenableFuture<LabelId>> futureIds = new ArrayList<>();
      for (final TerminalNode terminalNode : ctx.LABEL_NAME()) {
        futureIds.add(labelClient.lookupId(terminalNode.getText(), TAGV));
      }
      futurePredicateList.add(Futures.transform(allAsList(futureIds),
          new AsyncFunction<List<LabelId>, TimeSeriesIdPredicate>() {
            @Override
            public ListenableFuture<TimeSeriesIdPredicate> apply(final List<LabelId> labelIds)
                throws Exception {
              return immediateFuture(AlternationTimeSeriesIdPredicate.ids(labelIds));
            }
          }));
    }
  }

  @Override
  public void enterSimpleTag(@NotNull final se.tre.freki.query.SelectParser.SimpleTagContext ctx) {
    super.enterSimpleTag(ctx);

    if (isKey) {
      isKey = false;
      final ListenableFuture<LabelId> futureId = labelClient.lookupId(ctx.LABEL_NAME().getText(),
          TAGK);
      futurePredicateList.add(Futures.transform(futureId,
          new AsyncFunction<LabelId, TimeSeriesIdPredicate>() {
            @Override
            public ListenableFuture<TimeSeriesIdPredicate> apply(final LabelId labelId)
                throws Exception {
              return immediateFuture(SimpleTimeSeriesIdPredicate.id(labelId));
            }
          }));
    } else {
      isKey = true;
      final ListenableFuture<LabelId> futureId = labelClient.lookupId(ctx.LABEL_NAME().getText(),
          TAGV);
      futurePredicateList.add(Futures.transform(futureId,
          new AsyncFunction<LabelId, TimeSeriesIdPredicate>() {
            @Override
            public ListenableFuture<TimeSeriesIdPredicate> apply(final LabelId input)
                throws Exception {
              return immediateFuture(SimpleTimeSeriesIdPredicate.id(input));
            }
          }));
    }
  }

  @Override
  public void enterTag(@NotNull final se.tre.freki.query.SelectParser.TagContext ctx) {
    super.enterTag(ctx);

    if (ctx.operator().EQUALS() != null) {
      operatorList.add(true);
    } else {
      operatorList.add(false);
    }
  }

  public TimeSeriesQuery query() {
    return query;
  }
}
