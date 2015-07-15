package se.tre.freki.labels;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.util.concurrent.Futures.transform;
import static se.tre.freki.stats.Metrics.name;
import static se.tre.freki.stats.Metrics.tag;

import se.tre.freki.stats.CacheEvictionCountGauge;
import se.tre.freki.stats.CacheHitRateGauge;
import se.tre.freki.stats.Metrics;
import se.tre.freki.storage.Store;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class contains methods for resolving back and forth between the ID and name of individual
 * IDs. Each instance represents a single type of IDs.
 *
 * <p>Whenever it is known what kind of ID you are working with due to the context of the operation
 * you should prefer the methods in this class instead of the equivalent methods in {@link
 * se.tre.freki.core.LabelClient}.
 */
public class LabelClientTypeContext {
  private static final Logger LOG = LoggerFactory.getLogger(LabelClientTypeContext.class);

  /** The store to use. */
  private final Store store;

  /** The type of IDs represented by this cache. */
  private final LabelType type;

  /** Cache for forward mappings (name to ID). */
  private final Cache<String, LabelId> nameCache;

  /** Cache for backward mappings (ID to name). */
  private final Cache<LabelId, String> idCache;

  /** Map of pending UID assignments. */
  private final Map<String, ListenableFuture<LabelId>> pendingAssignments = new HashMap<>();

  /**
   * The event bus to which the id changes done by this instance will be published.
   */
  private final EventBus idEventBus;

  /**
   * Constructor.
   *
   * @param store The Store to use.
   * @param type The type of UIDs this instance represents
   * @param metrics The metric registry to register metrics on
   * @param idEventBus The event bus where to publish ID events
   */
  public LabelClientTypeContext(final Store store,
                                final LabelType type,
                                final MetricRegistry metrics,
                                final EventBus idEventBus,
                                final long maxCacheSize) {
    this.store = checkNotNull(store);
    this.type = checkNotNull(type);
    this.idEventBus = checkNotNull(idEventBus);

    nameCache = CacheBuilder.newBuilder()
        .maximumSize(maxCacheSize)
        .recordStats()
        .build();

    idCache = CacheBuilder.newBuilder()
        .maximumSize(maxCacheSize)
        .recordStats()
        .build();

    registerMetrics(metrics);
  }

  private void registerMetrics(final MetricRegistry registry) {
    Metrics.Tag typeTag = tag("type", type.toValue());

    registry.register(name("labels.names.hitRate", typeTag), new CacheHitRateGauge(nameCache));
    registry.register(name("labels.names.evictionCount", typeTag),
        new CacheEvictionCountGauge(nameCache));

    registry.register(name("labels.ids.hitRate", typeTag), new CacheHitRateGauge(idCache));
    registry.register(name("labels.ids.evictionCount", typeTag),
        new CacheEvictionCountGauge(idCache));
  }

  /**
   * Finds the name associated with a given ID.
   *
   * @param id The ID associated with that name.
   * @throws LabelException if the given ID is not assigned
   * @see #getId(String)
   * @see #createId(String)
   */
  @Nonnull
  public ListenableFuture<String> getName(final LabelId id) {
    final String name = idCache.getIfPresent(id);

    if (name != null) {
      return Futures.immediateFuture(name);
    }

    class GetNameFunction implements AsyncFunction<Optional<String>, String> {
      @Nullable
      @Override
      public ListenableFuture<String> apply(@Nullable final Optional<String> name) {
        checkNotNull(name);

        if (name.isPresent()) {
          addNameToCache(id, name.get());
          addIdToCache(name.get(), id);
          return Futures.immediateFuture(name.get());
        }

        return Futures.immediateFailedFuture(
            new LabelException(id, type, "No name with id " + id + " and type " + type));
      }
    }

    return transform(store.getName(id, type), new GetNameFunction());
  }

  private void addNameToCache(final LabelId id,
                              final String name) {
    final String foundName = idCache.getIfPresent(id);

    checkState(foundName == null || foundName.equals(name),
        "id %s was already mapped to %s, tried to map to %s", id, foundName, name);

    idCache.put(id, name);
  }

  /**
   * Fetch the label ID behind the provided name and the type associated with this
   * LabelClientTypeContext instance.
   *
   * @param name The name to lookup the ID behind
   * @return A future that on completion will contain the ID behind the name
   */
  @Nonnull
  public ListenableFuture<LabelId> getId(final String name) {
    final LabelId id = nameCache.getIfPresent(name);

    if (id != null) {
      return Futures.immediateFuture(id);
    }

    class GetIdFunction implements AsyncFunction<Optional<LabelId>, LabelId> {
      @Nullable
      @Override
      public ListenableFuture<LabelId> apply(@Nullable final Optional<LabelId> id) {
        checkNotNull(id);

        if (id.isPresent()) {
          addIdToCache(name, id.get());
          addNameToCache(id.get(), name);
          return Futures.immediateFuture(id.get());
        }

        return Futures.immediateFailedFuture(
            new LabelException(name, type, "No ID for name " + name + " and type " + type));
      }
    }

    return transform(store.getId(name, type), new GetIdFunction());
  }

  private void addIdToCache(final String name,
                            final LabelId id) {
    final LabelId foundId = nameCache.getIfPresent(name);

    checkState(foundId == null || foundId.equals(id),
        "name %s was already mapped to %s, tried to map to %s", name, foundId, id);

    nameCache.put(name, id);
  }

  /** Adds the bidirectional mapping in the cache. */
  private void cacheMapping(final String name,
                            final LabelId id) {
    addIdToCache(name, id);
    addNameToCache(id, name);
  }

  /**
   * Create an id with the specified name.
   *
   * @param name The name of the new id
   * @return A deferred with the byte uid if the id was successfully created
   */
  @Nonnull
  public ListenableFuture<LabelId> createId(final String name) {
    ListenableFuture<LabelId> assignment;
    synchronized (pendingAssignments) {
      assignment = pendingAssignments.get(name);
      if (assignment == null) {
        // to prevent UID leaks that can be caused when multiple time
        // series for the same metric or tags arrive, we need to write a
        // deferred to the pending map as quickly as possible. Then we can
        // start the assignment process after we've stashed the deferred
        // and released the lock
        assignment = SettableFuture.create();
        pendingAssignments.put(name, assignment);
      } else {
        LOG.info("Already waiting for UID assignment: {}", name);
        return assignment;
      }
    }

    // start the assignment dance after stashing the deferred
    ListenableFuture<LabelId> uid = store.allocateLabel(name, type);

    return transform(uid, new Function<LabelId, LabelId>() {
      @Nullable
      @Override
      public LabelId apply(@Nullable final LabelId uid) {
        checkNotNull(uid);

        cacheMapping(name, uid);

        LOG.info("Completed pending assignment for: {}", name);
        synchronized (pendingAssignments) {
          pendingAssignments.remove(name);
        }

        idEventBus.post(new LabelCreatedEvent(uid, name, type));

        return uid;
      }
    });
  }

  /**
   * Rename the label with the name {@code oldname} to {@code newname}.
   *
   * @param oldname  The current name of the label
   * @param newname  The desired new name of the label
   * @return A future that indicates the completion of the request
   */
  public ListenableFuture<Void> rename(final String oldname, final String newname) {
    return transform(checkUidExists(newname), new AsyncFunction<Boolean, Void>() {
      @Override
      public ListenableFuture<Void> apply(final Boolean exists) throws Exception {
        if (exists) {
          throw new IllegalArgumentException("An UID with name " + newname + ' '
                                             + "for " + type + " already exists");
        }

        return transform(getId(oldname), new AsyncFunction<LabelId, Void>() {
          @Override
          public ListenableFuture<Void> apply(final LabelId oldUid) throws Exception {
            store.allocateLabel(newname, oldUid, type);

            // Update cache.
            addIdToCache(newname, oldUid);  // add     new name -> ID
            idCache.put(oldUid, newname);   // update  ID -> new name
            nameCache.invalidate(oldname);  // remove  old name -> ID

            // Delete the old forward mapping.
            return store.deleteLabel(oldname, type);
          }
        });
      }
    });
  }

  /**
   * Check if there is a label with the given name.
   *
   * @param name The name to check if it exists
   * @return A future that on completion contains true if the name exists, false otherwise
   */
  public ListenableFuture<Boolean> checkUidExists(String name) {
    final SettableFuture<Boolean> exists = SettableFuture.create();

    Futures.addCallback(getId(name), new FutureCallback<LabelId>() {
      @Override
      public void onSuccess(@Nullable final LabelId result) {
        exists.set(result != null);
      }

      @Override
      public void onFailure(final Throwable throwable) {
        exists.set(false);
      }
    });

    return exists;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("type", type)
        .toString();
  }
}
