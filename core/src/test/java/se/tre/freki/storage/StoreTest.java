package se.tre.freki.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static se.tre.freki.labels.LabelType.METRIC;
import static se.tre.freki.labels.LabelType.TAGK;
import static se.tre.freki.labels.LabelType.TAGV;
import static se.tre.freki.query.predicate.SimpleTimeSeriesIdPredicate.id;
import static se.tre.freki.query.predicate.TimeSeriesTagPredicate.eq;

import se.tre.freki.labels.LabelException;
import se.tre.freki.labels.LabelId;
import se.tre.freki.labels.LabelType;
import se.tre.freki.labels.StaticTimeSeriesId;
import se.tre.freki.labels.TimeSeriesId;
import se.tre.freki.meta.LabelMeta;
import se.tre.freki.query.DataPoint;
import se.tre.freki.query.TimeSeriesQuery;
import se.tre.freki.query.predicate.TimeSeriesQueryPredicate;
import se.tre.freki.utils.AsyncIterator;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public abstract class StoreTest<K extends Store> {


  private static final String NAME = "oldName";
  private static final String NEW = "new";
  private static final String MISSING = "missing";
  private static final LabelType TYPE = LabelType.TAGK;

  private static final long VALUE = 123123;

  private LabelId nameId;

  protected K store;

  @Before
  public void setUp() throws Exception {
    store = buildStore();
    nameId = store.createLabel(NAME, TYPE).get();
  }

  protected abstract K buildStore();

  /**
   * Get a label id that the store perceives as not being created. Since we have no regulations on a
   * label id can look like there is no general way to create one, thereby this method.
   */
  protected abstract LabelId missingLabelId();

  @Test
  public void testAllocateLabelExistingException() throws Exception {
    try {
      store.createLabel(NAME, TYPE).get();
      fail("The second allocation with the same name should have thrown an exception");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof LabelException);
    }
  }

  @Test
  public void testCreateLabelNotExisting() throws Exception {
    final LabelId label = store.createLabel(NEW, TYPE).get();
    final Optional<LabelId> fetchedLabel = store.getId(NEW, TYPE).get();
    assertEquals(label, fetchedLabel.get());
  }

  @Test
  public void testGetIdExisting() throws Exception {
    final Optional<LabelId> fetchedLabel = store.getId(NAME, TYPE).get();
    assertEquals(nameId, fetchedLabel.get());
  }

  @Test
  public void testGetIdMissingAbsent() throws Exception {
    final Optional<LabelId> missing = store.getId(MISSING, TYPE).get();
    assertFalse(missing.isPresent());
  }

  @Test
  public void testGetNameExisting() throws Exception {
    final Optional<String> fetchedLabel = store.getName(nameId, TYPE).get();
    assertEquals(NAME, fetchedLabel.get());
  }

  @Test
  public void testGetNameMissingAbsent() throws Exception {
    final Optional<String> missing = store.getName(missingLabelId(), TYPE).get();
    assertFalse(missing.isPresent());
  }

  @Test
  public void testRenameIdFoundOnNewName() throws Exception {
    store.renameLabel(NEW, nameId, TYPE).get();
    final LabelId newNameId = store.getId(NEW, TYPE).get().get();
    assertEquals(nameId, newNameId);
  }

  @Test
  public void testRenameIdNotFoundOnOldName() throws Exception {
    store.renameLabel(NEW, nameId, TYPE).get();
    assertFalse(store.getId(NAME, TYPE).get().isPresent());
  }

  private TimeSeriesId addPoints(final long startTime,
                                 final long endTime,
                                 final int numberOfTags,
                                 final K store) throws Exception {

    final LabelId metric1 = store.createLabel("metric1", METRIC).get();
    List<LabelId> tags = new ArrayList<>(2 * numberOfTags);

    for (int i = 1; i <= numberOfTags; i++) {
      final LabelId tagk = store.createLabel("tagk" + i, TAGK).get();
      final LabelId tagv = store.createLabel("tagv" + i, TAGV).get();
      tags.add(tagk);
      tags.add(tagv);
    }

    final StaticTimeSeriesId staticTimeSeriesId1 = new StaticTimeSeriesId(metric1,
        ImmutableList.copyOf(tags));

    for (long time = startTime; time <= endTime; time++) {
      store.addPoint(staticTimeSeriesId1, time, VALUE).get();
    }

    return staticTimeSeriesId1;
  }

  private Map<TimeSeriesId, AsyncIterator<? extends DataPoint>> getDataPoints(
      final long startTime,
      final long endTime,
      final TimeSeriesQueryPredicate.Builder builder) throws Exception {

    final TimeSeriesQuery query = TimeSeriesQuery.builder()
        .startTime(startTime)
        .endTime(endTime)
        .predicate(builder.build())
        .build();

    return store.query(query).get();
  }

  private void assertDataPoints(
      final long points,
      final Map<TimeSeriesId, AsyncIterator<? extends DataPoint>> dataPoints) {

    final AsyncIterator<? extends DataPoint> iterator =
        dataPoints.entrySet().iterator().next().getValue();
    for (int dataPointIdx = 0; dataPointIdx < points; dataPointIdx++) {
      assertEquals(VALUE, ((DataPoint.LongDataPoint) iterator.next()).value());
    }
  }

  @Test
  public void testEqWrongTagQuery() throws Exception {
    final long startTime = 123123123;
    final long endTime = startTime + 10;
    final int numberOfTags = 1;

    final TimeSeriesId tsuid = addPoints(startTime, endTime, numberOfTags, store);
    final TimeSeriesQueryPredicate.Builder builder = TimeSeriesQueryPredicate.builder();
    builder.metric(tsuid.metric());
    for (int i = 0; i < tsuid.tags().size() / 2; i++) {
      builder.addTagPredicate(eq(id(tsuid.tags().get(i * 2)), id(missingLabelId())));
    }

    final Map<TimeSeriesId, AsyncIterator<? extends DataPoint>> dataPoints = getDataPoints(
        startTime, endTime, builder);

    assertEquals(0, dataPoints.keySet().size());
    assertTrue(dataPoints.entrySet().isEmpty());
  }

  @Test
  public void testMultipleTagsQuery() throws Exception {
    final long startTime = 123123123;
    final long endTime = startTime + 10;
    final int numberOfTags = 2;

    final TimeSeriesId tsId = addPoints(startTime, endTime, numberOfTags, store);

    final TimeSeriesQueryPredicate.Builder builder = TimeSeriesQueryPredicate.builder();
    builder.metric(tsId.metric());
    for (int i = 0; i < tsId.tags().size() / 2; i++) {
      builder.addTagPredicate(eq(id(tsId.tags().get(i * 2)), id(tsId.tags().get((i * 2) + 1))));
    }

    final Map<TimeSeriesId, AsyncIterator<? extends DataPoint>> dataPoints = getDataPoints(
        startTime, endTime, builder);

    assertEquals(1, dataPoints.keySet().size());
    assertDataPoints(startTime - endTime, dataPoints);
  }

  @Test
  public void testSimpleQuery() throws Exception {
    final long startTime = 123123123;
    final long endTime = startTime + 10;
    final int numberOfTags = 1;

    final TimeSeriesId tsId = addPoints(startTime, endTime, numberOfTags, store);

    final TimeSeriesQueryPredicate.Builder builder = TimeSeriesQueryPredicate.builder();
    builder.metric(tsId.metric());

    for (int i = 0; i < tsId.tags().size() / 2; i++) {
      builder.addTagPredicate(eq(id(tsId.tags().get(i * 2)), id(tsId.tags().get((i * 2) + 1))));
    }

    final Map<TimeSeriesId, AsyncIterator<? extends DataPoint>> dataPoints = getDataPoints(
        startTime, endTime, builder);

    assertEquals(1, dataPoints.keySet().size());
    assertDataPoints(startTime - endTime, dataPoints);
  }

  @Test
  public void testQueryFetchesData() throws Exception {

    final long startTime = 123123123;
    final long endTime = startTime + 10;
    final int numberOfTags = 1;

    final TimeSeriesId tsId = addPoints(startTime, endTime, numberOfTags, store);

    final TimeSeriesQueryPredicate.Builder builder = TimeSeriesQueryPredicate.builder();
    builder.metric(tsId.metric());
    for (int i = 0; i < tsId.tags().size() / 2; i++) {
      builder.addTagPredicate(eq(id(tsId.tags().get(i * 2)), id(tsId.tags().get((i * 2) + 1))));
    }

    final TimeSeriesQuery query = TimeSeriesQuery.builder()
        .startTime(123123123)
        .endTime(123123123 + 10)
        .predicate(builder.build())
        .build();

    final Map<TimeSeriesId, AsyncIterator<? extends DataPoint>> dataPoints =
        store.query(query).get();

    assertEquals(1, dataPoints.keySet().size());

    final AsyncIterator<? extends DataPoint> iterator =
        dataPoints.entrySet().iterator().next().getValue();

    for (int dataPointIdx = 0; dataPointIdx < 10; dataPointIdx++) {
      assertEquals(VALUE, ((DataPoint.LongDataPoint) iterator.next()).value());
    }
  }

  @Test
  public void testGetLabelMetaMissingId() throws Exception {
    final LabelId miss = missingLabelId();
    Optional<LabelMeta> meta = store.getMeta(miss, METRIC).get();
    assertFalse(meta.isPresent());
  }

  @Test
  public void getLabelMetaPresent() throws Exception {
    final String name = "meta_test";
    final LabelId nameId = store.createLabel(name, METRIC).get();
    final Clock clock = Clock.systemDefaultZone();
    final long now = clock.millis();

    final LabelMeta labelMeta = LabelMeta.create(nameId, METRIC, name, "Description", now);
    store.updateMeta(labelMeta).get();

    final LabelMeta meta = store.getMeta(nameId, METRIC).get().get();
    Assert.assertEquals(METRIC, meta.type());
    assertEquals("Description", meta.description());
    Assert.assertEquals(nameId, meta.identifier());
  }

  @Test
  public void getLabelMetaMetaNotPresent() throws Exception {
    final String name = "meta_test";
    final LabelId nameId = store.createLabel(name, METRIC).get();

    final Optional<LabelMeta> optional = store.getMeta(nameId, METRIC).get();
    assertFalse(optional.isPresent());
  }
}
