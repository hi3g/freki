package se.tre.freki.query;

import static com.google.common.util.concurrent.Futures.allAsList;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.Futures.transform;
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
import java.util.Iterator;
import java.util.List;

public class QueryStringTranslator extends se.tre.freki.query.SelectParserBaseListener {
  private final LabelClient labelClient;

  private final TimeSeriesQuery.Builder queryBuilder;
  private final TimeSeriesQueryPredicate.Builder predicateBuilder;

  private ListenableFuture<TimeSeriesQuery> query;

  private ListenableFuture<LabelId> metric;

  private List<ListenableFuture<TimeSeriesIdPredicate>> futurePredicateList;
  private boolean isKey;

  private List<Boolean> operatorList;

  /**
   * Create a new instance that will resolve label names against the provided {@link LabelClient}.
   *
   * @param labelClient The label client to use for resolving label names
   */
  public QueryStringTranslator(final LabelClient labelClient) {
    this.labelClient = labelClient;
    queryBuilder = TimeSeriesQuery.builder();
    predicateBuilder = TimeSeriesQueryPredicate.builder();
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

    query = transform(metric, new AsyncFunction<LabelId, TimeSeriesQuery>() {
      @Override
      public ListenableFuture<TimeSeriesQuery> apply(final LabelId metric)
          throws Exception {
        predicateBuilder.metric(metric);
        return transform(allAsList(futurePredicateList),
            resolveTimeSeriesPredicateList());
      }
    });
  }

  private AsyncFunction<List<TimeSeriesIdPredicate>,
      TimeSeriesQuery> resolveTimeSeriesPredicateList() {
    return new AsyncFunction<List<TimeSeriesIdPredicate>, TimeSeriesQuery>() {
      @Override
      public ListenableFuture<TimeSeriesQuery> apply(
          final List<TimeSeriesIdPredicate> predicateList)
          throws Exception {

        final Iterator<TimeSeriesIdPredicate> predicateIterator = predicateList.iterator();
        final Iterator<Boolean> operatorIterator = operatorList.iterator();

        while (predicateIterator.hasNext()) {
          final TimeSeriesIdPredicate key = predicateIterator.next();
          final TimeSeriesIdPredicate value = predicateIterator.next();

          final boolean operator = operatorIterator.next();
          if (operator) {
            predicateBuilder.addTagPredicate(TimeSeriesTagPredicate.eq(key, value));
          } else {
            predicateBuilder.addTagPredicate(TimeSeriesTagPredicate.neq(key, value));
          }
        }
        final TimeSeriesQueryPredicate predicate = predicateBuilder.build();
        final TimeSeriesQuery query = queryBuilder.predicate(predicate).build();
        return immediateFuture(query);
      }
    };
  }

  @Override
  public void enterQualifier(@NotNull final se.tre.freki.query.SelectParser.QualifierContext ctx) {
    super.enterQualifier(ctx);
    operatorList = new ArrayList<>(ctx.tags.size());
    futurePredicateList = new ArrayList<>(ctx.tags.size());

    metric = labelClient.lookupId(
        ctx.metric.getText(), LabelType.METRIC);
  }

  @Override
  public void enterWildcardTag(
      @NotNull final se.tre.freki.query.SelectParser.WildcardTagContext ctx) {

    super.enterWildcardTag(ctx);
    isKey = !isKey;
    futurePredicateList.add(immediateFuture(WildcardTimeSeriesIdPredicate.wildcard()));
  }

  @Override
  public void enterAlternatingTag(
      @NotNull final se.tre.freki.query.SelectParser.AlternatingTagContext ctx) {
    super.enterAlternatingTag(ctx);

    final LabelType labelType = alternate();

    final List<ListenableFuture<LabelId>> futureIds = new ArrayList<>();
    for (final TerminalNode terminalNode : ctx.LABEL_NAME()) {
      futureIds.add(labelClient.lookupId(terminalNode.getText(), labelType));
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

  @Override
  public void enterSimpleTag(@NotNull final se.tre.freki.query.SelectParser.SimpleTagContext ctx) {
    super.enterSimpleTag(ctx);

    final LabelType type = alternate();

    final ListenableFuture<LabelId> id = labelClient.lookupId(ctx.LABEL_NAME().getText(), type);
    futurePredicateList.add(
        Futures.transform(id, new AsyncFunction<LabelId, TimeSeriesIdPredicate>() {
          @Override
          public ListenableFuture<TimeSeriesIdPredicate> apply(final LabelId labelId)
              throws Exception {
            return immediateFuture(SimpleTimeSeriesIdPredicate.id(labelId));
          }
        }));
  }

  /**
   * Checks if the current typ is key, and inverts the boolean.
   *
   * @return Will return the type we should use. TAGK if isKey TAGV otherwise.
   */
  private LabelType alternate() {
    final LabelType labelType = isKey ? TAGK : TAGV;
    isKey = !isKey;
    return labelType;
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

  public ListenableFuture<TimeSeriesQuery> query() {
    return query;
  }
}
