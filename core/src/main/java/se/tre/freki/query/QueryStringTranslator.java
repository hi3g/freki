package se.tre.freki.query;

import se.tre.freki.core.LabelClient;
import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.LabelType;
import se.tre.freki.query.predicate.TimeSeriesQueryPredicate;

import com.google.common.util.concurrent.ListenableFuture;
import org.antlr.v4.runtime.misc.NotNull;

import java.util.concurrent.ExecutionException;

public class QueryStringTranslator extends se.tre.freki.query.SelectParserBaseListener {
  private final LabelClient labelClient;

  private final TimeSeriesQuery.Builder queryBuilder;
  private final TimeSeriesQueryPredicate.Builder predicateBuilder;

  private TimeSeriesQuery query;

  private ListenableFuture<LabelId> metric;

  /**
   * Create a new instance that will resolv label names against the provided {@link LabelClient}.
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

    final TimeSeriesQueryPredicate predicate = predicateBuilder.build();
    query = queryBuilder.predicate(predicate).build();
  }

  @Override
  public void enterQualifier(@NotNull final se.tre.freki.query.SelectParser.QualifierContext ctx) {
    super.enterQualifier(ctx);

    metric = labelClient.lookupId(
        ctx.metric.getText(), LabelType.METRIC);
  }

  public TimeSeriesQuery query() {
    return query;
  }
}
