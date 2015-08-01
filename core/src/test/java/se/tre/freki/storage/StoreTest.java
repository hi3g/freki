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
import se.tre.freki.query.DataPoint;
import se.tre.freki.query.TimeSeriesQuery;
import se.tre.freki.query.predicate.TimeSeriesQueryPredicate;
import se.tre.freki.utils.AsyncIterator;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public abstract class StoreTest<K extends Store> {


  private static final String NAME = "oldName";
  private static final String NEW = "new";
  private static final String MISSING = "missing";
  private static final LabelType TYPE = LabelType.TAGK;

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

  @Test
  public void testQueryFetchesData() throws Exception {
    final LabelId metric1 = store.createLabel("metric1", METRIC).get();
    final LabelId tagk1 = store.createLabel("tagk1", TAGK).get();
    final LabelId tagv1 = store.createLabel("tagv1", TAGV).get();
    final List<LabelId> tags1 = ImmutableList.of(tagk1, tagv1);
    final StaticTimeSeriesId staticTimeSeriesId1 = new StaticTimeSeriesId(metric1, tags1);

    final long startTime = 123123123;
    final long value = 123123;

    for (long time = startTime; time < startTime + 10; time++) {
      store.addPoint(staticTimeSeriesId1, time, value).get();
    }

    final TimeSeriesQueryPredicate.Builder builder = TimeSeriesQueryPredicate.builder();
    builder.metric(metric1);
    builder.addTagPredicate(eq(id(tags1.get(0)), id(tags1.get(1))));

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
      assertEquals(value, ((DataPoint.LongDataPoint) iterator.next()).value());
    }
  }
}
